package io.github.stellarsunset.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class IssueTest {

    @Test
    void testAsThrowable() {
        var exception = new IllegalArgumentException();
        var issue = new Issue.ExceptionThrown(exception);

        var throwable = issue.asException();
        assertArrayEquals(new Throwable[]{exception}, throwable.getSuppressed(),
                "Should see the exception as suppressed");
    }
}
