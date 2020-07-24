package com.github.alex1304.rservice.internal;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

class CircularInstantiationDetector {
	
	private final Chain chain;
	private final HashSet<Class<?>> knownClasses;
	private final boolean knownClassDetected;
	
	CircularInstantiationDetector() {
		this.chain = null;
		this.knownClasses = new HashSet<>();
		this.knownClassDetected = false;
	}
	
	private CircularInstantiationDetector(Chain chain, HashSet<Class<?>> knownClasses, boolean knownClassDetected) {
		this.chain = chain;
		this.knownClasses = knownClasses;
		this.knownClassDetected = knownClassDetected;
	}

	Optional<CircularInstantiationDetector> add(Class<?> clazz) {
		Chain chain = this.chain;
		HashSet<Class<?>> knownClasses = new HashSet<>(this.knownClasses);
		boolean knownClassDetected = this.knownClassDetected;
		
		chain = new Chain(clazz, chain);
		if (knownClasses.add(clazz)) {
			if (knownClassDetected) {
				knownClasses = new HashSet<>();
				knownClasses.add(clazz);
				knownClassDetected = false;
			}
			return Optional.of(new CircularInstantiationDetector(chain, knownClasses, knownClassDetected));
		}
		knownClassDetected = true;
		SearchResult searchResult = chain.searchForSamePreviousElement();
		int d = 1;
		Chain c1 = chain.previous;
		Chain c2 = searchResult.ref.previous;
		while (d < searchResult.distance) {
			if (!c1.equals(c2)) {
				return Optional.of(new CircularInstantiationDetector(chain, knownClasses, knownClassDetected));
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
		private final Class<?> node;
		private final Chain previous;
		private Chain(Class<?> element, Chain previous) {
			this.node = element;
			this.previous = previous;
		}
		SearchResult searchForSamePreviousElement() {
			int distance = 0;
			for (Chain c = previous ; c != null ; c = c.previous) {
				distance++;
				if (c.node.equals(node)) {
					return new SearchResult(c, distance);
				}
			}
			throw new AssertionError();
		}
		@Override
		public int hashCode() {
			return Objects.hash(node);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Chain))
				return false;
			Chain other = (Chain) obj;
			return Objects.equals(node, other.node);
		}
		@Override
		public String toString() {
			ArrayDeque<String> d = new ArrayDeque<>();
			for (Chain c = this ; c != null ; c = c.previous) {
				d.addFirst(c.node.getSimpleName());
			}
			return "[" + String.join(", ", d) + "]";
		}
	}
	
	private static class SearchResult {
		private final Chain ref;
		private final int distance;
		private SearchResult(Chain ref, int distance) {
			this.ref = ref;
			this.distance = distance;
		}
	}
}
