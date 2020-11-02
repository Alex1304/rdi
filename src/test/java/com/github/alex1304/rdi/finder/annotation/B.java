package com.github.alex1304.rdi.finder.annotation;

@RdiService
public class B {

	private C c;

	public final C getC() {
		return c;
	}

	@RdiSetter
	public final void setC(@RdiRef("crack") C c) {
		this.c = c;
	}
}
