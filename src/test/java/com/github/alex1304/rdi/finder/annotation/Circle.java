package com.github.alex1304.rdi.finder.annotation;

@RdiService(as = Shape.class)
public class Circle implements Shape {

    @Override
    public String value() {
        return "circle";
    }
}
