package com.github.alex1304.rdi.config;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

class StaticFactoryMethod extends AbstractFactoryMethod {

    StaticFactoryMethod(Class<?> owner, String methodName, Class<?> returnType, List<Injectable> params) {
        super(owner, methodName, returnType, params);
    }

    @Override
    MethodHandle findMethodHandle(Class<?> owner, String methodName, Class<?> returnType,
                                  List<Class<?>> paramTypes) throws ReflectiveOperationException {
        return MethodHandles.publicLookup().findStatic(owner, methodName, MethodType.methodType(returnType,
                paramTypes));
    }

    @Override
    String userFriendlyRepresentation(Class<?> owner, String methodName) {
        return owner.getName() + "." + methodName;
    }
}
