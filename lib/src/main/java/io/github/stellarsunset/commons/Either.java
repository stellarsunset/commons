package io.github.stellarsunset.commons;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Simple class for working with one of two possible values in a return type.
 *
 * <p>While generally useful, this class is primarily intended to be used to simulate Go-style method returns.
 *
 * <p>In Go, convention is for functions to return as follows:
 * <pre>{@code
 * import "package"
 *
 * result, err := package.MyFunction()
 * if err != nil {
 *     // handle it, or just panic throwing a runtime exception
 *     panic(err)
 * }
 * }</pre>
 *
 * <p>In Java, which doesn't have multiple assignment, this can be clunkier. {@link Either} is an imperfect attempt to
 * make this slightly better, specifically when used in conjunction with {@link Issue}:
 * <pre>{@code
 * interface MyFunction<U, V> {
 *     Either<V, Issue> apply(U u);
 * }
 *
 * // Avoid having to throw and handle checked exceptions in try {} catch {}
 * var either = myFunction.apply("hello")
 *     .flatMapRight(i -> i instanceof Fixable ? Either.ofLeft("fixed") : Either.ofRight(i));
 *
 * // Explicit as follows, but Issue also has special handling in Either.orThrowRight()
 * var result = either.orThrowRight(Issue::asThrowable);
 * }</pre>
 *
 * <p>The above blends a bit of the functional style most Java devs are used to with some of the upside of the Go-like
 * syntax namely:
 * <ul>
 *     <li>Readability - in that we avoid exceptions (checked or otherwise) and the try-catch song-and-dance everywhere</li>
 *     <li>Safety - clients can extend {@link Issue} (or write their own type) that's {@code sealed}, and enumerate a
 *     collection of known {@link Issue}s, some of which may be handleable before invoking {@link Either#orThrowRight()}</li>
 * </ul>
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
     * <pre>{@code
     *     Either<T, Exception> result = someMethod();
     *     return result.apply(t -> Result.SUCCESS, e -> Result.FAILURE);
     * }</pre>
     */
    public <T> T apply(Function<L, T> lFn, Function<R, T> rFn) {
        return left.map(lFn).or(() -> right.map(rFn)).orElseThrow();
    }

    /**
     * Consume either the left or right side if present and do something with it, but doesn't require type coalescing.
     * Usually useful for logging the content of an {@link Either} before doing something with it.
     *
     * <pre>{@code
     *     Either<T, Exception> result = someMethod();
     *     return result.peek(
     *         t -> log.info("Successfully computed result."),
     *         ex -> log.error("Failed during execution.", ex)
     *     );
     * }</pre>
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
     * <pre>{@code
     *     Either<T, Exception> result = someMethod();
     *     T t = result.orThrowRight();
     * }</pre>
     *
     * <p>There is special handling for {@link Issue}s, which can be thrown without a function to map it to an exception.
     */
    public R orThrowLeft() {
        return orThrowLeft(left -> left instanceof RuntimeException rt ? rt : left instanceof Issue i ? i.asException() : new NotAnExceptionTypeException(left));
    }

    /**
     * See {@link #orThrowLeft()}.
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
        return orThrowRight(right -> right instanceof RuntimeException rt ? rt : right instanceof Issue i ? i.asException() : new NotAnExceptionTypeException(right));
    }

    /**
     * See {@link #orThrowLeft()}.
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
