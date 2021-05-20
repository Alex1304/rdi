package com.github.alex1304.rdi.finder.annotation;

@RdiService
public class A {

    private final B b;
    private final String foo;
    private final int bar;

    @RdiFactory
    public A(B b, @RdiVal("hello") String foo, @RdiVal("42") int bar) {
        this.b = b;
        this.foo = foo;
        this.bar = bar;
    }

    public final B getB() {
        return b;
    }

    public final String getFoo() {
        return foo;
    }

    public final int getBar() {
        return bar;
    }

}
