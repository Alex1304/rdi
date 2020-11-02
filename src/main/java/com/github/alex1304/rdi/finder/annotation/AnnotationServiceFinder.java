package com.github.alex1304.rdi.finder.annotation;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import com.github.alex1304.rdi.RdiException;
import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.FactoryMethod;
import com.github.alex1304.rdi.config.Injectable;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import com.github.alex1304.rdi.finder.ServiceFinder;

/**
 * A {@link ServiceFinder} that finds services from annotations, given a set of
 * classes.
 */
public class AnnotationServiceFinder implements ServiceFinder {
	
	private final Set<Class<?>> classes;

	private AnnotationServiceFinder(Set<Class<?>> classes) {
		this.classes = classes;
	}
	
	/**
	 * Creates a new {@link AnnotationServiceFinder} with the provided classes to
	 * parse annotations from.
	 * 
	 * @param classes the classes to parse annotations from
	 * @return a new {@link AnnotationServiceFinder}
	 */
	public static AnnotationServiceFinder create(Set<Class<?>> classes) {
		return new AnnotationServiceFinder(classes);
	}

	@Override
	public Set<ServiceDescriptor> findServices() {
		return classes.stream()
				.filter(clazz -> clazz.isAnnotationPresent(RdiService.class))
				.map(clazz -> {
					RdiService service = clazz.getAnnotation(RdiService.class);
					ServiceReference<?> ref = service != null && !service.value().isEmpty()
							? ServiceReference.of(service.value(), clazz)
							: ServiceReference.ofType(clazz);
					ServiceDescriptor.Builder builder = ServiceDescriptor.builder(ref);
					Function<Class<?>, FactoryMethod> factory = findStaticFactory(clazz)
							.orElseGet(() -> findConstructor(clazz).orElseGet(FactoryMethod::constructor));
					builder.setFactoryMethod(factory);
					// Add setters
					Arrays.stream(clazz.getDeclaredMethods())
							.filter(m -> m.isAnnotationPresent(RdiSetter.class)
									&& Modifier.isPublic(m.getModifiers())
									&& m.getParameterCount() == 1)
							.forEach(s -> builder.addSetterMethod(s.getName(),
											paramToInjectable(s.getParameters()[0]),
											s.getReturnType()));
					return builder.build();
				})
				.collect(Collectors.toSet());
	}
	
	private static Optional<Function<Class<?>, FactoryMethod>> findStaticFactory(Class<?> clazz) {
		return Arrays.stream(clazz.getDeclaredMethods())
				.filter(m -> m.isAnnotationPresent(RdiFactory.class)
						&& Modifier.isPublic(m.getModifiers())
						&& Modifier.isStatic(m.getModifiers())
						& Publisher.class.isAssignableFrom(m.getReturnType()) || m.getReturnType().equals(clazz))
				.findFirst()
				.map(m -> FactoryMethod.staticFactory(m.getName(), m.getReturnType(), paramsToInjectable(m)));
	}
	
	private static Optional<Function<Class<?>, FactoryMethod>> findConstructor(Class<?> clazz) {
		return Arrays.stream(clazz.getDeclaredConstructors())
				.filter(m -> m.isAnnotationPresent(RdiFactory.class)
						&& Modifier.isPublic(m.getModifiers()))
				.findFirst()
				.map(c -> FactoryMethod.constructor(paramsToInjectable(c)));
	}
	
	private static Injectable[] paramsToInjectable(Executable executable) {
		return Arrays.stream(executable.getParameters())
				.map(AnnotationServiceFinder::paramToInjectable)
				.toArray(Injectable[]::new);
	}
	
	private static Injectable paramToInjectable(Parameter p) {
		Injectable injectable;
		RdiRef ref = p.getAnnotation(RdiRef.class);
		if (ref != null) {
			injectable = Injectable.ref(ServiceReference.of(ref.value(), p.getType()));
		} else {
			RdiVal val = p.getAnnotation(RdiVal.class);
			if (val != null) {
				if (p.getType().equals(boolean.class)) {
					injectable = Injectable.value(Boolean.parseBoolean(val.value()));
				} else if (p.getType().equals(byte.class)) {
					injectable = Injectable.value(Byte.parseByte(val.value()));
				} else if (p.getType().equals(char.class)) {
					injectable = Injectable.value(val.value().charAt(0));
				} else if (p.getType().equals(double.class)) {
					injectable = Injectable.value(Double.parseDouble(val.value()));
				} else if (p.getType().equals(float.class)) {
					injectable = Injectable.value(Float.parseFloat(val.value()));
				} else if (p.getType().equals(int.class)) {
					injectable = Injectable.value(Integer.parseInt(val.value()));
				} else if (p.getType().equals(long.class)) {
					injectable = Injectable.value(Long.parseLong(val.value()));
				} else if (p.getType().equals(short.class)) {
					injectable = Injectable.value(Short.parseShort(val.value()));
				} else if (p.getType().equals(String.class)) {
					injectable = Injectable.value(val.value(), String.class);
				} else if (p.getType().equals(CharSequence.class)) {
					injectable = Injectable.value(val.value(), CharSequence.class);
				} else if (p.getType().equals(Object.class)) {
					injectable = Injectable.value(val.value(), Object.class);
				} else {
					throw new RdiException("Unsupported parameter type for @RdiVal, only primitive "
							+ "types and supertypes of String (included) are supported. Given: " + p.getType());
				}
			} else {
				injectable = Injectable.ref(ServiceReference.ofType(p.getType()));
			}
		}
		return injectable;
	}

}
