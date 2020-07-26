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
		return new Ref(ref, ref.getServiceClass());
	}
	
	static <T> Injectable ref(ServiceReference<T> ref, Class<? super T> asSupertype) {
		return new Ref(ref, asSupertype);
	}
	
	static Injectable value(Object value) {
		return new Value(value, value.getClass());
	}
	
	static Injectable value(int value) {
		return new Value(value, int.class);
	}
	
	static Injectable value(long value) {
		return new Value(value, long.class);
	}
	
	static Injectable value(double value) {
		return new Value(value, double.class);
	}
	
	static Injectable value(char value) {
		return new Value(value, char.class);
	}
	
	static Injectable value(byte value) {
		return new Value(value, byte.class);
	}
	
	static Injectable value(short value) {
		return new Value(value, short.class);
	}
	
	static Injectable value(float value) {
		return new Value(value, float.class);
	}
	
	static Injectable value(boolean value) {
		return new Value(value, boolean.class);
	}
	
	static <T> Injectable value(T value, Class<? super T> asSupertype) {
		return new Value(value, asSupertype);
	}
}
