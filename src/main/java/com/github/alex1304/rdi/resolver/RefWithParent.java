package com.github.alex1304.rdi.resolver;

import java.util.Objects;

import com.github.alex1304.rdi.ServiceReference;

class RefWithParent {
	
	private final ServiceReference<?> parent;
	private final ServiceReference<?> element;
	
	RefWithParent(ServiceReference<?> parent, ServiceReference<?> element) {
		this.parent = parent;
		this.element = element;
	}

	@Override
	public int hashCode() {
		return Objects.hash(element, parent);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof RefWithParent))
			return false;
		RefWithParent other = (RefWithParent) obj;
		return element == other.element && parent == other.parent;
	}
	
	@Override
	public String toString() {
		return element + "(" + parent + ")";
	}
}