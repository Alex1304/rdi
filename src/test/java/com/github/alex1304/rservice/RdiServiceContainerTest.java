package com.github.alex1304.rservice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.alex1304.rdi.RdiServiceContainer;
import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.RdiConfig;
import com.github.alex1304.rdi.config.ServiceDescriptor;

import reactor.core.publisher.Hooks;

class RdiServiceContainerTest {
	
	private static final ServiceReference<A> A = ServiceReference.ofType(A.class);
	private static final ServiceReference<B> B = ServiceReference.ofType(B.class);
	private static RdiConfig conf1;
	
	@BeforeAll
	static void setUpBeforeClass() {
		Hooks.onOperatorDebug();
		conf1 = RdiConfig.builder()
				.registerService(ServiceDescriptor.builder(A).build())
				.build();
	}
	
	@Test
	void testMinimalist() {
		assertDoesNotThrow(() -> {
			RdiServiceContainer cont1 = RdiServiceContainer.create(conf1);
			A a = cont1.getService(A).block();
			assertNotNull(a);
		});
	}
	
	public static class A {
		
		public A() {
		}
		
		public A(A a) {
		}
		
		public A(B b) {
		}
		
		public void setB(B b) {
		}
	}
	
	public static class B {
		public B() {
		}
		
		public B(A a) {
		}
		
		public void setA(A a) {
		}
	}
}


