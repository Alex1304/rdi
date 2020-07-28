package com.github.alex1304.rdi;

/**
 * Thrown when something goes wrong while configuring or using a RDI service
 * container.
 */
public class RdiException extends RuntimeException {
	private static final long serialVersionUID = -462339584877052481L;

	public RdiException(String message) {
		super(message);
	}

	public RdiException(String message, Throwable cause) {
		super(message, cause);
	}
}
