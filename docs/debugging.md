---
id: debugging
title: Debugging
---

Since RDI uses [Reactor Core](https://projectreactor.io), it can take advantage from many debugging tools.

## Logging

Reactor comes with `reactor.util.Logger`, which can abstract the logging solution used by the application, according to what is present in the classpath. If you have an implementation of SLF4J, it will detect it and use it. Otherwise, it will use the native logger present in the JDK.

The default container of RDI logs a few things at the DEBUG level. Currently there are two loggers available:

* `rdi.resolver.assembly`: logs everything that happens when **creating the container**. It prints information on the dependency graph that is being constructed and the assembly of the reactive chains for each service.
* `rdi.resolver.subscription`: logs everything happening **at subscription time**. It delivers an output when a service instance has been created, when a dependency has been injected, when a setter is being invoked, when a singleton is being cached, and when the service is fully initialized and ready to be returned.

## Debugging reactive chains

Debugging what happens in reactive chain can become a hassle especially if you're a beginner at reactive programming and if you don't know the useful tools.

You can read [this article](https://spring.io/blog/2019/03/28/reactor-debugging-experience) on the Spring blog, which explains well the subtleties of Reactor and what tools exist to achieve debugging.
