![Scala Opentracing](./logo.png)

# Scala Opentracing

Scala Opentracing is a Scala  wrapper around the Opentracing library for Java, along with utilities to test Http4s applications. It was
developed specifically to address our needs for Datadog APM instrumentation at Colisweb.

It is being used in production at Colisweb.

## Goals

- **Provide Datadog instrumentation for our Scala applications** : this tracing library should work with any Opentracing compatible tracer,
like Jaeger, but it has only been tested with Datadog so far.
- **Stay out of the way** : we don't want the tracing system to get in the way of our business logic, so
- we try to reduce to a minimum the amount of boilerplate required to use this library. Furthermore, Spans
are automatically closed by a `Resource` from Cats effect for safety and convenience.
- **Be idiomatic** : we try to follow the principles of functional programming in Scala, and wrap all side-effects of the Java libraries into algebraic effects.

## Installation

// TODO

## Usage

## Creating a TracingContext explicitly

A `TracingContext[F[_]]` represents some unit of works associated with a unique `spanId` and which can spawn child units of work. This library provides
three instances of `TracingContext` so far: `OpenTracingContext`, `DDTracingContext` and `NoOpTracingContext` that does nothing in particular.

If you use Datadog, create a `DDTracingContext`, otherwise use `OpenTracingContext`. In both cases, you will need some `Tracer`, provided by whatever tracing
library you use (Jaeger, Datadog ...).

In examples, we'll use `IO` from Cats effect as our `F` but you can use any monad that satisfies the `Sync` Typeclass, like `Task` from Monix.

```scala
import io.opentracing._
import com.colisweb.tracing._

val tracer: Tracer = ???
val tracingContextBuilder = OpenTracingContext[IO, Tracer, Span](tracer) _
```

Once you have a `TracingContextBuilder[F[_]]`, you can use to wrap your computations.

```scala
// You can pass tags as a Map[String, String]
val result: IO[Int] = tracingContextBuilder("Heavy mathematics", Map.empty) use { _ =>
  IO { 42 - 5 }
}
```

Notice how the context is wrapped inside a `Resource[IO, TracingContext]`. This means the span will
automatically closed at the end of the computation. You can use the `TracingContext` you get from the
`Resource` to create a child computation :

```scala
val result: IO[Int] = tracingContextBuilder("Parent context", Map.empty) use { parentCtx =>
  // Some work here ...
  parentCtx.childSpan("Child context") use { _ =>
    IO { /* And some work there */ }
  } *> parentCtx.childSpan("Sibling context") use { _ =>
    IO { 20 + 20 }
  }
}

// Result value will 20. The Spans graph will look like this :
// <------------------ Parent context ------------------>
//   <---- Child context ----> <---- Sibling context -->
```

If you don't need to create child contexts, you can import the `wrap` convenience method
by importing `com.colisweb.tracing.implicits._`.

```scala
import com.colisweb.implicits._

val result: IO[Int] = tracingContextBuilder("Parent context", Map.empty) wrap IO {
  // Some work
  78 + 12
}
```

## Tracing Http4s services

## Correlating your logs

### Automatic correlation for Datadog

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Make sure to follow our [Code of Conduct](./CODE_OF_CONDUCT.md)

## License

[MIT](./LICENSE.md)