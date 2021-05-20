package com.github.alex1304.rdi.resolver;

import com.github.alex1304.rdi.ServiceReference;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;

class CycleDetector {

    private final Chain chain;
    private final HashSet<ServiceReference<?>> knownRefs;
    private final boolean knownRefDetected;
    private final boolean hasCycle;

    CycleDetector() {
        this.chain = null;
        this.knownRefs = new HashSet<>();
        this.knownRefDetected = false;
        this.hasCycle = false;
    }

    private CycleDetector(Chain chain, HashSet<ServiceReference<?>> knownRefs, boolean knownRefDetected,
                          boolean hasCycle) {
        this.chain = chain;
        this.knownRefs = knownRefs;
        this.knownRefDetected = knownRefDetected;
        this.hasCycle = hasCycle;
    }

    CycleDetector next(ServiceReference<?> ref) {
        if (hasCycle) {
            throw new IllegalStateException("Cycle already detected");
        }
        Chain chain = this.chain;
        HashSet<ServiceReference<?>> knownRefs = new HashSet<>(this.knownRefs);
        boolean knownRefDetected = this.knownRefDetected;

        chain = new Chain(ref, chain);
        if (knownRefs.add(ref)) {
            if (knownRefDetected) {
                knownRefs = new HashSet<>();
                knownRefs.add(ref);
                knownRefDetected = false;
            }
            return new CycleDetector(chain, knownRefs, knownRefDetected, false);
        }
        knownRefDetected = true;
        SearchResult searchResult = chain.searchForSamePreviousRef();
        int d = 1;
        Chain c1 = chain.previous;
        Chain c2 = searchResult.chain.previous;
        while (d < searchResult.distance) {
            if (!c1.equals(c2)) {
                return new CycleDetector(chain, knownRefs, knownRefDetected, false);
            }
            c1 = c1.previous;
            c2 = c2.previous;
            d++;
        }
        return new CycleDetector(chain, knownRefs, knownRefDetected, true);
    }

    boolean hasCycle() {
        return hasCycle;
    }

    @Override
    public String toString() {
        return String.valueOf(chain);
    }

    private static class Chain {

        private final ServiceReference<?> ref;
        private final Chain previous;

        private Chain(ServiceReference<?> ref, Chain previous) {
            this.ref = ref;
            this.previous = previous;
        }

        SearchResult searchForSamePreviousRef() {
            int distance = 0;
            for (Chain c = previous; c != null; c = c.previous) {
                distance++;
                if (c.ref.equals(ref)) {
                    return new SearchResult(c, distance);
                }
            }
            throw new AssertionError();
        }

        @Override
        public int hashCode() {
            return Objects.hash(ref);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Chain))
                return false;
            Chain other = (Chain) obj;
            return Objects.equals(ref, other.ref);
        }

        @Override
        public String toString() {
            ArrayDeque<String> d = new ArrayDeque<>();
            for (Chain c = this; c != null; c = c.previous) {
                d.addFirst(c.ref.toString());
            }
            return "[" + String.join(" => ", d) + "]";
        }
    }

    private static class SearchResult {

        private final Chain chain;
        private final int distance;

        private SearchResult(Chain chain, int distance) {
            this.chain = chain;
            this.distance = distance;
        }
    }
}
