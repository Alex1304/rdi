---
id: service-references
title: Service References
---

A **service** represents a Java object that is managed by the RDI container. In order to define the dependencies between the services, it is necessary to have a way to refer to them in a unique way. In most cases referring to a service by its class is sufficient. In some other cases however, you may want to have more than one (but a finite number of) instances for the same class in your application, possibly with different dependencies. This can be achieved by giving unique identifiers to these instances, which can be a simple name. RDI allows both ways to refer to services.

## Referring to a service by its class

The easiest way to define a reference to a service is by using the service class. With RDI it is done via the `ServiceReference#ofType(Class)` method. In this case, the name of the service will be equal to the name of the class as by `Class#getName()`:

```java
ServiceReference<A> ref = ServiceReference.ofType(A.class);
assertEquals(A.class.getName(), ref.getServiceName());
```

## Referring to a service by a unique name

You may as well give a custom name to the reference. This is done via `ServiceReference#of(String, Class)`:

```java
ServiceReference<A> ref1 = ServiceReference.of("a1", A.class);
ServiceReference<A> ref2 = ServiceReference.of("a2", A.class);
```

## Reference equality

The `ServiceReference` class implements `equals` and `hashCode` on the name field only, meaning that two references with the same name but of different type will be considered equal. As a result the following assertions will all be true:

```java
ServiceReference<A> ref1 = ServiceReference.of("a1", A.class);
ServiceReference<A> ref2 = ServiceReference.of("a2", A.class);
ServiceReference<A> ref3 = ServiceReference.ofType(A.class);
ServiceReference<A> ref4 = ServiceReference.of("foo", A.class);
ServiceReference<B> ref5 = ServiceReference.of("foo", B.class);

assertNotEquals(ref1, ref2);
assertNotEquals(ref1, ref3);
assertNotEquals(ref2, ref3);
assertNotEquals(ref3, ref4);
assertEquals(ref4, ref5);
```
