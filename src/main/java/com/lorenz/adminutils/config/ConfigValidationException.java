package com.lorenz.adminutils.config;

/**
 * Raised when a configuration file exists but cannot be trusted. Callers must fall
 * back to safe defaults instead of propagating the failure.
 */
public class ConfigValidationException extends Exception {
	public ConfigValidationException(String message) {
		super(message);
	}

	public ConfigValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
