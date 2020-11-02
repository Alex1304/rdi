package com.github.alex1304.rdi.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.alex1304.rdi.RdiException;
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
	private final List<SetterMethod> setterMethods;
	
	private ServiceDescriptor(ServiceReference<?> ref, boolean isSingleton, FactoryMethod factoryMethod,
			List<SetterMethod> setterMethods) {
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
	public List<SetterMethod> getSetterMethods() {
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
	 * Convenience method to create a descriptor for a service that does not have
	 * any dependencies. The class must define a public no-arg constructor for it to
	 * work. This is equivalent to:
	 * 
	 * <pre>
	 * ServiceDescriptor.builder(serviceReference).setSingleton(isSingleton).build();
	 * </pre>
	 * 
	 * <p>
	 * This variant allows to specify whether the service should be a singleton. If
	 * you want to define dependencies for the service, see
	 * {@link ServiceDescriptor#builder(ServiceReference)}.
	 * 
	 * @param serviceReference the service reference
	 * @param isSingleton      whether the service should be instantiated only once
	 *                         or if a new instance should be created every time it
	 *                         is requested
	 * @return a new ServiceDescriptor
	 * @throws RdiException if the class does not have a public no-arg constructor
	 */
	public static ServiceDescriptor standalone(ServiceReference<?> serviceReference, boolean isSingleton) {
		return new ServiceDescriptor(serviceReference, isSingleton, FactoryMethod.constructor()
				.apply(serviceReference.getServiceClass()), Collections.emptyList());
	}
	
	/**
	 * Convenience method to create a descriptor for a service that does not have
	 * any dependencies. The class must define a public no-arg constructor for it to
	 * work. This is equivalent to:
	 * 
	 * <pre>
	 * ServiceDescriptor.builder(serviceReference).build();
	 * </pre>
	 * 
	 * <p>
	 * This variant will configure the service as a singleton by default. If you do
	 * not want it to be singeton, you may use the overload
	 * {@link ServiceDescriptor#standalone(ServiceReference, boolean)} instead. If
	 * you want to define dependencies for the service, see
	 * {@link ServiceDescriptor#builder(ServiceReference)}.
	 * 
	 * @param serviceReference the service reference
	 * @return a new ServiceDescriptor
	 * @throws RdiException if the class does not have a public no-arg constructor
	 */
	public static ServiceDescriptor standalone(ServiceReference<?> serviceReference) {
		return standalone(serviceReference, true);
	}

	/**
	 * Creates a new builder for a {@link ServiceDescriptor}.
	 * 
	 * <p>
	 * The builder will allow you to define all the dependencies for the service, in
	 * factory methods as well as in setters. If your service does not require any
	 * dependency, you may prefer to use
	 * {@link ServiceDescriptor#standalone(ServiceReference)} and
	 * {@link ServiceDescriptor#standalone(ServiceReference, boolean)}.
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
		private Function<Class<?>, ? extends FactoryMethod> factoryMethod;
		private final List<Supplier<SetterMethod>> setterMethods = new ArrayList<>();
		
		private Builder(ServiceReference<?> ref) {
			this.ref = ref;
			this.defaultFactoryMethod = FactoryMethod.constructor();
			this.factoryMethod = defaultFactoryMethod;
		}
		
		/**
		 * Sets whether to configure the service as singleton. If true, the service will
		 * only be instantiated once and all other services depending on it will share
		 * the same instance. Defaults to <code>true</code>.
		 * 
		 * @param isSingleton true if singleton, false if not
		 * @return this builder
		 */
		public Builder setSingleton(boolean isSingleton) {
			this.isSingleton = isSingleton;
			return this;
		}
		
		/**
		 * Sets the factory method that will instantiate the service, with the potential
		 * dependencies to inject. Defaults to a constructor with no arguments
		 * (<code>FactoryMethod.constructor()</code>)
		 * 
		 * @param factoryMethod the factory method to set. The appropriate
		 *                      {@link Function} can be obtained by using one of the
		 *                      static methods of the {@link FactoryMethod} interface.
		 * @return this builder
		 */
		public Builder setFactoryMethod(@Nullable Function<Class<?>, ? extends FactoryMethod> factoryMethod) {
			this.factoryMethod = factoryMethod == null ? defaultFactoryMethod : factoryMethod;
			return this;
		}

		/**
		 * Adds a setter method that is capable of injecting an additional dependency
		 * after instantiation.
		 * 
		 * <p>
		 * This method may be called multiple times for the same setter. This is useful
		 * if your setter actually represents a <code>add</code> operation.
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
		 * This method may be called multiple times for the same setter. This is useful
		 * if your setter actually represents a <code>add</code> operation.
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
			setterMethods.add(() -> new SetterMethod(ref.getServiceClass(), name, returnType, param));
			return this;
		}
		
		/**
		 * Builds the service descriptor.
		 * 
		 * @return a newly built {@link ServiceDescriptor}
		 * @throws RdiException if one of the injection methods cannot be found in the
		 *                      target class or are not public
		 */
		public ServiceDescriptor build() {
			return new ServiceDescriptor(ref, isSingleton, factoryMethod.apply(ref.getServiceClass()),
					Collections.unmodifiableList(setterMethods.stream()
							.map(Supplier::get)
							.collect(Collectors.toList())));
		}
	}
}