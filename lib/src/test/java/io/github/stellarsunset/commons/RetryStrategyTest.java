package io.github.stellarsunset.commons;

import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.github.stellarsunset.commons.RetryDistribution.linear;
import static java.time.Duration.ofMillis;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.*;

class RetryStrategyTest {

    private static <Q> Throwable runToException(RetryStrategy strategy, Q request) throws Exception {
        CompletableFuture<Q> future = strategy.retryIfNecessary(
                request,
                s -> CompletableFuture.failedFuture(new IllegalArgumentException())
        );

        return future.handle((s, ex) -> ofNullable(ex).map(Throwable::getCause).orElseThrow()).get(1, TimeUnit.SECONDS);
    }

    @Test
    void testMaxRetriesStrategy_Count() throws Exception {

        CountingSubmitter<String> submitter = new CountingSubmitter<>("hello", 5);

        String three = RetryStrategy.toMaxRetries(RetryDistribution.constant(Duration.ofMillis(1)), 3)
                .retryIfNecessary("whatever", submitter.reset()::submit)
                .handle((s, ex) -> ofNullable(ex).map(Throwable::getCause).map(Throwable::getMessage).orElse(s))
                .get(1, TimeUnit.SECONDS);

        int submissionsThree = submitter.actualSubmissions();

        String ten = RetryStrategy.toMaxRetries(RetryDistribution.constant(Duration.ofMillis(1)), 10)
                .retryIfNecessary("whatever", submitter.reset()::submit)
                .handle((s, ex) -> ofNullable(ex).map(Throwable::getCause).map(Throwable::getMessage).orElse(s))
                .get(1, TimeUnit.SECONDS);

        int submissionsTen = submitter.actualSubmissions();

        assertAll(
                // 1 initial + 3 retries = 4 total submissions
                () -> assertEquals(4, submissionsThree, "Expected three total submissions (then the retry strategy should have let the error fall through)."),
                () -> assertNotEquals("hello", three, "Should be the body of some exception message."),

                () -> assertEquals(5, submissionsTen, "Expected five total submissions (then the submitter should have returned the successful response)."),
                () -> assertEquals("hello", ten, "Should be the expected contents of the submitter response.")
        );
    }

    @Test
    void testMaxRetriesStrategy() throws Exception {

        Stopwatch stopwatch = Stopwatch.createStarted();

        Throwable exception = runToException(RetryStrategy.toMaxRetries(linear(ofMillis(5)), 3), null);
        stopwatch.stop();

        assertAll(
                () -> assertInstanceOf(IllegalArgumentException.class, exception),
                () -> assertEquals(3, exception.getSuppressed().length, "Should be three suppressed exceptions from retries."),
                () -> assertTrue(5 + 10 + 15L <= stopwatch.elapsed().toMillis(), "At least 30ms should have elapsed (thanks to retries): " + stopwatch.elapsed().toMillis())
        );
    }

    @Test
    void testMaxWaitStrategy_DurationCompare() {
        Duration d1 = Duration.ofMinutes(1);
        Duration d2 = Duration.ofMinutes(2);
        Duration d3 = Duration.ofMinutes(3);

        assertAll(
                () -> assertTrue(RetryStrategy.MaxWait.waitTooLong(d3, d2), "D3 current is longer than D2 max - should return true, check your ><'s"),
                () -> assertFalse(RetryStrategy.MaxWait.waitTooLong(d1, d2), "D1 current is shorter than D2 max - should return false, check your ><'s")
        );
    }

    @Test
    void testMaxWaitStrategy() throws Exception {

        Stopwatch stopwatch = Stopwatch.createStarted();

        Throwable exception = runToException(RetryStrategy.toMaxWait(linear(ofMillis(5)), ofMillis(18)), null);
        stopwatch.stop();

        assertAll(
                () -> assertInstanceOf(IllegalArgumentException.class, exception),
                () -> assertEquals(3, exception.getSuppressed().length, "Should be three suppressed exceptions from retries."),
                () -> assertTrue(5 + 10 + 15L <= stopwatch.elapsed().toMillis(), "At least 30ms should have elapsed (thanks to retries): " + stopwatch.elapsed().toMillis())
        );
    }

    private static final class CountingSubmitter<T> {

        private final T value;
        private final int targetSubmissions;

        private int actualSubmissions = 0;

        private CountingSubmitter(T value, int targetSubmissions) {
            this.value = requireNonNull(value);
            this.targetSubmissions = targetSubmissions;
        }

        CompletableFuture<T> submit(Object input) {
            actualSubmissions += 1;
            return actualSubmissions >= targetSubmissions
                    ? CompletableFuture.completedFuture(value)
                    : CompletableFuture.failedFuture(new IllegalStateException());
        }

        CountingSubmitter<T> reset() {
            actualSubmissions = 0;
            return this;
        }

        int targetSubmissions() {
            return targetSubmissions;
        }

        int actualSubmissions() {
            return actualSubmissions;
        }
    }
}
