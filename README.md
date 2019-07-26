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
- **Be idiomatic** : we try to follow the principles of functional programming in Scala, and wrap all side-effects of the Java libraries into algrebraic effects.

## Installation

// TODO

## Usage

## Creating a TracingContext

## Tracing Http4s services

## Correlating your logs

### Automatic correlation for Datadog

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Make sure to follow our [Code of Conduct](./CODE_OF_CONDUCT.md)

## License

[MIT](./LICENSE.md)