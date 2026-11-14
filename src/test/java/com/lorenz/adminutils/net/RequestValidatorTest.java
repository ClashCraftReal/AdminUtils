package com.lorenz.adminutils.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.lorenz.adminutils.net.payload.ActionRequestC2S;
import com.lorenz.adminutils.net.payload.HelloC2S;
import com.lorenz.adminutils.net.payload.PlayerEnderChestRequestC2S;
import com.lorenz.adminutils.net.payload.PlayerInventoryRequestC2S;

import org.junit.jupiter.api.Test;

class RequestValidatorTest {
	private static final UUID TARGET = UUID.fromString("11111111-2222-3333-4444-555555555555");
	@Test
	void acceptsWellFormedHello() {
		assertTrue(RequestValidator.validate(new HelloC2S(1, "1.0.0+26.3")).isEmpty());
	}

	@Test
	void rejectsNullAndOversizedHello() {
		assertEquals(ErrorCode.INVALID_REQUEST, RequestValidator.validate((HelloC2S) null).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new HelloC2S(1, "x".repeat(Protocol.MAX_VERSION_STRING_LENGTH + 1))).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST, RequestValidator.validate(new HelloC2S(1, null)).orElseThrow());
	}

	@Test
	void acceptsWellFormedAction() {
		assertTrue(RequestValidator.validate(new ActionRequestC2S("req-1", "permissions.refresh", "")).isEmpty());
		assertTrue(RequestValidator.validate(new ActionRequestC2S("a.b:c_d", "disguise.status", "arg")).isEmpty());
	}

	@Test
	void rejectsNullAction() {
		assertEquals(ErrorCode.INVALID_REQUEST, RequestValidator.validate((ActionRequestC2S) null).orElseThrow());
	}

	@Test
	void rejectsEmptyOrOversizedRequestId() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new ActionRequestC2S("", "permissions.refresh", "")).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new ActionRequestC2S("r".repeat(Protocol.MAX_REQUEST_ID_LENGTH + 1),
				"permissions.refresh", "")).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new ActionRequestC2S(null, "permissions.refresh", "")).orElseThrow());
	}

	@Test
	void rejectsIllegalCharactersInIds() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new ActionRequestC2S("req 1", "permissions.refresh", "")).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new ActionRequestC2S("req/1", "permissions.refresh", "")).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new ActionRequestC2S("req1", "permissions refresh", "")).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new ActionRequestC2S("req1", "permissions.refresh", null)).orElseThrow());
	}

	@Test
	void rejectsOversizedArgument() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new ActionRequestC2S("req1", "permissions.refresh",
				"a".repeat(Protocol.MAX_ARGUMENT_LENGTH + 1))).orElseThrow());
	}

	@Test
	void acceptsWellFormedPlayerContainerRequests() {
		assertTrue(RequestValidator.validate(new PlayerInventoryRequestC2S("req-1", TARGET)).isEmpty());
		assertTrue(RequestValidator.validate(new PlayerEnderChestRequestC2S("req-2", TARGET)).isEmpty());
	}

	@Test
	void rejectsNullPlayerContainerRequests() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate((PlayerInventoryRequestC2S) null).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate((PlayerEnderChestRequestC2S) null).orElseThrow());
	}

	@Test
	void rejectsPlayerContainerRequestsWithoutTarget() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryRequestC2S("req-1", null)).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerEnderChestRequestC2S("req-1", null)).orElseThrow());
	}

	@Test
	void rejectsPlayerContainerRequestsWithInvalidRequestId() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryRequestC2S(null, TARGET)).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryRequestC2S("", TARGET)).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryRequestC2S("req 1", TARGET)).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerEnderChestRequestC2S(
				"r".repeat(Protocol.MAX_REQUEST_ID_LENGTH + 1), TARGET)).orElseThrow());
	}

	@Test
	void unknownActionIsNotResolved() {
		assertTrue(ServerAction.byId("permissions.refresh").isPresent());
		assertTrue(ServerAction.byId("disguise.status").isPresent());
		assertTrue(ServerAction.byId("admin.grant").isEmpty());
		assertTrue(ServerAction.byId(null).isEmpty());
	}
}
