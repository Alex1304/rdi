package com.github.alex1304.rservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.alex1304.rservice.annotation.RdiInject;
import com.github.alex1304.rservice.annotation.RdiSingleton;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

class RdiServiceContainerTest {
	
	private RdiServiceContainer rsc, rsc2, rsc3;
	
	@BeforeAll
	static void setUpBeforeClass() {
		Hooks.onOperatorDebug();
	}
	
	@BeforeEach
	void setUp() {
		rsc = RdiServiceContainer.annotationBased(A.class);
		rsc2 = RdiServiceContainer.annotationBased(H.class);
		rsc3 = RdiServiceContainer.annotationBased(K.class);
	}
	
	@Test
	void testGetService() {
		System.out.println("testGetService");
		A a = rsc.getService(A.class).block(Duration.ofSeconds(10));
		B b = a.b;
		C c = a.b.c;
		D d = a.b.c.d;
		assertNotNull(a);
		assertNotNull(b);
		assertNotNull(c);
		assertNotNull(d);
	}
	
	@Test
	void testSingleton() {
		System.out.println("testSingleton");
		A a = rsc.getService(A.class).block();
		C c = rsc.getService(C.class).block();
		assertTrue(a.b.c == c);
		assertFalse(a.b.d == a.b.c.d);
	}
	
	@Test
	void testSingletonThreadSafety() {
		Mono<K> kMono = rsc3.getService(K.class);
		Flux<Mono<K>> k10000Times = Flux.fromIterable(Collections.nCopies(10000, kMono));
		assertEquals(1, Flux.merge(k10000Times, 10000, 10000).distinct().count().block());
	}
	
	@Test
	void testSetterInjection() {
		System.out.println("testSetterInjection");
		A a = rsc.getService(A.class).block();
		assertNotNull(a.b.a);
		H h = rsc2.getService(H.class).block();
		assertNotNull(h.a);
		assertNotNull(h.b);
		assertNotNull(h.c);
		assertNotNull(h.d);
	}
	
	@Test
	void testCircularDependency() {
		System.out.println("testCircularDependency");
		assertThrows(IllegalStateException.class,
				() -> RdiServiceContainer.annotationBased(E.class),
				() -> "Depends on itself");
		assertThrows(IllegalStateException.class,
				() -> RdiServiceContainer.annotationBased(F.class),
				() -> "Mutual Dependency");
	}
	
	@Test
	void testCircularInstantiation() {
		System.out.println("testCircularInstantiation");
		assertThrows(IllegalStateException.class,
				() -> RdiServiceContainer.annotationBased(I.class).getService(I.class).block(),
				() -> "I instantiates J and J instantiates I endlessly");
	}
	
	public static class A {
		
		private final B b;

		@RdiInject
		public A(B b) {
			this.b = b;
		}
		
	}

	public static class B {
		
		private final C c;
		private final D d;
		
		private A a;
		
		@RdiInject
		public B(C c, D d) {
			this.c = c;
			this.d = d;
		}
		
		@RdiInject
		public void setA(A a) {
			this.a = a;
		}
	}

	public static class C {
		
		private final D d;
		
		private C(D d) {
			this.d = d;
		}
		
		@RdiInject
		public static Mono<C> create(D d) {
			return Mono.fromCallable(() -> new C(d));
		}
	}

	@RdiSingleton(false)
	public static class D {
	}

	public static class E {

		@RdiInject
		public E(E e) {
		}
	}

	public static class F {

		@RdiInject
		public F(G g) {
		}
	}

	public static class G {

		@RdiInject
		public G(F f) {
		}
	}
	
	public static class H {
		
		private A a;
		private B b;
		private C c;
		private D d;
		
		@RdiInject
		public void setA(A a) {
			this.a = a;
		}
		
		@RdiInject
		public void setB(B b) {
			this.b = b;
		}
		
		@RdiInject
		public void setC(C c) {
			this.c = c;
		}

		@RdiInject
		public void setD(D d) {
			this.d = d;
		}
	}
	

	@RdiSingleton(false)
	public static class I {

		@RdiInject
		public void setJ(J j) {
		}
	}

	@RdiSingleton(false)
	public static class J {

		@RdiInject
		public void setI(I i) {
		}
	}
	
	public static class K {
		
		public Mono<K> create() {
			return Mono.delay(Duration.ofSeconds(1)).then(Mono.fromCallable(K::new));
		}
	}

}


