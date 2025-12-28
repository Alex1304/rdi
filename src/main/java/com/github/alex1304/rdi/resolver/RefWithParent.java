package com.github.alex1304.rdi.resolver;

import com.github.alex1304.rdi.ServiceReference;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

class RefWithParent {

    private final @Nullable ServiceReference<?> parent;
    private final @Nullable ServiceReference<?> element;

    RefWithParent(@Nullable ServiceReference<?> parent, @Nullable ServiceReference<?> element) {
        this.parent = parent;
        this.element = element;
    }

    @Nullable ServiceReference<?> getParent() {
        return parent;
    }

    @Nullable ServiceReference<?> getElement() {
        return element;
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, parent);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof RefWithParent))
            return false;
        RefWithParent other = (RefWithParent) obj;
        return element == other.element && parent == other.parent;
    }

    @Override
    public String toString() {
        return element + "(" + parent + ")";
    }
}