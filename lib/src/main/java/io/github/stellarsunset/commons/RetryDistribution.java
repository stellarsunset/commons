package io.github.stellarsunset.commons;

import java.time.Duration;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Interface representing a distribution of retry times - typically to be used alongside a {@link RetryStrategy}.
 */
@FunctionalInterface
public interface RetryDistribution {

    /**
     * Returns a new {@link RetryDistribution} where the internal interval between subsequent retries increases by the
     * provided amount each time.
     *
     * <p>e.g. {@code constant(Duration.ofSeconds(1) -> 1s -> 1s -> 1s -> 1s}.
     */
    static RetryDistribution constant(Duration retryInterval) {
        return previousWait -> retryInterval;
    }

    /**
     * Returns a new {@link RetryDistribution} that increases the backoff by a linear amount request-to-request.
     *
     * <p>e.g. {@code linear(Duration.ofSeconds(1) -> 2s -> 3s -> 4s -> 5s)}.
     */
    static RetryDistribution linear(Duration intervalIncrease) {
        return previousWait -> previousWait.plus(intervalIncrease);
    }

    /**
     * Returns a new {@link RetryDistribution} where the interval between subsequent retries is the provided multiplication
     * factor times the previous wait interval.
     *
     * <p>The multiplication factor here is taken to be an integer so even units of time are returned when converting the
     * previous duration to the next duration (i.e. not 10.001ms).
     *
     * <p>e.g. {@code multiplicative(Duration.ofMillis(20), 2.0) -> 10ms -> 20ms -> 40ms -> 80ms}
     */
    static RetryDistribution multiplicative(Duration initial, int multiplicationFactor) {
        checkArgument(multiplicationFactor > 0, "Multiplication factor should be > 0");
        return previousWait -> previousWait.isNegative() || previousWait.isZero()
                ? initial
                : previousWait.multipliedBy(multiplicationFactor);
    }

    /**
     * Returns a new {@link RetryDistribution} where the interval between subsequent retries increases by an exponential
     * amount.
     *
     * <p>Remember, because this is exponential this increases <i>extremely quickly</i> - and this method is specifying
     * the time to first retry.
     *
     * <p>e.g. {@code exponential(Duration.ofMillis(10)) -> 10ms -> 100ms -> 1s -> 10s -> 100s}
     */
    static RetryDistribution exponential(Duration waitForFirstRetry) {
        checkArgument(!waitForFirstRetry.isZero(), "Wait for the first retry cannot be zero.");
        return previousWait -> {
            if (previousWait.isZero()) {
                return waitForFirstRetry;
            }else{
                double iteration = Math.log(previousWait.toMillis()) / Math.log(waitForFirstRetry.toMillis());
                return Duration.ofMillis((long) Math.pow(waitForFirstRetry.toMillis(), iteration + 1));
            }
        };
    }

    /**
     * Returns a new {@link RetryDistribution} decorating the provided distribution as one with a random fractional "jitter"
     * added to the original returned next wait time of the provided distribution.
     *
     * <p>Jitter prevents the submissions from multiple retried requests from all getting fired off at the same time and
     * immediately overloading the target server and getting rate-limited.
     */
    static RetryDistribution jitter(RetryDistribution distribution, int maxJitterMillis) {
        Random random = new Random();
        return previousWait -> {
            Duration nextWait = distribution.nextWait(previousWait);

            int jitter = (int) Math.min(Math.max(nextWait.toMillis(), 1), maxJitterMillis);
            return nextWait.plus(Duration.ofMillis(random.nextInt(jitter)));
        };
    }

    /**
     * Given the previous wait duration - return the next wait duration based on an internal distribution.
     */
    Duration nextWait(Duration previousWait);
}
