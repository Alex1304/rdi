package com.github.alex1304.rdi.config;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.alex1304.rdi.RdiException;

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
	
	public static class Builder {
		
		private final Set<ServiceDescriptor> serviceDescriptors = new HashSet<>();
		
		/**
		 * Registers a new {@link ServiceDescriptor} containing all the dependency
		 * injection metadata for a particular service.
		 * 
		 * @param serviceDescriptor the service descriptor to register
		 * @return this builder
		 */
		public Builder registerService(ServiceDescriptor serviceDescriptor) {
			requireNonNull(serviceDescriptor);
			if (!serviceDescriptors.add(serviceDescriptor)) {
				throw new RdiException("Duplicate service registered: "
						+ serviceDescriptor.getServiceReference());
			}
			return this;
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
