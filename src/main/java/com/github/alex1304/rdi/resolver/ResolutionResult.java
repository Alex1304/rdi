package com.github.alex1304.rdi.resolver;

import com.github.alex1304.rdi.RdiException;
import com.github.alex1304.rdi.ServiceReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class ResolutionResult {

    private final List<ResolutionContext> resolved;
    private final List<ServiceReference<?>> unresolved;

    ResolutionResult(List<ResolutionContext> resolved, List<ServiceReference<?>> unresolved) {
        this.resolved = resolved;
        this.unresolved = unresolved;
    }

    static ResolutionResult compute(ServiceReference<?> owner, List<ServiceReference<?>> deps,
                                    Map<ServiceReference<?>, ResolutionContext> resolutionContextByRef) {
        ArrayList<ResolutionContext> resolved = new ArrayList<>();
        ArrayList<ServiceReference<?>> unresolved = new ArrayList<>();
        for (ServiceReference<?> dep : deps) {
            ResolutionContext rctx = resolutionContextByRef.get(dep);
            if (rctx == null) {
                throw new RdiException("The service '" + owner.getServiceName()
                        + "' is referring to missing service '" + dep.getServiceName()
                        + "'. Did you forget to register '" + dep.getServiceName() + "' in the config?");
            }
            if (rctx.getMono() != null) {
                resolved.add(rctx);
            } else {
                unresolved.add(dep);
            }
        }
        return new ResolutionResult(resolved, unresolved);
    }

    boolean hasNoDeps() {
        return resolved.isEmpty();
    }

    boolean isFullyResolved() {
        return unresolved.isEmpty();
    }

    List<ResolutionContext> getResolved() {
        return Collections.unmodifiableList(resolved);
    }

    List<ServiceReference<?>> getUnresolved() {
        return Collections.unmodifiableList(unresolved);
    }
}