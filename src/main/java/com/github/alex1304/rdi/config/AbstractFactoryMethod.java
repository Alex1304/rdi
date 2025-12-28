package com.github.alex1304.rdi.config;

import com.github.alex1304.rdi.RdiException;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.Map.Entry;

abstract class AbstractFactoryMethod implements FactoryMethod {

    private final Class<?> owner;
    private final @Nullable String methodName;
    private final @Nullable Class<?> returnType;
    private final List<Injectable> params;
    private final MethodHandle methodHandle;

    AbstractFactoryMethod(Class<?> owner, @Nullable String methodName, @Nullable Class<?> returnType, List<Injectable> params) {
        this.owner = owner;
        this.methodName = methodName;
        this.returnType = returnType;
        this.params = params;
        this.methodHandle = prepareMethodHandle();
    }

    private static MethodHandle asGenericSpreader(MethodHandle mh, int argsCount) {
        return mh.asType(mh.type().generic()).asSpreader(Object[].class, argsCount);
    }

    @Override
    public Mono<Object> invoke(Object... args) {
        return Mono.defer(() -> {
            try {
                if (Publisher.class.isAssignableFrom(methodHandle.type().returnType())) {
                    //noinspection unchecked
                    return Mono.from((Publisher<Object>) asGenericSpreader(methodHandle, args.length).invoke(args))
                            .switchIfEmpty(Mono.error(() -> new RdiException("Reactive factory " +
                                    userFriendlyRepresentation(owner, methodName) + " completed empty")));
                }
                Object o = asGenericSpreader(methodHandle, args.length).invoke(args);
                return Mono.just(o);
            } catch (Throwable t) {
                return Mono.error(t);
            }
        });
    }

    @Override
    public List<Injectable> getInjectableParameters() {
        return Collections.unmodifiableList(params);
    }

    abstract MethodHandle findMethodHandle(Class<?> owner, @Nullable String methodName, @Nullable Class<?> returnType,
                                           List<Class<?>> paramTypes) throws ReflectiveOperationException;

    private MethodHandle prepareMethodHandle() {
        List<Class<?>> paramTypes = new ArrayList<>();
        Map<Integer, Object> values = new TreeMap<>(Collections.reverseOrder());
        int i = 0;
        for (Injectable inj : params) {
            paramTypes.add(inj.getType());
            final int captureOfI = i;
            inj.getValue().ifPresent(v -> values.put(captureOfI, v));
            i++;
        }
        try {
            MethodHandle mh = findMethodHandle(owner, methodName, returnType, paramTypes);
            for (Entry<Integer, Object> entry : values.entrySet()) {
                mh = MethodHandles.insertArguments(mh, entry.getKey(), entry.getValue());
            }
            return mh;
        } catch (ReflectiveOperationException e) {
            throw new RdiException("Error when acquiring factory method handle for class " + owner.getName(), e);
        }
    }

    abstract String userFriendlyRepresentation(Class<?> owner, @Nullable String methodName);

    @Override
    public String toString() {
        return "FactoryMethod{owner=" + owner + ", methodName=" + methodName + ", returnType=" + returnType
                + ", params=" + params + ", methodHandle=" + methodHandle + "}";
    }
}
