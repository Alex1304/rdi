package com.github.alex1304.rdi.config;

import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

class ConstructorFactoryMethod extends AbstractFactoryMethod {

    ConstructorFactoryMethod(Class<?> owner, List<Injectable> constructorParams) {
        super(owner, null, null, constructorParams);
    }

    @Override
    MethodHandle findMethodHandle(Class<?> owner, @Nullable String methodName, @Nullable Class<?> returnType,
                                  List<Class<?>> paramTypes) throws ReflectiveOperationException {
        return MethodHandles.publicLookup().findConstructor(owner, MethodType.methodType(void.class, paramTypes));
    }

    @Override
    String userFriendlyRepresentation(Class<?> owner, @Nullable String methodName) {
        return owner.getName() + ".<init>";
    }
}
