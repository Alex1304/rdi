package com.github.alex1304.rdi.finder.annotation;

import com.github.alex1304.rdi.RdiServiceContainer;
import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.RdiConfig;
import com.github.alex1304.rdi.finder.ServiceFinder;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnnotationServiceFinderTest {

    @Test
    void testBuildConfig() {
        ServiceFinder finder = AnnotationServiceFinder.create(
                new Reflections(AnnotationServiceFinderTest.class)
                        .getTypesAnnotatedWith(RdiService.class));
        RdiConfig config = RdiConfig.fromServiceFinder(finder);
        RdiServiceContainer container = RdiServiceContainer.create(config);
        A a = container.getService(ServiceReference.ofType(A.class)).block();
        assertNotNull(a);
        assertEquals(a.getFoo(), "hello");
        assertEquals(a.getBar(), 42);
        assertNotNull(a.getB());
        assertNotNull(a.getB().getC());
        Shape shape = container.getService(ServiceReference.ofType(Shape.class)).block();
        assertNotNull(shape);
        assertEquals("circle", shape.value());
    }

}
