package com.github.alex1304.rdi.config;

import com.github.alex1304.rdi.ServiceReference;

import java.util.Optional;

class Ref implements Injectable {

    private final ServiceReference<?> ref;
    private final Class<?> type;

    Ref(ServiceReference<?> ref, Class<?> type) {
        this.ref = ref;
        this.type = type;
    }

    @Override
    public Optional<ServiceReference<?>> getReference() {
        return Optional.of(ref);
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Ref{ref=" + ref + "}";
    }
}
