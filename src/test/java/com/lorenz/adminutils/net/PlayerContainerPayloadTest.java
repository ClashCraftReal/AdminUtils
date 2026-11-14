package com.lorenz.adminutils.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.lorenz.adminutils.net.payload.PlayerEnderChestS2C;
import com.lorenz.adminutils.net.payload.PlayerInventoryChunkS2C;
import com.lorenz.adminutils.net.payload.PlayerInventoryS2C;
import com.lorenz.adminutils.players.PlayerContainerKind;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Codec and validation coverage for the read-only player container snapshots.
 *
 * <p>ItemStack encoding needs the vanilla registries, so this class bootstraps Minecraft
 * once. The request payloads are covered by {@link RequestValidatorTest}, which stays
 * registry-free.
 */
class PlayerContainerPayloadTest {
	private static final UUID TARGET = UUID.fromString("11111111-2222-3333-4444-555555555555");

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	/**
	 * Builds a stack whose default component map is bound explicitly.
	 *
	 * <p>26.1+ resolves an item's default components from its registry holder, and that
	 * binding normally happens during full server resource loading rather than
	 * {@code Bootstrap.bootStrap()}. Binding an empty map keeps the codec path real while
	 * staying independent of datapack tag data. 1.21.11 reads the component map from the
	 * {@code Item} value directly, so no binding step is needed there.
	 */
	@SuppressWarnings("unchecked")
	private static ItemStack stack(Item item, int count) {
		//? if >=26.1 {
		((Holder.Reference<Item>) item.builtInRegistryHolder()).bindComponents(DataComponentMap.EMPTY);
		//?}
		return new ItemStack(item, count);
	}

	@Test
	void containerKindsExposeVerifiedSlotCounts() {
		assertEquals(43, PlayerContainerKind.INVENTORY.slotCount());
		assertEquals(27, PlayerContainerKind.ENDER_CHEST.slotCount());
	}

	@Test
	void inventorySnapshotRoundTrips() {
		List<ItemStack> slots = emptySlots(PlayerContainerKind.INVENTORY.slotCount());
		slots.set(0, stack(Items.STONE, 3));
		slots.set(9, stack(Items.DIAMOND_SWORD, 1));
		slots.set(36, stack(Items.DIAMOND_BOOTS, 1));
		slots.set(40, stack(Items.SHIELD, 1));

		PlayerInventoryS2C original = new PlayerInventoryS2C("req-1", TARGET, 7, 43, slots);
		RegistryFriendlyByteBuf buffer = buffer();
		PlayerInventoryS2C.STREAM_CODEC.encode(buffer, original);
		PlayerInventoryS2C decoded = PlayerInventoryS2C.STREAM_CODEC.decode(buffer);

		assertEquals("req-1", decoded.requestId());
		assertEquals(TARGET, decoded.target());
		assertEquals(7, decoded.revision());
		assertEquals(43, decoded.slotCount());
		assertEquals(43, decoded.slots().size());
		for (int slot = 0; slot < slots.size(); slot++) {
			assertStackEquals(slots.get(slot), decoded.slots().get(slot), slot);
		}
		assertTrue(RequestValidator.validate(decoded).isEmpty());
	}

	@Test
	void enderChestSnapshotRoundTrips() {
		List<ItemStack> slots = emptySlots(PlayerContainerKind.ENDER_CHEST.slotCount());
		slots.set(0, stack(Items.ENDER_CHEST, 1));
		slots.set(26, stack(Items.GOLD_INGOT, 16));

		PlayerEnderChestS2C original = new PlayerEnderChestS2C("req-2", TARGET, 0, 27, slots);
		RegistryFriendlyByteBuf buffer = buffer();
		PlayerEnderChestS2C.STREAM_CODEC.encode(buffer, original);
		PlayerEnderChestS2C decoded = PlayerEnderChestS2C.STREAM_CODEC.decode(buffer);

		assertEquals("req-2", decoded.requestId());
		assertEquals(TARGET, decoded.target());
		assertEquals(0, decoded.revision());
		assertEquals(27, decoded.slotCount());
		assertEquals(27, decoded.slots().size());
		for (int slot = 0; slot < slots.size(); slot++) {
			assertStackEquals(slots.get(slot), decoded.slots().get(slot), slot);
		}
		assertTrue(RequestValidator.validate(decoded).isEmpty());
	}

