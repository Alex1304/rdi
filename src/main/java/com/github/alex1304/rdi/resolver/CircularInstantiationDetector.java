package com.github.alex1304.rdi.resolver;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import com.github.alex1304.rdi.ServiceReference;

class CircularInstantiationDetector {
	
	private final Chain chain;
	private final HashSet<ServiceReference<?>> knownRefs;
	private final boolean knownRefDetected;
	
	CircularInstantiationDetector() {
		this.chain = null;
		this.knownRefs = new HashSet<>();
		this.knownRefDetected = false;
	}
	
	private CircularInstantiationDetector(Chain chain, HashSet<ServiceReference<?>> knownRefs, boolean knownRefDetected) {
		this.chain = chain;
		this.knownRefs = knownRefs;
		this.knownRefDetected = knownRefDetected;
	}

	Optional<CircularInstantiationDetector> add(ServiceReference<?> clazz) {
		Chain chain = this.chain;
		HashSet<ServiceReference<?>> knownRefs = new HashSet<>(this.knownRefs);
		boolean knownRefDetected = this.knownRefDetected;
		
		chain = new Chain(clazz, chain);
		if (knownRefs.add(clazz)) {
			if (knownRefDetected) {
				knownRefs = new HashSet<>();
				knownRefs.add(clazz);
				knownRefDetected = false;
			}
			return Optional.of(new CircularInstantiationDetector(chain, knownRefs, knownRefDetected));
		}
		knownRefDetected = true;
		SearchResult searchResult = chain.searchForSamePreviousRef();
		int d = 1;
		Chain c1 = chain.previous;
		Chain c2 = searchResult.chain.previous;
		while (d < searchResult.distance) {
			if (!c1.equals(c2)) {
				return Optional.of(new CircularInstantiationDetector(chain, knownRefs, knownRefDetected));
			}
			c1 = c1.previous;
			c2 = c2.previous;
			d++;
		}
		return Optional.empty();
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
			for (Chain c = previous ; c != null ; c = c.previous) {
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
			for (Chain c = this ; c != null ; c = c.previous) {
				d.addFirst(c.ref.toString());
			}
			return "Chain{" + String.join(" => ", d) + "}";
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
