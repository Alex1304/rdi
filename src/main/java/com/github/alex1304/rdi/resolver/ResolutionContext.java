package com.github.alex1304.rdi.resolver;

import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import reactor.core.publisher.Mono;

class ResolutionContext {

    private final ServiceDescriptor descriptor;
    private Mono<Object> mono;
    private Object singleton;
    private Throwable instantiationError;
    private ResolutionStep step = ResolutionStep.RESOLVING_FACTORY;

    public ResolutionContext(ServiceDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    ServiceReference<?> getReference() {
        return descriptor.getServiceReference();
    }

    ServiceDescriptor getDescriptor() {
        return descriptor;
    }

    Mono<Object> getMono() {
        return mono;
    }

    void setMono(Mono<Object> mono) {
        this.mono = mono;
    }

    Object getSingleton() {
        return singleton;
    }

    void setSingleton(Object singleton) {
        this.singleton = singleton;
    }

    Throwable getInstantiationError() {
        return instantiationError;
    }

    void setInstantiationError(Throwable instantiationError) {
        this.instantiationError = instantiationError;
    }

    ResolutionStep getStep() {
        return step;
    }

    void setStep(ResolutionStep step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "ResolutionContext{" +
                "descriptor=" + descriptor +
                ", mono=" + mono +
                ", singleton=" + singleton +
                ", instantiationError=" + instantiationError +
                ", step=" + step +
                '}';
    }
}