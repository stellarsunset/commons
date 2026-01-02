package io.github.stellarsunset.commons;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * An interface letting clients define custom and descriptive {@link Issue}s that happen during normal code execution to
 * be used with {@link Either} to simulate Go-style method returns.
 *
 * <p>This interface is provided here to allow us to special-case some quality-of-life interop between an {@link Either}
 * and it's right-hand type.
 *
 * <p>For convenience, this interface ships with a few dumb out-of-the-box {@link Issue} implementations, but most of the
 * time clients should be extending {@link Issue} with their own sealed-hierarchy of types.
 */
public interface Issue {

    /**
     * There is no issue.
     *
     * <p>Defined centrally as it <i>may</i> be useful to have this notion centralized.
     */
    static None none() {
        return new None();
    }

    /**
     * Convenience, overload.
     */
    static ExceptionThrown exceptionThrown(Exception exception) {
        return exceptionThrown("Encountered an exception while running.", exception);
    }

    /**
     * Provided for convenience.
     *
     * <p>Use this to demote an exception to an issue that shouldn't be thrown up the call stack, without throwing a runtime
     * exception or adding a checked exception to the signature:
     * <pre>{@code
     * Either<byte[], Issue> readFile(File file) {
     *     try {
     *         return Either.ofLeft(new FileInputStream(file).readAllBytes());
     *     } (IOException e) {
     *         return Either.ofRight(Issue.exceptionThrown(e));
     *     }
     * }
     * }</pre>
     *
     * <p>Has special handling in the {@link AsThrowable}.
     */
    static ExceptionThrown exceptionThrown(String summary, Exception exception) {
        return new ExceptionThrown(summary, exception);
    }

    /**
     * Convenience, overload.
     */
    static AllOf allOf(Issue... issues) {
        return allOf(Stream.of(issues).map(Issue::summary).collect(Collectors.joining("\n")), List.of(issues));
    }

    /**
     * Returns a new {@link Issue} representing the combination of all the provided issues.
     *
     * <p>This is mostly useful for issue aggregation and indicating when multiple distinct things went wrong in the
     * program all of which have different underlying causal factors that need to be handled.
     *
     * <p>Has special handling in the {@link AsThrowable}.
     */
    static AllOf allOf(String summary, List<Issue> issues) {
        return new AllOf(summary, issues);
    }

    /**
     * Out of courtesy, {@link Issue}s should include a descriptive summary.
     *
     * <p>This is used in a couple built-in places like setting the {@link AsThrowable} exception message.
     */
    String summary();

    /**
     * Returns the {@link Issue} wrapped as a {@link RuntimeException} that can be directly thrown.
     *
     * <p>A sensible default implementation is provided here to cover most cases, but this can and should be overwritten
     * for any custom {@link Issue} subtypes that may want to throw their own dedicated exception shape.
     */
    default RuntimeException asException() {
        return new AsThrowable(this);
    }

    record None() implements Issue {
        @Override
        public String summary() {
            return "There is no issue.";
        }
    }

    record ExceptionThrown(String summary, Exception exception) implements Issue {
    }

    record AllOf(String summary, List<Issue> issues) implements Issue {
    }

    /**
     * A wrapper for an {@link Issue} as a {@link RuntimeException} that can be thrown, but also caught again at a higher
     * level in the program and unwrapped to inspect the underlying {@link Issue}.
     *
     * <p>Rather than add more hooks here to allow {@link Issue} implementations more control over how this default error
     * prints they should instead override {@link Issue#asException()} directly.
     */
    final class AsThrowable extends RuntimeException {

        private final Issue issue;

        private AsThrowable(Issue issue) {
            super(issue.summary(), issue instanceof ExceptionThrown ex ? ex.exception() : null);
            this.issue = requireNonNull(issue);
            this.addSuppressed(issue);
        }

        private void addSuppressed(Issue issue) {
            if (issue instanceof ExceptionThrown ex) {
                this.addSuppressed(ex.exception());
            }
            if (issue instanceof AllOf ao) {
                ao.issues().forEach(this::addSuppressed);
            }
        }

        public Issue issue() {
            return issue;
        }
    }
}
