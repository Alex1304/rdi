package com.github.alex1304.rdi.config;

import java.util.Optional;

import com.github.alex1304.rdi.ServiceReference;

public interface Injectable {
	
	Class<?> getType();
	
	default Optional<Object> getValue() {
		return Optional.empty();
	}
	
	default Optional<ServiceReference<?>> getReference() {
		return Optional.empty();
	}
	
	static Injectable ref(ServiceReference<?> ref) {
		return new Ref(ref);
	}
	
	static Injectable value(Object value) {
		return new Value(value);
	}
}
