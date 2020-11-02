package com.github.alex1304.rdi.config;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.alex1304.rdi.RdiException;
import com.github.alex1304.rdi.finder.ServiceFinder;

/**
 * Contains all the configuration to create a service container. It stores the
 * metadata of each service necessary to achieve the dependency injection.
 * 
 * @see RdiConfig#builder()
 */
public class RdiConfig {
	
	private final Set<ServiceDescriptor> serviceDescriptors;

	private RdiConfig(Set<ServiceDescriptor> serviceDescriptors) {
		this.serviceDescriptors = serviceDescriptors;
	}

	public Set<ServiceDescriptor> getServiceDescriptors() {
		return serviceDescriptors;
	}
	
	/**
	 * Initializes a new {@link RdiConfig} builder.
	 * 
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}
	
	/**
	 * Delegates the discovery of services to the given {@link ServiceFinder} and
	 * creates a config with all services found.
	 * 
	 * @param serviceFinder the service finder
	 * @return a new {@link RdiConfig}
	 */
	public static RdiConfig fromServiceFinder(ServiceFinder serviceFinder) {
		return builder().fromServiceFinder(serviceFinder).build();
	}
	
	public static class Builder {
		
		private final Set<ServiceDescriptor> serviceDescriptors = new HashSet<>();
		
		/**
		 * Registers a new {@link ServiceDescriptor} containing all the dependency
		 * injection metadata for a particular service.
		 * 
		 * @param serviceDescriptor the service descriptor to register
		 * @return this builder
		 * @throws RdiException if the service represented by the given descriptor is
		 *                      already registered
		 */
		public Builder registerService(ServiceDescriptor serviceDescriptor) {
			requireNonNull(serviceDescriptor);
			checkAndAdd(serviceDescriptor);
			return this;
		}
		
		/**
		 * Delegates the discovery of services to the given {@link ServiceFinder} and
		 * registers all services found.
		 * 
		 * @param serviceFinder the service finder
		 * @return this builder
		 * @throws RdiException if at least one of the services discovered by the finder
		 *                      is already registered.
		 */
		public Builder fromServiceFinder(ServiceFinder serviceFinder) {
			requireNonNull(serviceFinder);
			serviceFinder.findServices().forEach(this::checkAndAdd);
			return this;
		}

		private void checkAndAdd(ServiceDescriptor descriptor) {
			if (!serviceDescriptors.add(descriptor)) {
				throw new RdiException("Duplicate service registered: " + descriptor.getServiceReference());
			}
		}

		/**
		 * Builds the {@link RdiConfig} instance with all the service descriptors
		 * registered at the moment this method is invoked.
		 * 
		 * @return a new {@link RdiConfig} instance
		 */
		public RdiConfig build() {
			return new RdiConfig(Collections.unmodifiableSet(serviceDescriptors));
		}
	}
}
