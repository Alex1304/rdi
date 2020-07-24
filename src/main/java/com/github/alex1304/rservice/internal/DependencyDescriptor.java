package com.github.alex1304.rservice.internal;

import java.lang.invoke.MethodHandle;
import java.util.List;

class DependencyDescriptor {

	private final Class<?> clazz;
	private final boolean isSingleton;
	private final MethodHandle factoryMH;
	private final List<MethodHandle> setterMHs;
	private DependencyMode mode;
	
	public DependencyDescriptor(Class<?> clazz, boolean isSingleton, MethodHandle factoryMH,
			List<MethodHandle> setterMHs) {
		this.clazz = clazz;
		this.isSingleton = isSingleton;
		this.factoryMH = factoryMH;
		this.setterMHs = setterMHs;
		this.mode = DependencyMode.FACTORY;
	}
	
	Class<?> getClazz() {
		return clazz;
	}
	
	boolean isSingleton() {
		return isSingleton;
	}

	MethodHandle getFactoryMH() {
		return factoryMH;
	}

	List<MethodHandle> getSetterMHs() {
		return setterMHs;
	}

	DependencyMode getMode() {
		return mode;
	}

	void setMode(DependencyMode mode) {
		this.mode = mode;
	}

	@Override
	public String toString() {
		return "DependencyDescriptor{element=" + clazz.getSimpleName() + ", isSingleton=" + isSingleton + ", factoryMH=" + factoryMH
				+ ", setterMHs=" + setterMHs + ", mode=" + mode + "}";
	}
}