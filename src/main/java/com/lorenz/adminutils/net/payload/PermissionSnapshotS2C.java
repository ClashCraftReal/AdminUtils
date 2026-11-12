package com.lorenz.adminutils.net.payload;

import java.util.Set;

import com.lorenz.adminutils.api.PermissionSnapshot;
import com.lorenz.adminutils.net.Protocol;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client permission view, used only to enable or grey out UI. The client
 * must never treat this as authorization.
 */
public record PermissionSnapshotS2C(String permissionsCsv, boolean enabled, boolean operator, boolean opBypass)
	implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PermissionSnapshotS2C> TYPE =
		new CustomPacketPayload.Type<>(Protocol.channel("permission_snapshot"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PermissionSnapshotS2C> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, PermissionSnapshotS2C::permissionsCsv,
		ByteBufCodecs.BOOL, PermissionSnapshotS2C::enabled,
		ByteBufCodecs.BOOL, PermissionSnapshotS2C::operator,
		ByteBufCodecs.BOOL, PermissionSnapshotS2C::opBypass,
		PermissionSnapshotS2C::new);

	public static PermissionSnapshotS2C of(PermissionSnapshot snapshot) {
		return new PermissionSnapshotS2C(snapshot.csv(), snapshot.enabled(), snapshot.operator(), snapshot.opBypass());
	}

	public Set<String> permissions() {
		return PermissionSnapshot.parseCsv(permissionsCsv);
	}

	public PermissionSnapshot toSnapshot() {
		return new PermissionSnapshot(enabled, operator, opBypass, permissions());
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
