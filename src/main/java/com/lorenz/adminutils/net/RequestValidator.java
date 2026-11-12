package com.lorenz.adminutils.net;

import java.util.Optional;
import java.util.regex.Pattern;

import com.lorenz.adminutils.net.payload.ActionRequestC2S;
import com.lorenz.adminutils.net.payload.HelloC2S;

/**
 * Structural validation of inbound payloads. This runs before any permission check
 * so malformed or oversized input is rejected without touching server state.
 */
public final class RequestValidator {
	private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_.:-]+");

	private RequestValidator() {
	}

	public static Optional<ErrorCode> validate(HelloC2S payload) {
		if (payload == null) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (payload.clientVersion() == null || payload.clientVersion().length() > Protocol.MAX_VERSION_STRING_LENGTH) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		return Optional.empty();
	}

	public static Optional<ErrorCode> validate(ActionRequestC2S payload) {
		if (payload == null) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (!isValidId(payload.requestId(), Protocol.MAX_REQUEST_ID_LENGTH)) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (!isValidId(payload.actionId(), Protocol.MAX_ACTION_ID_LENGTH)) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (payload.argument() == null || payload.argument().length() > Protocol.MAX_ARGUMENT_LENGTH) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		return Optional.empty();
	}

	private static boolean isValidId(String value, int maxLength) {
		return value != null
			&& !value.isEmpty()
			&& value.length() <= maxLength
			&& ID_PATTERN.matcher(value).matches();
	}
}
