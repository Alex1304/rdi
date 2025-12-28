package com.github.alex1304.rdi.resolver;

import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.Objects;

class ResolutionContext {

    private final ServiceDescriptor descriptor;
    private @Nullable Mono<Object> mono;
    private @Nullable Object singleton;
    private @Nullable Throwable instantiationError;
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

    boolean hasMono() {
        return mono != null;
    }

    Mono<Object> getMono() {
        return Objects.requireNonNull(mono);
    }

    void setMono(Mono<Object> mono) {
        this.mono = mono;
    }

    @Nullable Object getSingleton() {
        return singleton;
    }

    void setSingleton(Object singleton) {
        this.singleton = singleton;
    }

    @Nullable Throwable getInstantiationError() {
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