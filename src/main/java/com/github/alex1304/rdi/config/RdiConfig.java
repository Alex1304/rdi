package com.github.alex1304.rdi.config;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.alex1304.rdi.RdiException;

public class RdiConfig {
	
	private final Set<ServiceDescriptor> serviceDescriptors;

	private RdiConfig(Set<ServiceDescriptor> serviceDescriptors) {
		this.serviceDescriptors = serviceDescriptors;
	}

	public Set<ServiceDescriptor> getServiceDescriptors() {
		return serviceDescriptors;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private final Set<ServiceDescriptor> serviceDescriptors = new HashSet<>();
		
		public Builder registerService(ServiceDescriptor serviceDescriptor) {
			requireNonNull(serviceDescriptor);
			if (!serviceDescriptors.add(serviceDescriptor)) {
				throw new RdiException("Duplicate service registered: "
						+ serviceDescriptor.getServiceReference());
			}
			return this;
		}
		
		public RdiConfig build() {
			return new RdiConfig(Collections.unmodifiableSet(serviceDescriptors));
		}
	}
}
