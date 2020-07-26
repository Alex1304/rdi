package com.github.alex1304.rdi.config;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

class ConstructorFactoryMethod extends AbstractFactoryMethod {
	
	ConstructorFactoryMethod(Class<?> owner, List<Injectable> constructorParams) {
		super(owner, null, null, constructorParams);
	}

	@Override
	MethodHandle findMethodHandle(Class<?> owner, String methodName, Class<?> returnType,
			List<Class<?>> paramTypes) throws ReflectiveOperationException {
		return MethodHandles.publicLookup().findConstructor(owner, MethodType.methodType(void.class, paramTypes));
	}
}
