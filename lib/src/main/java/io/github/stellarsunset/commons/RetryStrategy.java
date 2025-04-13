package io.github.stellarsunset.commons;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

/**
 * Represents a high-level strategy that can be used to retry a task, typically until some implementation-specific exit
 * criteria has been met.
 */
@FunctionalInterface
public interface RetryStrategy {

    /**
     * Returns a retry strategy that won't retry any failed requests.
     */
    static RetryStrategy dont() {
        return new Dont();
    }

    /**
     * Returns a retry strategy that will retry requests with intervals following the provided distribution up to the
     * provided maximum number of retries.
     *
     * @param distribution the distribution to follow for spacing between requests
     * @param maxRetries   the maximum number of times to retry the request
     */
    static RetryStrategy toMaxRetries(RetryDistribution distribution, int maxRetries) {
        return new MaxRetries(distribution, maxRetries);
    }

    /**
     * Returns a retry strategy that will retry requests until the time between requests duration exceeds the provided
     * max wait.
     *
     * <p>This is sensitive to the distribution used e.g. {@link RetryDistribution#constant(Duration)} is a bad choice
     * as you will either immediately hit the max wait or never hit it.
     */
    static RetryStrategy toMaxWait(RetryDistribution distribution, Duration maxWait) {
        return new MaxWait(distribution, maxWait);
    }

    /**
     * Called to enter the retry strategy for the provided request and submission strategy. The request is provided as
     * an argument alongside the submission method (instead of as a {@link Callable} or a {@link Supplier}) for two reasons:
     * <ul>
     *     <li>So the request can potentially be modified between subsequent calls to the submitter</li>
     *     <li>So on retry the strategy can potentially hide multiple sub-strategies which delegate to the request by type</li>
     * </ul>
     * That being the case - strategies can be thought of as 1 (one) nominal submission of the request, and some number
     * of retries dictated by the strategy implementation. With the base exception being from the nominal submission and
     * suppressed ones from any failed retries.
     *
     * <p>Note: this means sending the initial nominal request should be delegated to the strategy, rather than being
     * called from the client (in most cases).
     *
     * <p>Note: these strategies should immediately exit on any interrupted exceptions - cutting the retry chain and returning.
     *
     * @param request   - the request to submit
     * @param submitter - submission function for resubmitting the supplied request
     */
    <Q, R> CompletableFuture<R> retryIfNecessary(Q request, Function<Q, CompletableFuture<R>> submitter);

    record Dont() implements RetryStrategy {
        @Override
        public <Q, R> CompletableFuture<R> retryIfNecessary(Q request, Function<Q, CompletableFuture<R>> submitter) {
            return submitter.apply(request);
        }
    }

    record MaxRetries(RetryDistribution distribution, int maxRetries) implements RetryStrategy {

        public MaxRetries {
            checkArgument(maxRetries >= 0, "Should specify 0 or more retries.");
        }

        @Override
        public <Q, R> CompletableFuture<R> retryIfNecessary(Q request, Function<Q, CompletableFuture<R>> submitter) {
            return submitter.apply(request)
                    .handle((r, ex) -> ofNullable(ex)
                            .map(e -> retry(
                                    1,
                                    distribution.nextWait(Duration.ZERO),
                                    e,
                                    request,
                                    submitter
                            )).orElseGet(() -> completedFuture(r)))
                    .thenCompose(Function.identity());
        }

        private static Throwable addSuppressed(Throwable first, Throwable next) {
            first.addSuppressed(next);
            return first;
        }

        private static void waitUnlessInterrupted(int retry, Duration wait) {
            try {
                Thread.sleep(wait.toMillis());
            } catch (InterruptedException e) {
                throw new IllegalArgumentException(String.format("Interrupted on retry: %s", retry), e);
            }
        }

        private <Q, R> CompletableFuture<R> retry(
                int count,
                Duration wait,
                Throwable throwable,
                Q request,
                Function<Q, CompletableFuture<R>> submitter
        ) {
            if (count > maxRetries) {
                return failedFuture(throwable);
            }
            waitUnlessInterrupted(count + 1, wait);
            return submitter.apply(request)
                    .handle((r, ex) -> ofNullable(ex)
                            .map(e -> retry(
                                    count + 1,
                                    distribution.nextWait(wait),
                                    addSuppressed(throwable, e),
                                    request,
                                    submitter
                            )).orElseGet(() -> completedFuture(r))
                    ).thenCompose(Function.identity());
        }
    }

    record MaxWait(RetryDistribution distribution, Duration maxWait) implements RetryStrategy {

        public MaxWait {
            checkArgument(!maxWait.isZero() && !maxWait.isNegative(), "Wait should be > 0");
        }

        @Override
        public <Q, R> CompletableFuture<R> retryIfNecessary(Q request, Function<Q, CompletableFuture<R>> submitter) {
            return submitter.apply(request)
                    .handle((r, ex) -> ofNullable(ex)
                            .map(e -> retry(
                                    1,
                                    distribution.nextWait(Duration.ZERO),
                                    ex,
                                    request,
                                    submitter
                            )).orElseGet(() -> completedFuture(r)))
                    .thenCompose(Function.identity());
        }

        static boolean waitTooLong(Duration wait, Duration max) {
            return wait.compareTo(max) > 0;
        }

        private <Q, R> CompletableFuture<R> retry(
                int count,
                Duration wait,
                Throwable throwable,
                Q request,
                Function<Q, CompletableFuture<R>> submitter
        ) {
            if (waitTooLong(wait, maxWait)) {
                return failedFuture(throwable);
            }
            MaxRetries.waitUnlessInterrupted(count + 1, wait);
            return submitter.apply(request)
                    .handle((r, ex) -> ofNullable(ex)
                            .map(e -> retry(
                                    count + 1,
                                    distribution.nextWait(wait),
                                    MaxRetries.addSuppressed(throwable, e),
                                    request,
                                    submitter
                            )).orElseGet(() -> completedFuture(r))
                    ).thenCompose(Function.identity());
        }
    }
}
