package com.github.alex1304.rdi;

import com.github.alex1304.rdi.config.RdiConfig;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import static com.github.alex1304.rdi.config.FactoryMethod.constructor;
import static com.github.alex1304.rdi.config.FactoryMethod.staticFactory;
import static com.github.alex1304.rdi.config.Injectable.ref;
import static com.github.alex1304.rdi.config.Injectable.value;
import static org.junit.jupiter.api.Assertions.*;

class RdiServiceContainerTest {

    private static final ServiceReference<A> A = ServiceReference.of("A", A.class);
    private static final ServiceReference<B> B = ServiceReference.of("B", B.class);
    private static final ServiceReference<C> C = ServiceReference.of("C", C.class);
    private static final ServiceReference<D> D = ServiceReference.of("D", D.class);
    private static RdiConfig conf1, conf2, conf3, conf4, conf5, conf6,
            conf7, conf8, conf9, conf10, conf11, conf12, conf13;
    private static int D_INSTANCE_COUNT = 0;

    @BeforeAll
    static void setUpBeforeClass() {
        Hooks.onOperatorDebug();
        conf1 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A).build())
                .build();
        conf2 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A)
                        .setFactoryMethod(constructor(ref(A)))
                        .build())
                .build();
        conf3 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A)
                        .setFactoryMethod(constructor(ref(B)))
                        .build())
                .build();
        conf4 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A)
                        .setFactoryMethod(constructor(ref(B)))
                        .build())
                .registerService(ServiceDescriptor.standalone(B))
                .build();
        conf5 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A)
                        .setFactoryMethod(constructor(ref(B)))
                        .build())
                .registerService(ServiceDescriptor.builder(B)
                        .addSetterMethod("setA", ref(A))
                        .build())
                .build();
        conf6 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A)
                        .addSetterMethod("setB", ref(B))
                        .build())
                .registerService(ServiceDescriptor.builder(B)
                        .addSetterMethod("setA", ref(A))
                        .build())
                .build();
        conf7 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A)
                        .setSingleton(false)
                        .addSetterMethod("setB", ref(B))
                        .build())
                .registerService(ServiceDescriptor.builder(B)
                        .addSetterMethod("setA", ref(A))
                        .build())
                .build();
        conf8 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A)
                        .setSingleton(false)
                        .addSetterMethod("setB", ref(B))
                        .build())
                .registerService(ServiceDescriptor.builder(B)
                        .setSingleton(false)
                        .addSetterMethod("setA", ref(A))
                        .build())
                .build();
        conf9 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A)
                        .setFactoryMethod(constructor(value(1304)))
                        .build())
                .build();
        conf10 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A)
                        .addSetterMethod("setValue", value(1304))
                        .build())
                .build();
        conf11 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(A)
                        .setFactoryMethod(constructor(ref(B), value(1304)))
                        .build())
                .registerService(ServiceDescriptor.standalone(B))
                .build();
        conf12 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(B)
                        .setFactoryMethod(staticFactory("create", Mono.class,
                                value("test", String.class),
                                value(1304)))
                        .build())
                .build();
        conf13 = RdiConfig.builder()
                .registerService(ServiceDescriptor.builder(C)
                        .setFactoryMethod(constructor(ref(D)))
                        .build())
                .registerService(ServiceDescriptor.standalone(D))
                .build();
    }

    private static void logExpectedException(Logger logger, Throwable t) {
        logger.info("Exception thrown as expected with message: {}{}", t.getMessage(),
                t.getCause() != null ? ", caused by " + t.getCause() : "");
    }

    @Test
    void testA_NoDeps() {
        assertDoesNotThrow(() -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf1);
            A a = cont.getService(A).block();
            assertNotNull(a);
        });
    }

    @Test
    void testA_ErrorDependsOnItself() {
        RdiException e = assertThrows(RdiException.class, () -> RdiServiceContainer.create(conf2));
        logExpectedException(Loggers.getLogger("testA_ErrorDependsOnItself"), e);
    }

    @Test
    void testA_ErrorMissingRefB() {
        RdiException e = assertThrows(RdiException.class, () -> RdiServiceContainer.create(conf3));
        logExpectedException(Loggers.getLogger("testA_ErrorMissingRefB"), e);
    }

    @Test
    void testAInjectsBViaFactory() {
        assertDoesNotThrow(() -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf4);
            A a = cont.getService(A).block();
            assertNotNull(a);
            assertNotNull(a.b);
        });
    }

    @Test
    void testAInjectsBViaFactoryAndBInjectsAViaSetter() {
        assertDoesNotThrow(() -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf5);
            A a = cont.getService(A).block();
            assertNotNull(a);
            assertNotNull(a.b);
            assertNotNull(a.b.a);
            assertSame(a, a.b.a); // A is singleton so should be the same instance
        });
    }

    @Test
    void testAInjectsBViaSetterAndBInjectsAViaSetter() {
        assertDoesNotThrow(() -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf6);
            A a = cont.getService(A).block();
            assertNotNull(a);
            assertNotNull(a.b);
            assertNotNull(a.b.a);
            assertSame(a, a.b.a); // A is singleton so should be the same instance
        });
    }

    @Test
    void testAInjectsBViaSetterAndBInjectsAViaSetter_ANotSingleton() {
        assertDoesNotThrow(() -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf7);
            A a = cont.getService(A).block();
            assertNotNull(a);
            assertNotNull(a.b);
            assertNotNull(a.b.a);
            assertNotSame(a, a.b.a); // A is NOT singleton so should be DIFFERENT instances
            assertSame(a.b, a.b.a.b); // B however is a singleton so both A instances should share the same B
        });
    }

    @Test
    void testAInjectsBViaSetterAndBInjectsAViaSetter_NoSingleton_ErrorCircularInstantiation() {
        RdiServiceContainer cont = assertDoesNotThrow(() -> RdiServiceContainer.create(conf8));
        RdiException e = assertThrows(RdiException.class, () -> cont.getService(A).block());
        logExpectedException(Loggers.getLogger(
                "testAInjectsBViaSetterAndBInjectsAViaSetter_NoSingleton_ErrorCircularInstantiation"), e);
    }

    @Test
    void testAInjectsValueViaFactory() {
        assertDoesNotThrow(() -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf9);
            A a = cont.getService(A).block();
            assertNotNull(a);
            assertEquals(1304, a.value);
        });
    }

    @Test
    void testAInjectsValueViaSetter() {
        assertDoesNotThrow(() -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf10);
            A a = cont.getService(A).block();
            assertNotNull(a);
            assertEquals(1304, a.value);
        });
    }

    @Test
    void testAInjectsValueAndBViaSetter() {
        assertDoesNotThrow(() -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf11);
            A a = cont.getService(A).block();
            assertNotNull(a);
            assertEquals(1304, a.value);
            assertNotNull(a.b);
        });
    }

    @Test
    void testBInjectsAViaStaticFactory() {
        assertDoesNotThrow(() -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf12);
            B b = cont.getService(B).block();
            assertNotNull(b);
        });
    }

    @Test
    void testCannotConstructD() {
        Throwable t = assertThrows(ServiceInstantiationException.class, () -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf13);
            cont.getService(D).block();
        });
        logExpectedException(Loggers.getLogger("testCannotConstructD"), t);
    }

    @Test
    void testCannotConstructCBecauseDFails() {
        Throwable t = assertThrows(ServiceInstantiationException.class, () -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf13);
            cont.getService(C).block();
        });
        logExpectedException(Loggers.getLogger("testCannotConstructCBecauseDFails"), t);
    }

    @Test
    void testErrorCachesProperly() {
        assertDoesNotThrow(() -> {
            RdiServiceContainer cont = RdiServiceContainer.create(conf13);
            assertSame(D_INSTANCE_COUNT, 0);
            cont.getService(D).onErrorResume(ServiceInstantiationException.class, e -> Mono.empty()).block();
            assertSame(D_INSTANCE_COUNT, 1);
            cont.getService(D).onErrorResume(ServiceInstantiationException.class, e -> Mono.empty()).block();
            assertSame(D_INSTANCE_COUNT, 1);
        });
    }

    public static class A {

        private B b;
        private int value;

        public A() {
        }

        public A(A a) {
        }

        public A(B b) {
            this.b = b;
        }

        public A(int value) {
            this.value = value;
        }

        public A(B b, int value) {
            this.b = b;
            this.value = value;
        }

        public void setB(B b) {
            this.b = b;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class B {
        private A a;

        public B() {
        }

        public B(A a) {
            this.a = a;
        }

        public static Mono<B> create(String value1, int value2) {
            return Mono.fromCallable(B::new);
        }

        public void setA(A a) {
            this.a = a;
        }
    }

    public static class C {

        public C(D d) {

        }
    }

    public static class D {

        public D() {
            D_INSTANCE_COUNT++;
            throw new RuntimeException("Oops! Cannot construct D!");
        }
    }
}


