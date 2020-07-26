package com.github.alex1304.rdi.config;

import java.util.Optional;

class Value implements Injectable {
	
	private final Object value;
	
	Value(Object value) {
		this.value = value;
	}

	@Override
	public Class<?> getType() {
		return value.getClass();
	}
	
	@Override
	public Optional<Object> getValue() {
		return Optional.of(value);
	}
}
