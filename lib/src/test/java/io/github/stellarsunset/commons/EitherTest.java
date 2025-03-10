package io.github.stellarsunset.commons;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EitherTest {

    @Test
    void testFactoryMethods() {
        assertAll(
                () -> assertEquals(Optional.of(1), Either.ofLeft(1).left(), "Left"),
                () -> assertEquals(Optional.empty(), Either.ofLeft(1).right(), "Left, get Right"),
                () -> assertEquals(Optional.of(2), Either.ofRight(2).right(), "Right"),
                () -> assertEquals(Optional.empty(), Either.ofRight(2).left(), "Right, get Left"),
                () -> assertThrows(IllegalArgumentException.class, () -> new Either<>(Optional.empty(), Optional.empty()), "Empty"),
                () -> assertThrows(IllegalArgumentException.class, () -> new Either<>(Optional.of(1), Optional.of(2)), "Both")
        );
    }

    @Test
    void testMap() {
        assertAll(
                () -> assertEquals(Optional.of(2), Either.ofLeft(1).mapLeft(i -> i + 1).left(), "MapLeft"),
                () -> assertEquals(Optional.of(0), Either.ofRight(1).mapRight(i -> i - 1).right(), "MapRight")
        );
    }

    @Test
    void testSwap() {
        assertAll(
                () -> assertEquals(Either.ofRight(1), Either.ofLeft(1).swap(), "Swap Left"),
                () -> assertEquals(Either.ofLeft(1), Either.ofRight(1).swap(), "Swap Right"),
                () -> assertEquals(Either.ofLeft(1), Either.ofLeft(1).swap().swap(), "Swap Swap")
        );
    }

    @Test
    void testApply() {
        assertEquals(1, Either.ofLeft(1).<Integer>apply(i -> i, r -> 2));
    }

    @Test
    void testOrThrow() {
        var exception = new IllegalArgumentException();
        assertAll(
                () -> assertEquals(1, Either.ofLeft(1).orThrowRight(), "(1, null).orThrowRight()"),
                () -> assertThrows(IllegalArgumentException.class, () -> Either.ofRight(exception).orThrowRight(), "(null, exception).orThrowRight()"),
                () -> assertThrows(IllegalArgumentException.class, () -> Either.ofRight(1).orThrowRight(i -> exception), "(null, 1).orThrowRight(...)"),
                () -> assertThrows(Either.NotAnExceptionTypeException.class, () -> Either.ofRight(1).orThrowRight(), "(null, 1).orThrowRight()"),
                () -> assertEquals(2, Either.ofRight(2).orThrowLeft(), "(null, 2).orThrowLeft()"),
                () -> assertThrows(IllegalArgumentException.class, () -> Either.ofLeft(exception).orThrowLeft(), "(exception, null).orThrowLeft()"),
                () -> assertThrows(IllegalArgumentException.class, () -> Either.ofLeft(2).orThrowLeft(i -> exception), "(2, null).orThrowLeft(...)"),
                () -> assertThrows(Either.NotAnExceptionTypeException.class, () -> Either.ofLeft(2).orThrowLeft(), "(2, null).orThrowLeft()")
        );
    }
}
