package com.github.alex1304.rdi.config;

import com.github.alex1304.rdi.ServiceReference;

import java.util.Optional;

/**
 * Represents an injectable parameter. You may inject known values as well as references to other services.
 *
 * <p>
 * Although not required, the static methods in this interface are designed for use with an <code>import static</code>
 * statement for better code readability.
 */
public interface Injectable {

    /**
     * Creates an injectable parameter which represents a reference to another service.
     *
     * <p>
     * The type of the reference will be used as the type of the parameter to find the correct injection method. If the
     * service referred to by that reference does not match the type of the method parameter (for example the reference
     * is an implementation class while the service constructor or setter expects the superinterface), you will need to
     * use {@link #ref(ServiceReference, Class)} instead.
     *
     * @param ref the reference
     * @return an injectable parameter
     */
    static Injectable ref(ServiceReference<?> ref) {
        return new Ref(ref, ref.getServiceClass());
    }

    /**
     * Creates an injectable parameter which represents a reference to another service.
     *
     * <p>
     * This overload is suited if you have a configuration where the reference type is a subtype of the parameter type
     * expected by the injection method.
     *
     * @param ref         the reference
     * @param asSupertype the supertype of the reference that matches will the target injection method's parameter type
     * @param <T>         the generic type of the reference, captured to ensure that the second argument is actually a
     *                    supertype of it
     * @return an injectable parameter
     */
    static <T> Injectable ref(ServiceReference<T> ref, Class<? super T> asSupertype) {
        return new Ref(ref, asSupertype);
    }

    /**
     * Creates an injectable parameter which represents an already known value.
     *
     * @param value the value to inject
     * @return an injectable parameter
     */
    static Injectable value(int value) {
        return new Value(value, int.class);
    }

    /**
     * Creates an injectable parameter which represents an already known value.
     *
     * @param value the value to inject
     * @return an injectable parameter
     */
    static Injectable value(long value) {
        return new Value(value, long.class);
    }

    /**
     * Creates an injectable parameter which represents an already known value.
     *
     * @param value the value to inject
     * @return an injectable parameter
     */
    static Injectable value(double value) {
        return new Value(value, double.class);
    }

    /**
     * Creates an injectable parameter which represents an already known value.
     *
     * @param value the value to inject
     * @return an injectable parameter
     */
    static Injectable value(char value) {
        return new Value(value, char.class);
    }

    /**
     * Creates an injectable parameter which represents an already known value.
     *
     * @param value the value to inject
     * @return an injectable parameter
     */
    static Injectable value(byte value) {
        return new Value(value, byte.class);
    }

    /**
     * Creates an injectable parameter which represents an already known value.
     *
     * @param value the value to inject
     * @return an injectable parameter
     */
    static Injectable value(short value) {
        return new Value(value, short.class);
    }

    /**
     * Creates an injectable parameter which represents an already known value.
     *
     * @param value the value to inject
     * @return an injectable parameter
     */
    static Injectable value(float value) {
        return new Value(value, float.class);
    }

    /**
     * Creates an injectable parameter which represents an already known value.
     *
     * @param value the value to inject
     * @return an injectable parameter
     */
    static Injectable value(boolean value) {
        return new Value(value, boolean.class);
    }

    /**
     * Creates an injectable parameter which represents an already known value.
     *
     * @param value the value to inject
     * @param type  the type of the value. It may be a super type of the actual runtime type of the value, in a such way
     *              that it matches with the injection method's parameter type
     * @param <T>   the generic type of the object, captured to ensure that the second argument is actually a supertype
     *              of it
     * @return an injectable parameter
     */
    static <T> Injectable value(T value, Class<? super T> type) {
        return new Value(value, type);
    }

    /**
     * Gets the type of this injectable parameter.
     *
     * @return the type
     */
    Class<?> getType();

    /**
     * Gets the value of this injectable parameter, if it represents a provided value.
     *
     * @return the value, if applicable
     */
    default Optional<Object> getValue() {
        return Optional.empty();
    }

    /**
     * Gets the reference of this injectable parameter, if it represents a reference to another service.
     *
     * @return the reference, if applicable
     */
    default Optional<ServiceReference<?>> getReference() {
        return Optional.empty();
    }
}
