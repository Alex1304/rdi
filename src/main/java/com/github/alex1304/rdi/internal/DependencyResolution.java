package com.github.alex1304.rdi.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class DependencyResolution<R, U> {
	
	private final List<Supplier<R>> resolved;
	private final List<U> unresolved;
	
	DependencyResolution(List<Supplier<R>> resolved, List<U> unresolved) {
		this.resolved = resolved;
		this.unresolved = unresolved;
	}
	
	boolean hasNoDeps() {
		return resolved.isEmpty();
	}
	
	boolean isFullyResolved() {
		return unresolved.isEmpty();
	}
	
	List<R> getResolved() {
		return Collections.unmodifiableList(resolved.stream()
				.map(Supplier::get)
				.collect(Collectors.toList()));
	}

	List<U> getUnresolved() {
		return Collections.unmodifiableList(unresolved);
	}

	static <R, U> DependencyResolution<R, U> resolve(List<U> deps, Map<Class<?>, R> cache,
			Function<U, Class<?>> classGetter) {
		ArrayList<Supplier<R>> resolved = new ArrayList<>();
		ArrayList<U> unresolved = new ArrayList<>();
		for (U u : deps) {
			Class<?> clazz = classGetter.apply(u);
			if (cache.containsKey(clazz)) {
				resolved.add(() -> cache.get(clazz));
			} else {
				unresolved.add(u);
			}
		}
		return new DependencyResolution<>(resolved, unresolved);
	}
}