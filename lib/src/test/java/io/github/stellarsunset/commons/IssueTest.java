package io.github.stellarsunset.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IssueTest {

    @Test
    void testAsThrowable() {
        var exception = new IllegalArgumentException();
        var issue = Issue.exceptionThrown(exception);

        var throwable = issue.asException();
        assertAll(
                () -> assertArrayEquals(new Throwable[]{exception}, throwable.getSuppressed(),
                        "Should see the exception as suppressed"),
                () -> assertEquals(exception, throwable.getCause(), "Should see exception in cause")
        );
    }
}
