package com.github.alex1304.rdi;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * Refers to a specific service. A service is identified by a name and its
 * class. As such, several services of the same class may live in the
 * application, even if they are declared as singleton, as long as they have a
 * different name. If the service name is not important in your application, you
 * may refer to a service only by its class via
 * {@link ServiceReference#ofType(Class)}. In that case, the name of the service
 * will be the name of the class as by {@link Class#getName()}.
 * 
 * <p>
 * This class implements {@link #equals(Object)} and {@link #hashCode()} on the
 * name field only, meaning that in your application you are not supposed to use
 * two references with the same name but different classes. However, as said
 * earlier, the other way around is possible.
 * 
 * @param <T> the generic type of the service class
 * 
 * @see ServiceReference#of(String, Class)
 * @see ServiceReference#ofType(Class)
 */
public class ServiceReference<T> {
	
	private final String serviceName;
	private final Class<T> serviceClass;
	
	private ServiceReference(String serviceName, Class<T> serviceClass) {
		this.serviceName = serviceName;
		this.serviceClass = serviceClass;
	}
	
	/**
	 * Creates a new {@link ServiceReference} with the given name and class.
	 * 
	 * @param <T>          the generic type of the service class
	 * @param serviceName  the service name
	 * @param serviceClass the service class
	 * @return a new {@link ServiceReference}
	 */
	public static <T> ServiceReference<T> of(String serviceName, Class<T> serviceClass) {
		requireNonNull(serviceName);
		requireNonNull(serviceClass);
		return new ServiceReference<>(serviceName, serviceClass);
	}
	
	/**
	 * Creates a new {@link ServiceReference} with the given class. The name of the
	 * class as by {@link Class#getName()} will be used as name for this reference.
	 * 
	 * @param <T>          the generic type of the service class
	 * @param serviceClass the service class
	 * @return a new {@link ServiceReference}
	 */
	public static <T> ServiceReference<T> ofType(Class<T> serviceClass) {
		requireNonNull(serviceClass);
		return new ServiceReference<>(serviceClass.getName(), serviceClass);
	}

	/**
	 * Gets the name of the service that is referred to.
	 * 
	 * @return the service name
	 */
	public String getServiceName() {
		return serviceName;
	}
	
	/**
	 * Gets the class of the service that is referred to.
	 * 
	 * @return the service class
	 */
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
		return serviceName;
	}
}
