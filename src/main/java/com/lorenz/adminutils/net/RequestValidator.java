package com.lorenz.adminutils.net;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.lorenz.adminutils.net.payload.ActionRequestC2S;
import com.lorenz.adminutils.net.payload.HelloC2S;
import com.lorenz.adminutils.net.payload.PlayerEnderChestRequestC2S;
import com.lorenz.adminutils.net.payload.PlayerEnderChestS2C;
import com.lorenz.adminutils.net.payload.PlayerInventoryChunkS2C;
import com.lorenz.adminutils.net.payload.PlayerInventoryRequestC2S;
import com.lorenz.adminutils.net.payload.PlayerInventoryS2C;
import com.lorenz.adminutils.players.InventorySnapshotLimits;
import com.lorenz.adminutils.players.PlayerContainerKind;

import net.minecraft.world.item.ItemStack;

/**
 * Structural validation of inbound payloads. This runs before any permission check
 * so malformed or oversized input is rejected without touching server state.
 *
 * <p>Target existence is deliberately not checked here: resolving a UUID to an online
 * player requires server state and belongs to the request handler.
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

	public static Optional<ErrorCode> validate(PlayerInventoryRequestC2S payload) {
		if (payload == null || payload.target() == null) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (!isValidId(payload.requestId(), Protocol.MAX_REQUEST_ID_LENGTH)) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		return Optional.empty();
	}

	public static Optional<ErrorCode> validate(PlayerEnderChestRequestC2S payload) {
		if (payload == null || payload.target() == null) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (!isValidId(payload.requestId(), Protocol.MAX_REQUEST_ID_LENGTH)) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		return Optional.empty();
	}

	public static Optional<ErrorCode> validate(PlayerInventoryS2C payload) {
		if (payload == null || payload.target() == null) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (!isValidId(payload.requestId(), Protocol.MAX_REQUEST_ID_LENGTH)) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (payload.revision() < 0) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (!hasExpectedSlots(payload.slots(), payload.slotCount(), PlayerContainerKind.INVENTORY)) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		return Optional.empty();
	}

	public static Optional<ErrorCode> validate(PlayerEnderChestS2C payload) {
		if (payload == null || payload.target() == null) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (!isValidId(payload.requestId(), Protocol.MAX_REQUEST_ID_LENGTH)) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (payload.revision() < 0) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (!hasExpectedSlots(payload.slots(), payload.slotCount(), PlayerContainerKind.ENDER_CHEST)) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		return Optional.empty();
	}

	public static Optional<ErrorCode> validate(PlayerInventoryChunkS2C payload) {
		if (payload == null || payload.target() == null || payload.slots() == null) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (!isValidId(payload.requestId(), Protocol.MAX_REQUEST_ID_LENGTH)) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (payload.revision() < 0) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (payload.slotCount() != PlayerContainerKind.INVENTORY.slotCount()) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (payload.chunkCount() < 1 || payload.chunkCount() > InventorySnapshotLimits.MAX_CHUNKS) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (payload.chunkIndex() < 0 || payload.chunkIndex() >= payload.chunkCount()) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (payload.firstSlot() < 0 || payload.slots().isEmpty()) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		if (payload.endSlot() > payload.slotCount()) {
			return Optional.of(ErrorCode.INVALID_REQUEST);
		}
		for (ItemStack stack : payload.slots()) {
			if (stack == null) {
				return Optional.of(ErrorCode.INVALID_REQUEST);
			}
		}
		return Optional.empty();
	}

	private static boolean hasExpectedSlots(List<ItemStack> slots, int declaredSlotCount, PlayerContainerKind kind) {
		if (slots == null || declaredSlotCount != kind.slotCount() || slots.size() != kind.slotCount()) {
			return false;
		}
		for (ItemStack stack : slots) {
			if (stack == null) {
				return false;
			}
		}
		return true;
	}

	private static boolean isValidId(String value, int maxLength) {
		return value != null
			&& !value.isEmpty()
			&& value.length() <= maxLength
			&& ID_PATTERN.matcher(value).matches();
	}
}
