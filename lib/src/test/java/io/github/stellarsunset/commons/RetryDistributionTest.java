package io.github.stellarsunset.commons;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RetryDistributionTest {

    @Test
    void testConstantDistribution() {
        RetryDistribution distribution = RetryDistribution.constant(Duration.ofMinutes(1));

        assertAll(
                () -> assertEquals(Duration.ofMinutes(1), distribution.nextWait(Duration.ZERO), "Zero should go to one."),
                () -> assertEquals(Duration.ofMinutes(1), distribution.nextWait(Duration.ofMinutes(1)), "One should go to one.")
        );
    }

    @Test
    void testLinearDistribution() {
        RetryDistribution distribution = RetryDistribution.linear(Duration.ofMinutes(1));

        assertAll(
                () -> assertEquals(Duration.ofMinutes(1), distribution.nextWait(Duration.ZERO), "Zero should go to one."),
                () -> assertEquals(Duration.ofMinutes(2), distribution.nextWait(Duration.ofMinutes(1)), "One should go to two.")
        );
    }

    @Test
    void testMultiplicativeDistribution() {
        RetryDistribution distribution = RetryDistribution.multiplicative(Duration.ofMillis(10), 2);

        assertAll(
                () -> assertEquals(Duration.ofMillis(10), distribution.nextWait(Duration.ZERO), "Zero should go to provided base."),
                () -> assertEquals(Duration.ofMillis(20), distribution.nextWait(Duration.ofMillis(10)), "10ms should go to 20ms.")
        );
    }

    @Test
    void testExponentialDistribution() {
        RetryDistribution distribution = RetryDistribution.exponential(Duration.ofMillis(10));

        assertAll(
                () -> assertEquals(Duration.ofMillis(10), distribution.nextWait(Duration.ZERO), "Zero should go to 10ms."),
                () -> assertEquals(Duration.ofMillis(100), distribution.nextWait(Duration.ofMillis(10)), "10ms should go to 100ms."),
                () -> assertEquals(Duration.ofSeconds(1), distribution.nextWait(Duration.ofMillis(100)), "100ms should go to 1s.")
        );
    }

    @Test
    void testJitter() {
        RetryDistribution distribution = RetryDistribution.jitter(RetryDistribution.constant(Duration.ofMillis(50)), 10);

        assertAll(
                () -> assertEquals(50, distribution.nextWait(Duration.ZERO).toMillis(), 10, "Expected value in range [40, 60]"),
                () -> assertEquals(50, distribution.nextWait(Duration.ofMillis(10)).toMillis(), 10, "Expected value in range [40, 60]")
        );
    }
}
