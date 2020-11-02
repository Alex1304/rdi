package com.github.alex1304.rdi.finder.annotation;

import reactor.core.publisher.Mono;

@RdiService("crack")
public class C {

	@RdiFactory
	public static Mono<C> create() {
		return Mono.fromCallable(C::new);
	}
}
