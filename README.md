# commons

[![Test](https://github.com/stellarsunset/commons/actions/workflows/test.yaml/badge.svg)](https://github.com/stellarsunset/commons/actions/workflows/test.yaml)
[![codecov](https://codecov.io/github/stellarsunset/commons/graph/badge.svg?token=JIzptwIhbN)](https://codecov.io/github/stellarsunset/commons)

common classes that may make it into the public API of my open-source repos.

## usage

this repository provides a single, low-dependency place, for me to capture a small number of common APIs, e.g. container
classes, that multiple of my OSS projects use.

this saves me having to replicate them in multiple projects and prevents runtime conflicts for clients (and myself) when
using multiple of my OSS libraries in concert.

it's not expected that:

1. any clients use this repository directly
2. this repo grow significantly in size, unless I get into data structures or something

## quick hits

quick overview of a couple classes

### either & issue

container class with a left or a right value, `Either`. generally useful, but intended to mimic Go-style method returns
in tandem with `Issue`:

```java
interface MyFunction<U, V> {
    /** Return either the result of the operation, or an issue describing a problem that occurred. */
    Either<V, Issue> apply(U u);
}

String result = myFunction.apply("hello")
        .flatMapRight(i -> i instanceof CanFix ? Either.ofLeft("fixed") : Either.ofRight(i))
        // Explicit as follows, but Issue also has special handling in the no-argument version of Either.orThrowRight()
        .orThrowRight(Issue::asThrowable);
```

`Issue` is provided here for fluency when composing libraries that use this pattern. see docs on the classes for more 
detail + reasoning.