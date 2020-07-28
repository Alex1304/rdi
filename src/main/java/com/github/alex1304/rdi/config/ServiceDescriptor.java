package com.github.alex1304.rdi.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.github.alex1304.rdi.ServiceReference;

import reactor.util.annotation.Nullable;

/**
 * Contains all the metadata on how to instantiate a service and how to inject
 * all the dependencies it needs.
 * 
 * <p>
 * This class implements {@link #equals(Object)} and {@link #hashCode()} on the
 * service reference, meaning that there should be only one descriptor per
 * service container and per service.
 * 
 * @see ServiceDescriptor#builder(ServiceReference)
 */
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
	
	/**
	 * Gets the service reference targeted by this descriptor.
	 * 
	 * @return the service reference
	 */
	public ServiceReference<?> getServiceReference() {
		return ref;
	}
	
	/**
	 * Gets whether the service is configured as singleton.
	 * 
	 * @return a boolean
	 */
	public boolean isSingleton() {
		return isSingleton;
	}

	/**
	 * Gets information about the factory method of the service that will be subject to dependency injection.
	 * 
	 * @return the factory method
	 */
	public FactoryMethod getFactoryMethod() {
		return factoryMethod;
	}

	/**
	 * Gets information about all the setter methods of the service that will be
	 * subject to dependency injection.
	 * 
	 * @return the setter methods
	 */
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

	/**
	 * Creates a new builder for a {@link ServiceDescriptor}.
	 * 
	 * @param ref the service reference targeted by the descriptor to build
	 * @return a new builder
	 */
	public static Builder builder(ServiceReference<?> ref) {
		return new Builder(ref);
	}
	
	public static class Builder {
		
		private final ServiceReference<?> ref;
		private boolean isSingleton = true;
		private final Function<Class<?>, FactoryMethod> defaultFactoryMethod;
		private Function<Class<?>, FactoryMethod> factoryMethod;
		private final Set<SetterMethod> setterMethods = new HashSet<>();
		
		private Builder(ServiceReference<?> ref) {
			this.ref = ref;
			this.defaultFactoryMethod = FactoryMethod.constructor();
			this.factoryMethod = defaultFactoryMethod;
		}
		
		/**
		 * Sets whether to configure the service as singleton. If true, the service will
		 * only be instantiated once and all other services depending on it will share
		 * the same instance.
		 * 
		 * @param isSingleton true if singleton, false if not
		 * @return this builder
		 */
		public Builder setSingleton(boolean isSingleton) {
			this.isSingleton = isSingleton;
			return this;
		}
		
		/**
		 * Sets the factory method that will instantiate the service,
		 * with the potential dependencies to inject.
		 * 
		 * @param factoryMethod the factory method to set. The appropriate {@link Function} can be
		 *                      obtained by using one of the static methods of the
		 *                      {@link FactoryMethod} interface.
		 * @return this builder
		 */
		public Builder setFactoryMethod(@Nullable Function<Class<?>, FactoryMethod> factoryMethod) {
			this.factoryMethod = factoryMethod == null ? defaultFactoryMethod : factoryMethod;
			return this;
		}

		/**
		 * Adds a setter method that is capable of injecting an additional dependency
		 * after instantiation.
		 * 
		 * <p>
		 * This assumes that the setter has a <code>void</code> return type. If it is
		 * not the case, use {@link #addSetterMethod(String, Injectable, Class)} instead
		 * to specify a return type for the setter method.
		 * 
		 * @param name  the setter name
		 * @param param the injectable parameter describing the dependency to inject in
		 *              the setter
		 * @return this builder
		 */
		public Builder addSetterMethod(String name, Injectable param) {
			return addSetterMethod(name, param, void.class);
		}
		
		/**
		 * Adds a setter method that is capable of injecting an additional dependency
		 * after instantiation.
		 * 
		 * <p>
		 * This overload allows you to specify a return type for the setter method. In
		 * most cases it will be <code>void</code>, if that's the case you may prefer
		 * {@link #addSetterMethod(String, Injectable)} with makes this assumption.
		 * 
		 * @param name  the setter name
		 * @param param the injectable parameter describing the dependency to inject in
		 *              the setter
		 * @param returnType the return type of the setter method
		 * @return this builder
		 */
		public Builder addSetterMethod(String name, Injectable param, Class<?> returnType) {
			setterMethods.add(new SetterMethod(ref.getServiceClass(), name, returnType, param));
			return this;
		}
		
		/**
		 * Builds the service descriptor.
		 * 
		 * @return a newly built {@link ServiceDescriptor}
		 */
		public ServiceDescriptor build() {
			return new ServiceDescriptor(ref, isSingleton, factoryMethod.apply(ref.getServiceClass()),
					Collections.unmodifiableSet(setterMethods));
		}
	}
}