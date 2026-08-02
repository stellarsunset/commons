package io.github.stellarsunset.commons;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IssueTest {

  @Test
  void testAsThrowableExceptionThrown() {
    var exception = new IllegalArgumentException();
    var issue = Issue.exceptionThrown(exception);

    var throwable = issue.asException();
    assertAll(
        () ->
            assertArrayEquals(
                new Throwable[] {exception},
                throwable.getSuppressed(),
                "Should see the exception as suppressed"),
        () -> assertEquals(exception, throwable.getCause(), "Should see exception in cause"));
  }

  @Test
  void testAsThrowableAllOf() {
    var exception = new IllegalArgumentException();
    var issue = Issue.allOf(Issue.exceptionThrown(exception), Issue.exceptionThrown(exception));

    var throwable = issue.asException();
    assertArrayEquals(
        new Throwable[] {exception, exception},
        throwable.getSuppressed(),
        "Should see the exception as suppressed");
  }
}
