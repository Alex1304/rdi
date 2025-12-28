package com.github.alex1304.rdi.config;

import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Objects;

class StaticFactoryMethod extends AbstractFactoryMethod {

    StaticFactoryMethod(Class<?> owner, String methodName, Class<?> returnType, List<Injectable> params) {
        super(owner, methodName, returnType, params);
    }

    @Override
    MethodHandle findMethodHandle(Class<?> owner, @Nullable String methodName, @Nullable Class<?> returnType,
                                  List<Class<?>> paramTypes) throws ReflectiveOperationException {
        return MethodHandles.publicLookup().findStatic(owner, Objects.requireNonNull(methodName),
                MethodType.methodType(returnType, paramTypes));
    }

    @Override
    String userFriendlyRepresentation(Class<?> owner, @Nullable String methodName) {
        return owner.getName() + "." + methodName;
    }
}
