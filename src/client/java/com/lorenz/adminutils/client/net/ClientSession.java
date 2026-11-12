package com.lorenz.adminutils.client.net;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.lorenz.adminutils.api.AdminPermission;
import com.lorenz.adminutils.api.PermissionSnapshot;
import com.lorenz.adminutils.net.payload.ActionResponseS2C;

/**
 * Client-side session state. Everything here is advisory: it drives enabled/disabled
 * UI states only and is never used to authorise anything.
 */
public final class ClientSession {
	private static final List<Runnable> STATE_LISTENERS = new CopyOnWriteArrayList<>();
	private static final List<Consumer<ActionResponseS2C>> RESPONSE_LISTENERS = new CopyOnWriteArrayList<>();

	private static volatile ConnectionState state = ConnectionState.DISCONNECTED;
	private static volatile PermissionSnapshot snapshot = PermissionSnapshot.EMPTY;
	private static volatile String detail = "";

	private ClientSession() {
	}

	public static ConnectionState state() {
		return state;
	}

	public static PermissionSnapshot snapshot() {
		return snapshot;
	}

	public static String detail() {
		return detail;
	}

	public static boolean can(AdminPermission permission) {
		return snapshot.has(permission);
	}

	public static void addListener(Runnable listener) {
		STATE_LISTENERS.add(listener);
	}

	public static void removeListener(Runnable listener) {
		STATE_LISTENERS.remove(listener);
	}

	public static void addResponseListener(Consumer<ActionResponseS2C> listener) {
		RESPONSE_LISTENERS.add(listener);
	}

	public static void removeResponseListener(Consumer<ActionResponseS2C> listener) {
		RESPONSE_LISTENERS.remove(listener);
	}

	public static void update(ConnectionState newState, String newDetail) {
		state = newState;
		detail = newDetail == null ? "" : newDetail;
		notifyStateListeners();
	}

	public static void updateSnapshot(PermissionSnapshot newSnapshot) {
		snapshot = newSnapshot == null ? PermissionSnapshot.EMPTY : newSnapshot;
		if (state == ConnectionState.HANDSHAKING) {
			state = ConnectionState.READY;
			detail = "";
		}
		notifyStateListeners();
	}

	public static void dispatchResponse(ActionResponseS2C response) {
		for (Consumer<ActionResponseS2C> listener : RESPONSE_LISTENERS) {
			listener.accept(response);
		}
	}

	public static void reset() {
		state = ConnectionState.DISCONNECTED;
		snapshot = PermissionSnapshot.EMPTY;
		detail = "";
		notifyStateListeners();
	}

	private static void notifyStateListeners() {
		for (Runnable listener : STATE_LISTENERS) {
			listener.run();
		}
	}
}
