package com.lorenz.adminutils.config;

/**
 * Validation hook for {@link JsonConfigFile}. Declared as its own type so a validator
 * may reject a configuration with a checked {@link ConfigValidationException}.
 */
@FunctionalInterface
public interface ConfigValidator<T> {
	void validate(T value) throws ConfigValidationException;
}
