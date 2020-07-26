package com.github.alex1304.rdi;

import java.util.Objects;

public class ServiceReference<T> {
	
	private final String serviceName;
	private final Class<T> serviceClass;
	
	private ServiceReference(String serviceName, Class<T> serviceClass) {
		this.serviceName = serviceName;
		this.serviceClass = serviceClass;
	}
	
	public static <T> ServiceReference<T> of(String serviceName, Class<T> serviceClass) {
		return new ServiceReference<>(serviceName, serviceClass);
	}
	
	public static <T> ServiceReference<T> ofType(Class<T> serviceClass) {
		return new ServiceReference<>(serviceClass.getName(), serviceClass);
	}

	public String getServiceName() {
		return serviceName;
	}

	public Class<T> getServiceClass() {
		return serviceClass;
	}

	@Override
	public int hashCode() {
		return Objects.hash(serviceName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ServiceReference))
			return false;
		ServiceReference<?> other = (ServiceReference<?>) obj;
		return Objects.equals(serviceName, other.serviceName);
	}

	@Override
	public String toString() {
		return "ServiceReference{serviceName=" + serviceName + ", serviceClass=" + serviceClass + "}";
	}
}
