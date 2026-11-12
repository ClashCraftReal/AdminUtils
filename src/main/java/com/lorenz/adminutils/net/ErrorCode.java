package com.lorenz.adminutils.net;

import java.util.Optional;

/**
 * Stable, machine-readable failure reasons. The client maps these to localised
 * messages; the wire value never carries free-form server detail.
 */
public enum ErrorCode {
	OK("ok"),
	PROTOCOL_MISMATCH("protocol_mismatch"),
	NOT_READY("not_ready"),
	RATE_LIMITED("rate_limited"),
	INVALID_REQUEST("invalid_request"),
	PERMISSION_DENIED("permission_denied"),
	UNSUPPORTED_ACTION("unsupported_action"),
	INTERNAL_ERROR("internal_error");

	private final String id;

	ErrorCode(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static Optional<ErrorCode> byId(String id) {
		for (ErrorCode code : values()) {
			if (code.id.equals(id)) {
				return Optional.of(code);
			}
		}
		return Optional.empty();
	}
}
