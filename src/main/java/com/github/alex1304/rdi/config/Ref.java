package com.github.alex1304.rdi.config;

import com.github.alex1304.rdi.ServiceReference;

class Ref implements Injectable {
	
	private final ServiceReference<?> ref;

	Ref(ServiceReference<?> ref) {
		this.ref = ref;
	}

	@Override
	public Class<?> getType() {
		return ref.getServiceClass();
	}
}
