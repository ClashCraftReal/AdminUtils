package com.lorenz.adminutils.client.gui.page;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.lorenz.adminutils.client.gui.AdminScreen;
import com.lorenz.adminutils.client.gui.GuiTheme;
import com.lorenz.adminutils.client.gui.NavigationSection;
import com.lorenz.adminutils.client.gui.RenderContext;
import com.lorenz.adminutils.client.gui.page.players.PlayerListPanel;
import com.lorenz.adminutils.client.gui.page.players.PlayerProfilePanel;
import com.lorenz.adminutils.client.gui.widget.AdminButton;
import com.lorenz.adminutils.client.net.ClientSession;
import com.lorenz.adminutils.client.net.ConnectionState;
import com.lorenz.adminutils.client.net.PlayerInventoryStore;
import com.lorenz.adminutils.client.player.PlayerDirectory;
import com.lorenz.adminutils.client.player.PlayerListEntry;
import com.lorenz.adminutils.client.player.PlayerListFilter;
import com.lorenz.adminutils.client.player.SkinTextureCache;
import com.lorenz.adminutils.net.payload.PlayerInventoryRequestC2S;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/**
 * Online player directory with search, sorting, a read-only profile and a read-only
 * inventory view.
 *
 * <p>The list is sourced from the client's own tab list
 * ({@code ClientPacketListener.getOnlinePlayers()}), so it contains exactly the
 * players this client already knows about. No offline player data is read.
 *
 * <p>State that must survive a widget rebuild - the search text, sort order, scroll
 * offset and selection - lives in fields, because {@code AdminScreen.init()} calls
 * {@link #init} again on every rebuild.
 */
public final class PlayersPage extends AdminPage {
	private static final int SEARCH_HEIGHT = 18;
	private static final int SORT_HEIGHT = 18;
	private static final int CONTROL_GAP = 4;
	private static final int PANEL_GAP = 8;
	private static final int MIN_LIST_WIDTH = 120;
	private static final int MAX_LIST_WIDTH = 220;
	private static final double LIST_WIDTH_RATIO = 0.35;
	/** Below this page width the list and the profile are stacked instead of split. */
	private static final int STACK_THRESHOLD = 300;
	private static final int MIN_STACKED_PROFILE = 120;

	private final PlayerDirectory directory = new PlayerDirectory();
	private final SkinTextureCache skins = new SkinTextureCache();
	private final PlayerInventoryStore inventory = PlayerInventoryStore.shared();

	private String search = "";
	private PlayerListFilter.SortOrder order = PlayerListFilter.SortOrder.ASCENDING;
	private int scroll;

	/**
	 * Filtered view, recomputed only when the query, the sort order or the directory
	 * changes. Rendering runs every frame, so rebuilding this list per frame would
	 * allocate and sort for no reason.
	 */
	private List<PlayerListEntry> visible = List.of();
	private boolean visibleDirty = true;

	private int listX;
	private int listY;
	private int listWidth;
	private int listHeight;
	private int profileX;
	private int profileY;
	private int profileWidth;
	private int profileHeight;

	public PlayersPage() {
		super(NavigationSection.PLAYERS);
		// Reopening the screen while a request was still in flight would otherwise leave
		// the store in LOADING with no tick to expire it, so the new page starts clean.
		if (this.inventory.state() == PlayerInventoryStore.State.LOADING) {
			this.inventory.clear();
		}
	}

	@Override
	public Component title() {
		return Component.literal("Players");
	}

	@Override
	public Component subtitle() {
		return Component.literal("Online players on this server");
	}

	@Override
	public void init(AdminScreen screen, int x, int y, int width, int height) {
		layout(x, y, width, height);

		EditBox searchBox = new EditBox(Minecraft.getInstance().font, x, y, this.listWidth, SEARCH_HEIGHT,
			Component.literal("Search"));
		searchBox.setMaxLength(32);
		searchBox.setValue(this.search);
		searchBox.setResponder(value -> {
			this.search = value == null ? "" : value;
			this.scroll = 0;
			this.visibleDirty = true;
		});
		screen.addWidget(searchBox);

		screen.addButton(AdminButton.create(Component.literal("Sort: " + this.order.label()),
			x, y + SEARCH_HEIGHT + CONTROL_GAP, this.listWidth, SORT_HEIGHT, true, pressed -> {
				this.order = this.order.toggled();
				this.scroll = 0;
				this.visibleDirty = true;
				screen.refresh();
			}));
	}

	/**
	 * Refreshes the directory from the tab list and keeps the inventory request in step
	 * with the selection. Only a join or leave changes the entry list, so the skin cache
	 * is pruned and the scroll clamped on those ticks alone.
	 *
	 * <p>The store is ticked before the request check so an expired request becomes
	 * retryable in the same tick instead of one tick later.
	 */
	@Override
	public void tick() {
		if (this.directory.update(readOnlinePlayers())) {
			Set<UUID> ids = new HashSet<>();
			for (PlayerListEntry entry : this.directory.entries()) {
				ids.add(entry.id());
			}
			this.skins.retain(ids);
			this.visibleDirty = true;
			this.scroll = PlayerListPanel.clampScroll(this.scroll, filtered().size(), this.listHeight);
		}
		this.inventory.tick();
		syncInventory();
	}

