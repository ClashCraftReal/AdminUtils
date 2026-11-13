package com.lorenz.adminutils.client.gui;

//? if <26.1 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import com.lorenz.adminutils.AdminUtils;
import com.lorenz.adminutils.client.gui.page.AdminPage;
import com.lorenz.adminutils.client.gui.widget.AdminButton;
import com.lorenz.adminutils.client.gui.widget.NotificationStack;
import com.lorenz.adminutils.client.gui.widget.StatusIndicator;
import com.lorenz.adminutils.client.net.ClientSession;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shared chrome for every AdminUtils screen: backdrop, navigation rail, page header,
 * connection indicator and notification stack.
 *
 * <p>Version-specific workaround: the render entry point differs between the two
 * Minecraft generations. 1.21.11 overrides {@code render(GuiGraphics, ...)}; 26.1+
 * overrides {@code extractRenderState(GuiGraphicsExtractor, ...)}. Both delegate to
 * the same {@link #renderContent} implementation.
 */
public abstract class AdminScreen extends Screen {
	private static boolean navigationPending;

	private final NavigationSection section;
	private final NotificationStack notifications = NotificationStack.shared();
	private final Runnable sessionListener = this::onSessionChanged;
	private final long openedAtMillis = System.currentTimeMillis();
	private boolean rebuilding;
	private boolean active;
	private boolean pendingRefresh;
	private AdminPage page;

	protected AdminScreen(NavigationSection section) {
		super(Component.literal("AdminUtils"));
		this.section = section;
	}

	public NavigationSection section() {
		return section;
	}

	public NotificationStack notifications() {
		return notifications;
	}

	/**
	 * Marks the next {@link #removed()} as a section change rather than a close, so the
	 * shared notification stack is not cleared when the user navigates.
	 */
	protected static void beginNavigation() {
		navigationPending = true;
	}

	@Override
	protected void init() {
		// The page instance is created once per screen so page-local state such as a
		// selection or a pending delete confirmation survives a snapshot-driven rebuild.
		if (this.page == null) {
			this.page = createPage();
		}
		buildNavigation();
		// The page receives the same origin and size that renderContent uses, so widget
		// positions and drawn text share one coordinate space.
		int padding = GuiTheme.padding();
		this.page.init(this, contentX() + padding, contentY() + GuiTheme.headerHeight(),
			contentWidth() - padding * 2, contentHeight() - GuiTheme.headerHeight() - 8);
		this.active = true;
		navigationPending = false;
		ClientSession.removeListener(this.sessionListener);
		ClientSession.addListener(this.sessionListener);
	}

	@Override
	public void removed() {
		this.active = false;
		ClientSession.removeListener(this.sessionListener);
		if (navigationPending) {
			navigationPending = false;
		} else {
			notifications.clear();
		}
		super.removed();
	}

	/**
	 * Rebuilds the widgets when the server pushes a new snapshot or the connection
	 * state changes. Without this the menu keeps whatever snapshot existed when it was
	 * opened, which is why an operator could see "Operator: no / Permissions: none".
	 */
	private void onSessionChanged() {
		refresh();
	}

	/**
	 * Requests a widget rebuild on the next client tick. Rebuilding straight from a
	 * button callback would clear the widget list while it is being iterated, so the
	 * work is deferred instead.
	 */
	public void refresh() {
		if (this.active) {
			this.pendingRefresh = true;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.pendingRefresh) {
			return;
		}
		this.pendingRefresh = false;
		if (!this.active || this.rebuilding) {
			return;
		}
		this.rebuilding = true;
		try {
			this.rebuildWidgets();
		} finally {
			this.rebuilding = false;
		}
	}

	protected abstract AdminPage createPage();

	public Button addButton(Button button) {
		return this.addRenderableWidget(button);
	}

	private void buildNavigation() {
		NavigationSection[] sections = NavigationSection.values();
		for (int index = 0; index < sections.length; index++) {
			NavigationSection target = sections[index];
			int y = GuiTheme.navTop() + index * (GuiTheme.navItemHeight() + GuiTheme.navItemGap());
			boolean current = target == this.section;
			boolean permitted = target.isAccessible(ClientSession.can(target.permission()));
			String label = current ? "> " + target.label() : target.label();

			Button button = AdminButton.create(Component.literal(label), 6, y, GuiTheme.NAV_WIDTH - 12,
				GuiTheme.navItemHeight(), permitted && !current, pressed -> AdminUtilsMenuScreen.open(target));
			this.addRenderableWidget(button);
		}
	}

	//? if <26.1 {
	/*@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		// Chrome first, widgets last: renderContent paints opaque fills over the whole
		// screen, so drawing widgets before it would hide every navigation button.
		renderContent(new RenderContext(graphics), mouseX, mouseY, delta);
		super.render(graphics, mouseX, mouseY, delta);
	}
	*///?} else {
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// Chrome first, widgets last: renderContent paints opaque fills over the whole
		// screen, so drawing widgets before it would hide every navigation button.
		renderContent(new RenderContext(graphics), mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}
	//?}

	protected void renderContent(RenderContext context, int mouseX, int mouseY, float delta) {
		Font font = font();

		context.fill(0, 0, this.width, this.height, GuiTheme.BACKDROP);
		context.fill(0, 0, GuiTheme.NAV_WIDTH, this.height, GuiTheme.NAV_BACKGROUND);
		context.fill(GuiTheme.NAV_WIDTH, 0, GuiTheme.NAV_WIDTH + 1, this.height, GuiTheme.BORDER);
		context.text(font, Component.literal("AdminUtils"), 8, 10, GuiTheme.TEXT);
		context.text(font, Component.literal("v" + AdminUtils.modVersion()), 8, 21, GuiTheme.TEXT_MUTED);

		int x = contentX();
		int y = contentY();
		int width = contentWidth();
		int height = contentHeight();
		int padding = GuiTheme.padding();

		context.fill(x, y, x + width, y + height, GuiTheme.PANEL);
		context.outline(x, y, width, height, GuiTheme.BORDER);

		context.text(font, page.title(), x + padding, y + 8, GuiTheme.TEXT);
		context.text(font, page.subtitle(), x + padding, y + 20, GuiTheme.TEXT_MUTED);

		// Accent underline. It is the only decorative animation on the chrome and
		// collapses to its final width immediately when animations are disabled.
		int underlineWidth = Math.max(1, width - padding * 2);
		float reveal = GuiAnimation.progress(this.openedAtMillis, GuiAnimation.FAST_MILLIS);
		int visibleUnderline = Math.max(1, (int) (underlineWidth * reveal));
		context.fill(x + padding, y + 31, x + padding + visibleUnderline, y + 32, GuiTheme.accent());

		int pageX = x + padding;
		int pageY = y + GuiTheme.headerHeight();
		int pageWidth = width - padding * 2;
		int pageHeight = height - GuiTheme.headerHeight() - 8;

		// Page content is clipped to the panel so a long list cannot draw over the
		// navigation rail or past the panel border.
		context.scissor(pageX, pageY, pageWidth, pageHeight);
		try {
			page.render(context, font, pageX, pageY, pageWidth, pageHeight, mouseX, mouseY);
		} finally {
			context.resetScissor();
		}

		// Accent marker for the active section, aligned with its navigation button.
		int navY = GuiTheme.navTop() + this.section.ordinal() * (GuiTheme.navItemHeight() + GuiTheme.navItemGap());
		context.fill(4, navY, 6, navY + GuiTheme.navItemHeight(), GuiTheme.accent());

		StatusIndicator.render(context, font, 8, this.height - 30);
		notifications.render(context, font, this.width - padding, padding, 170);
	}

	private int contentX() {
		return GuiTheme.NAV_WIDTH + GuiTheme.padding();
	}

	private int contentY() {
		return GuiTheme.padding();
	}

	private int contentWidth() {
		return Math.max(80, this.width - contentX() - GuiTheme.padding());
	}

	private int contentHeight() {
		return Math.max(60, this.height - contentY() - GuiTheme.padding());
	}

	private static Font font() {
		return Minecraft.getInstance().font;
	}
}
