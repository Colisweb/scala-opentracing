# Scala Opentracing
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](https://semver.org/).

More infos about this file : https://keepachangelog.com/

## [Unreleased] - no_due_date

## [v0.1.0] 2019.09.12

### Core

- Breaking : `com.colisweb.tracing.TracingContext.TracingContextBuilder` has been moved to `com.colisweb.tracing.TracingContextBuilder`
- `LoggingTracingContext` now prints tags to the console
- All tracing context companion objects now include a `get[...]TracingContextBuilder[F]` method that returns a `F[TracingContextBuilder[F]]` . The `TracingContext[F]` that this `TracingContextBuilder[F]` will build will have no parent span. This has been done for consitency with regard to the `DDTracingContext` which requires some effectful registration to be ran before the tracer can work properly.
- Code has been reorganised so all implicits can be imported with `com.colisweb.tracing.implicits._`
- There is now a `Tags` type alias for `Map[String, String]`
- Log correlation has been reorgnaised to support more than Datadog in the future

### Tapir integration
- One can now pass a `Http4sServerOptions` class to the `toTracedRoute` and `toTracedRouteRecoverErrors` 