	/**
	 * Requests a snapshot for the selection, then refreshes it once per
	 * {@link PlayerInventoryStore#refreshIntervalMillis()} while the page stays open. A
	 * request that is still loading is never overlapped, and a failed request is retried at
	 * most {@link PlayerInventoryStore#maxAttempts()} times with a delay between attempts,
	 * so the page cannot turn into a poll loop.
	 */
	private void syncInventory() {
		UUID selected = this.directory.selected();
		if (selected == null) {
			if (this.inventory.state() != PlayerInventoryStore.State.IDLE) {
				this.inventory.clear();
			}
			return;
		}
		if (!this.inventory.needsRequest(selected) || ClientSession.state() != ConnectionState.READY) {
			return;
		}
		String requestId = this.inventory.beginRequest(selected);
		if (requestId != null) {
			ClientPlayNetworking.send(new PlayerInventoryRequestC2S(requestId, selected));
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return false;
		}
		List<PlayerListEntry> entries = filtered();
		int index = PlayerListPanel.rowAt(mouseX, mouseY, this.listX, this.listY, this.listWidth, this.listHeight,
			this.scroll, entries.size());
		if (index < 0) {
			return false;
		}
		this.directory.select(entries.get(index).id());
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (mouseX < this.listX || mouseX >= this.listX + this.listWidth
			|| mouseY < this.listY || mouseY >= this.listY + this.listHeight) {
			return false;
		}
		int next = PlayerListPanel.scrollBy(this.scroll, vertical, filtered().size(), this.listHeight);
		if (next == this.scroll) {
			return false;
		}
		this.scroll = next;
		return true;
	}

	@Override
	public void render(RenderContext context, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
		layout(x, y, width, height);

		List<PlayerListEntry> entries = filtered();
		this.scroll = PlayerListPanel.clampScroll(this.scroll, entries.size(), this.listHeight);

		PlayerListPanel.render(context, font, entries, this.directory.selected(), localPlayerId(),
			this.listX, this.listY, this.listWidth, this.listHeight, this.scroll, mouseX, mouseY, this.skins);

		PlayerProfilePanel.render(context, font, this.directory.selectedEntry(), localPlayerId(),
			this.profileX, this.profileY, this.profileWidth, this.profileHeight, this.skins, this.inventory);

		context.text(font, Component.literal(entries.size() + " of " + this.directory.entries().size() + " online"),
			this.listX, y + height - GuiTheme.rowHeight(), GuiTheme.TEXT_MUTED);
	}

	/**
	 * Side-by-side above {@link #STACK_THRESHOLD}, stacked below it. A 9-column inventory
	 * grid needs roughly 110 pixels, which the split layout cannot give on a narrow
	 * window, so the profile takes the full width there instead of clipping the grid.
	 */
	private void layout(int x, int y, int width, int height) {
		int controls = SEARCH_HEIGHT + CONTROL_GAP + SORT_HEIGHT + CONTROL_GAP;
		this.listX = x;
		this.listY = y + controls;

		if (width < STACK_THRESHOLD) {
			int available = Math.max(0, height - controls);
			this.listWidth = width;
			this.listHeight = Math.max(0, Math.min(available / 2, available - MIN_STACKED_PROFILE));
			this.profileX = x;
			this.profileY = this.listY + this.listHeight + PANEL_GAP;
			this.profileWidth = width;
			this.profileHeight = Math.max(0, y + height - this.profileY);
			return;
		}

		this.listWidth = Math.max(MIN_LIST_WIDTH, Math.min(MAX_LIST_WIDTH, (int) (width * LIST_WIDTH_RATIO)));
		this.listHeight = Math.max(0, height - controls - GuiTheme.rowHeight());
		this.profileX = x + this.listWidth + PANEL_GAP;
		this.profileY = this.listY;
		this.profileWidth = Math.max(0, width - this.listWidth - PANEL_GAP);
		this.profileHeight = this.listHeight;
	}

	private List<PlayerListEntry> filtered() {
		if (this.visibleDirty) {
			this.visible = PlayerListFilter.apply(this.directory.entries(), this.search, this.order);
			this.visibleDirty = false;
		}
		return this.visible;
	}

	private static UUID localPlayerId() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player == null ? null : minecraft.player.getUUID();
	}

	/**
	 * Snapshot of the client tab list. {@code getOnlinePlayers()} is used rather than
	 * {@code getListedOnlinePlayers()} so players hidden by the server's listing
	 * setting are not silently omitted.
	 */
	private static List<PlayerListEntry> readOnlinePlayers() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null) {
			return List.of();
		}
		List<PlayerListEntry> entries = new ArrayList<>();
		for (PlayerInfo info : connection.getOnlinePlayers()) {
			if (info == null || info.getProfile() == null || info.getProfile().id() == null) {
				continue;
			}
			entries.add(new PlayerListEntry(info.getProfile().id(), info.getProfile().name()));
		}
		return entries;
	}
}
