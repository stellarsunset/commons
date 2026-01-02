package io.github.stellarsunset.commons;

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
     * Returns the {@link Issue} wrapped as a {@link RuntimeException} that can be directly thrown.
     *
     * <p>A sensible default implementation is provided here to cover most cases, but this can and should be overwritten
     * for any custom {@link Issue} subtypes that may want to throw their own dedicated exception shape.
     */
    default RuntimeException asException() {
        return new AsThrowable(this);
    }

    /**
     * There is no issue.
     *
     * <p>Defined centrally as it <i>may</i> be useful to have this notion centralized.
     */
    record Nothing() implements Issue {
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
     */
    record ExceptionThrown(String message, Exception exception) implements Issue {

        public ExceptionThrown(Exception exception) {
            this("An unexpected exception was thrown.", exception);
        }
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
            super("Encountered an issue in program execution: " + issue);
            this.issue = requireNonNull(issue);

            // Special-case ExceptionThrown to add the wrapped exception as suppressed on the final exception
            if (issue instanceof ExceptionThrown ex) {
                this.addSuppressed(ex.exception());
            }
        }

        public Issue issue() {
            return issue;
        }
    }
}
