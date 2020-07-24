package com.github.alex1304.rservice.internal;

import java.util.Objects;

class ClassWithParent {
	
	private final Class<?> parent;
	private final Class<?> element;
	
	ClassWithParent(Class<?> parent, Class<?> node) {
		this.parent = parent;
		this.element = node;
	}

	@Override
	public int hashCode() {
		return Objects.hash(element, parent);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ClassWithParent))
			return false;
		ClassWithParent other = (ClassWithParent) obj;
		return element == other.element && parent == other.parent;
	}
	
	@Override
	public String toString() {
		return (element == null ? "null" : element.getSimpleName())
				+ "(" + (parent == null ? "null" : parent.getSimpleName()) + ")";
	}
}