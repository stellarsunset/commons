package io.github.stellarsunset.commons;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Simple class for working with one of two possible values in a return type.
 *
 * <p>This makes it easier to work with Go-like syntax in error handling, returning {@code Either<Value, Error>}. Often
 * the {@code Error} will be a sealed interface of known error conditions which can either be handled or down-converted
 * to {@link RuntimeException}s and thrown.
 */
public record Either<L, R>(Optional<L> left, Optional<R> right) {

    public static <L, R> Either<L, R> ofLeft(L left) {
        return new Either<>(Optional.of(left), Optional.empty());
    }

    public static <L, R> Either<L, R> ofRight(R right) {
        return new Either<>(Optional.empty(), Optional.of(right));
    }

    public Either {
        checkArgument(left.isEmpty() || right.isEmpty(), "Left and Right cannot both be present");
        checkArgument(left.isPresent() || right.isPresent(), "Left and Right cannot both be missing");
    }

    public <T> Either<T, R> mapLeft(Function<L, T> fn) {
        return new Either<>(left.map(fn), right);
    }

    public <T> Either<L, T> mapRight(Function<R, T> fn) {
        return new Either<>(left, right.map(fn));
    }

    /**
     * E.g. {@link Optional#flatMap(Function)}.
     *
     * <p>For composition with functions whose results are themselves an {@link Either}.
     */
    public <T> Either<T, R> flatMapLeft(Function<L, Either<T, R>> fn) {
        return right.isPresent() ? new Either<>(Optional.empty(), right) : fn.apply(left.orElseThrow());
    }

    /**
     * E.g. {@link Optional#flatMap(Function)}.
     *
     * <p>For composition with functions whose results are themselves an {@link Either}.
     */
    public <T> Either<L, T> flatMapRight(Function<R, Either<L, T>> fn) {
        return left.isPresent() ? new Either<>(left, Optional.empty()) : fn.apply(right.orElseThrow());
    }

    public Either<R, L> swap() {
        return new Either<>(right, left);
    }

    /**
     * Useful for coalescing an {@link Either} instance to a result type, e.g. a success or failure.
     *
     * <pre>
     *     Either<T, Exception> result = someMethod();
     *     return result.apply(t -> Result.SUCCESS, e -> Result.FAILURE);
     * </pre>
     */
    public <T> T apply(Function<L, T> lFn, Function<R, T> rFn) {
        return left.map(lFn).or(() -> right.map(rFn)).orElseThrow();
    }

    /**
     * Consume either the left or right side if present and do something with it, but doesn't require type coalescing.
     * Usually useful for logging the content of an {@link Either} before doing something with it.
     *
     * <pre>
     *     Either<T, Exception> result = someMethod();
     *     return result.peek(
     *         t -> log.info("Successfully computed result."),
     *         ex -> log.error("Failed during execution.", ex)
     *     );
     * </pre>
     */
    public Either<L, R> peek(Consumer<L> lFn, Consumer<R> rFn) {
        left.ifPresentOrElse(lFn, () -> right.ifPresent(rFn));
        return this;
    }

    /**
     * Shorthand for {@link #orThrowLeft(Function)} given the left-hand type is a subclass of {@link Exception}, this
     * does require that the left value be a {@link RuntimeException}.
     *
     * <p>To convert something to an exception and then throw see {@link #orThrowLeft(Function)}.
     *
     * <pre>
     *     Either<T, Exception> result = someMethod();
     *     T t = result.orThrowRight();
     * </pre>
     */
    public R orThrowLeft() {
        return orThrowLeft(left -> left instanceof RuntimeException rt ? rt : new NotAnExceptionTypeException(left));
    }

    /**
     *
     */
    public <E extends RuntimeException> R orThrowLeft(Function<L, E> toException) {
        if (left.isPresent()) {
            throw toException.apply(left.get());
        }
        return right.orElseThrow();
    }

    /**
     * See {@link #orThrowLeft()}.
     */
    public L orThrowRight() {
        return orThrowRight(right -> right instanceof RuntimeException rt ? rt : new NotAnExceptionTypeException(right));
    }

    /**
     * See {@link #orThrowLeft(Function)}.
     */
    public <E extends RuntimeException> L orThrowRight(Function<R, E> toException) {
        if (right.isPresent()) {
            throw toException.apply(right.get());
        }
        return left.orElseThrow();
    }

    public static class NotAnExceptionTypeException extends RuntimeException {
        public NotAnExceptionTypeException(Object object) {
            super(String.format("Unable to implicitly throw non-runtime exception type: %s", object.getClass()));
        }
    }
}
