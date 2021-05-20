package com.github.alex1304.rdi.finder;

import com.github.alex1304.rdi.config.ServiceDescriptor;

import java.util.Set;

/**
 * Abstraction for service discovery. Implementors are able to find and declare services from any declarative source,
 * whether it's XML, annotations, a database...
 * <p>
 * RDI provides an annotation-based {@link ServiceFinder} by default.
 */
public interface ServiceFinder {

    /**
     * Finds the declared services from some source.
     *
     * @return a {@link Set} of {@link ServiceDescriptor} corresponding to the services found.
     */
    Set<ServiceDescriptor> findServices();
}
