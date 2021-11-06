package com.github.alex1304.rdi;

public class ServiceInstantiationException extends RdiException {

    private final ServiceReference<?> ref;

    public ServiceInstantiationException(ServiceReference<?> ref, Throwable cause) {
        super("An error occurred when instantiating service `" + ref + '`' +
                (cause instanceof ServiceInstantiationException ? ", because it depends on service " +
                        ((ServiceInstantiationException) cause).ref + " which also failed to instantiate" : ""),
                cause);
        this.ref = ref;
    }
}
