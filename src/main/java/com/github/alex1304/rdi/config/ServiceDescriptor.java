package com.github.alex1304.rdi.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.github.alex1304.rdi.ServiceReference;

import reactor.util.annotation.Nullable;

public class ServiceDescriptor {

	private final ServiceReference<?> ref;
	private final boolean isSingleton;
	private final FactoryMethod factoryMethod;
	private final Set<SetterMethod> setterMethods;
	
	private ServiceDescriptor(ServiceReference<?> ref, boolean isSingleton, FactoryMethod factoryMethod,
			Set<SetterMethod> setterMethods) {
		this.ref = ref;
		this.isSingleton = isSingleton;
		this.factoryMethod = factoryMethod;
		this.setterMethods = setterMethods;
	}
	
	public ServiceReference<?> getServiceReference() {
		return ref;
	}
	
	public boolean isSingleton() {
		return isSingleton;
	}

	public FactoryMethod getFactoryMethod() {
		return factoryMethod;
	}

	public Set<SetterMethod> getSetterMethods() {
		return setterMethods;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ref);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ServiceDescriptor))
			return false;
		ServiceDescriptor other = (ServiceDescriptor) obj;
		return Objects.equals(ref, other.ref);
	}

	@Override
	public String toString() {
		return "ServiceDescriptor{ref=" + ref + ", isSingleton=" + isSingleton + ", factoryMethod=" + factoryMethod
				+ ", setterMethods=" + setterMethods + "}";
	}

	public static Builder builder(ServiceReference<?> ref) {
		return new Builder(ref);
	}
	
	public static class Builder {
		
		private final ServiceReference<?> ref;
		private boolean isSingleton;
		private final FactoryMethod defaultFactoryMethod;
		private FactoryMethod factoryMethod;
		private final Set<SetterMethod> setterMethods = new HashSet<>();
		
		private Builder(ServiceReference<?> ref) {
			this.ref = ref;
			this.defaultFactoryMethod = FactoryMethod.constructor(ref.getServiceClass());
			this.factoryMethod = defaultFactoryMethod;
		}
		
		public Builder setSingleton(boolean isSingleton) {
			this.isSingleton = isSingleton;
			return this;
		}
		
		public Builder setFactoryMethod(@Nullable FactoryMethod factoryMethod) {
			this.factoryMethod = factoryMethod == null ? defaultFactoryMethod : factoryMethod;
			return this;
		}
		
		public Builder addSetterMethod(String name, Injectable paramType) {
			return addSetterMethod(name, paramType, void.class);
		}
		
		public Builder addSetterMethod(String name, Injectable paramType, Class<?> returnType) {
			setterMethods.add(new SetterMethod(ref.getServiceClass(), name, returnType, paramType));
			return this;
		}
		
		public ServiceDescriptor build() {
			return new ServiceDescriptor(ref, isSingleton,
					factoryMethod, Collections.unmodifiableSet(setterMethods));
		}
	}
}