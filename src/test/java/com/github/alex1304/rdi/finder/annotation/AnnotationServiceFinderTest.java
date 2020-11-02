package com.github.alex1304.rdi.finder.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import com.github.alex1304.rdi.RdiServiceContainer;
import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.RdiConfig;
import com.github.alex1304.rdi.finder.ServiceFinder;

class AnnotationServiceFinderTest {

	@Test
	void testBuildConfig() {
		ServiceFinder finder = AnnotationServiceFinder.create(
				new Reflections("com.github.alex1304.rdi.finder.*")
						.getTypesAnnotatedWith(RdiService.class));
		RdiConfig config = RdiConfig.fromServiceFinder(finder);
		RdiServiceContainer container = RdiServiceContainer.create(config);
		A a = container.getService(ServiceReference.ofType(A.class)).block();
		assertEquals(a.getFoo(), "hello");
		assertEquals(a.getBar(), 42);
		assertNotNull(a.getB());
		assertNotNull(a.getB().getC());
	}

}