	@Test
	void rejectsInventorySnapshotWithWrongSlotCount() {
		List<ItemStack> slots = emptySlots(42);
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryS2C("req-1", TARGET, 1, 43, slots)).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryS2C("req-1", TARGET, 1, 27, emptySlots(27))).orElseThrow());
	}

	@Test
	void rejectsEnderChestSnapshotWithWrongSlotCount() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerEnderChestS2C("req-1", TARGET, 1, 43, emptySlots(43))).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerEnderChestS2C("req-1", TARGET, 1, 27, emptySlots(26))).orElseThrow());
	}

	@Test
	void rejectsNullSnapshotCollections() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryS2C("req-1", TARGET, 1, 43, null)).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerEnderChestS2C("req-1", TARGET, 1, 27, null)).orElseThrow());
	}

	@Test
	void rejectsNegativeRevision() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryS2C("req-1", TARGET, -1, 43, emptySlots(43))).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerEnderChestS2C("req-1", TARGET, -1, 27, emptySlots(27))).orElseThrow());
	}

	@Test
	void rejectsNullTargetAndInvalidRequestIdOnSnapshots() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryS2C("req-1", null, 1, 43, emptySlots(43))).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryS2C(null, TARGET, 1, 43, emptySlots(43))).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryS2C("req 1", TARGET, 1, 43, emptySlots(43))).orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerEnderChestS2C("req-1", null, 1, 27, emptySlots(27))).orElseThrow());
	}

	@Test
	void rejectsOversizedInventoryCollectionOnEncode() {
		PlayerInventoryS2C oversized = new PlayerInventoryS2C("req-1", TARGET, 1, 43, emptySlots(44));
		RegistryFriendlyByteBuf buffer = buffer();
		assertThrows(EncoderException.class, () -> PlayerInventoryS2C.STREAM_CODEC.encode(buffer, oversized));
	}

	@Test
	void rejectsOversizedEnderChestCollectionOnEncode() {
		PlayerEnderChestS2C oversized = new PlayerEnderChestS2C("req-1", TARGET, 1, 27, emptySlots(28));
		RegistryFriendlyByteBuf buffer = buffer();
		assertThrows(EncoderException.class, () -> PlayerEnderChestS2C.STREAM_CODEC.encode(buffer, oversized));
	}

	@Test
	void rejectsOversizedDeclaredCountOnDecode() {
		RegistryFriendlyByteBuf buffer = buffer();
		buffer.writeUtf("req-1");
		UUIDUtil.STREAM_CODEC.encode(buffer, TARGET);
		buffer.writeVarInt(1);
		buffer.writeVarInt(43);
		buffer.writeVarInt(44);
		assertThrows(DecoderException.class, () -> PlayerInventoryS2C.STREAM_CODEC.decode(buffer));
	}

	@Test
	void inventoryChunkRoundTrips() {
		List<ItemStack> slots = List.of(stack(Items.STONE, 5), ItemStack.EMPTY, stack(Items.SHIELD, 1));

		PlayerInventoryChunkS2C original = new PlayerInventoryChunkS2C("req-3", TARGET, 12, 43, 1, 3, 9, slots);
		RegistryFriendlyByteBuf buffer = buffer();
		PlayerInventoryChunkS2C.STREAM_CODEC.encode(buffer, original);
		PlayerInventoryChunkS2C decoded = PlayerInventoryChunkS2C.STREAM_CODEC.decode(buffer);

		assertEquals("req-3", decoded.requestId());
		assertEquals(TARGET, decoded.target());
		assertEquals(12, decoded.revision());
		assertEquals(43, decoded.slotCount());
		assertEquals(1, decoded.chunkIndex());
		assertEquals(3, decoded.chunkCount());
		assertEquals(9, decoded.firstSlot());
		assertEquals(12, decoded.endSlot());
		assertEquals(3, decoded.slots().size());
		assertStackEquals(slots.get(0), decoded.slots().get(0), 0);
		assertTrue(decoded.slots().get(1).isEmpty());
		assertStackEquals(slots.get(2), decoded.slots().get(2), 2);
		assertTrue(RequestValidator.validate(decoded).isEmpty());
	}

	@Test
	void rejectsChunkWithSlotRangeBeyondTheSnapshot() {
		List<ItemStack> slots = emptySlots(4);
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryChunkS2C("req-1", TARGET, 1, 43, 0, 1, 41, slots))
				.orElseThrow());
	}

	@Test
	void rejectsChunkWithInvalidIndices() {
		List<ItemStack> slots = emptySlots(2);
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryChunkS2C("req-1", TARGET, 1, 43, 3, 3, 0, slots))
				.orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryChunkS2C("req-1", TARGET, 1, 43, 0, 0, 0, slots))
				.orElseThrow());
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryChunkS2C("req-1", TARGET, 1, 43, 0, 1, -1, slots))
				.orElseThrow());
	}

	@Test
	void rejectsChunkWithWrongSlotCount() {
		assertEquals(ErrorCode.INVALID_REQUEST,
			RequestValidator.validate(new PlayerInventoryChunkS2C("req-1", TARGET, 1, 27, 0, 1, 0, emptySlots(2)))
				.orElseThrow());
	}

	@Test
	void rejectsOversizedChunkCollectionOnEncode() {
		PlayerInventoryChunkS2C oversized = new PlayerInventoryChunkS2C("req-1", TARGET, 1, 43, 0, 1, 0,
			emptySlots(44));
		RegistryFriendlyByteBuf buffer = buffer();
		assertThrows(EncoderException.class, () -> PlayerInventoryChunkS2C.STREAM_CODEC.encode(buffer, oversized));
	}

	private static List<ItemStack> emptySlots(int count) {
		List<ItemStack> slots = new ArrayList<>(count);
		for (int slot = 0; slot < count; slot++) {
			slots.add(ItemStack.EMPTY);
		}
		return slots;
	}

	private static RegistryFriendlyByteBuf buffer() {
		return new RegistryFriendlyByteBuf(Unpooled.buffer(),
			RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
	}

	private static void assertStackEquals(ItemStack expected, ItemStack actual, int slot) {
		assertEquals(expected.isEmpty(), actual.isEmpty(), "empty mismatch at slot " + slot);
		assertEquals(expected.getCount(), actual.getCount(), "count mismatch at slot " + slot);
		if (!expected.isEmpty()) {
			assertEquals(expected.getItem(), actual.getItem(), "item mismatch at slot " + slot);
		}
	}
}
