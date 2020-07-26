package com.github.alex1304.rdi.config;

import java.util.Optional;

class Value implements Injectable {
	
	private final Object value;
	private final Class<?> type;
	
	Value(Object value, Class<?> type) {
		this.value = value;
		this.type = type;
	}

	@Override
	public Class<?> getType() {
		return type;
	}
	
	@Override
	public Optional<Object> getValue() {
		return Optional.of(value);
	}

	@Override
	public String toString() {
		return "Value{value=" + value + "[" + type + "]}";
	}
}
