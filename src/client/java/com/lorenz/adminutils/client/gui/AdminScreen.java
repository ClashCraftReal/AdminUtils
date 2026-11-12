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
	private final NavigationSection section;
	private final NotificationStack notifications = new NotificationStack();
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

	@Override
	protected void init() {
		this.page = createPage();
		buildNavigation();
		this.page.init(this, contentX(), contentY(), contentWidth(), contentHeight());
	}

	protected abstract AdminPage createPage();

	public Button addButton(Button button) {
		return this.addRenderableWidget(button);
	}

	private void buildNavigation() {
		NavigationSection[] sections = NavigationSection.values();
		for (int index = 0; index < sections.length; index++) {
			NavigationSection target = sections[index];
			int y = GuiTheme.NAV_TOP + index * (GuiTheme.NAV_ITEM_HEIGHT + GuiTheme.NAV_ITEM_GAP);
			boolean current = target == this.section;
			boolean permitted = ClientSession.can(target.permission());
			String label = current ? "> " + target.label() : target.label();

			Button button = AdminButton.create(Component.literal(label), 6, y, GuiTheme.NAV_WIDTH - 12,
				GuiTheme.NAV_ITEM_HEIGHT, permitted && !current, pressed -> AdminUtilsMenuScreen.open(target));
			this.addRenderableWidget(button);
		}
	}

	//? if <26.1 {
	/*@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		super.render(graphics, mouseX, mouseY, delta);
		renderContent(new RenderContext(graphics), mouseX, mouseY, delta);
	}
	*///?} else {
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		renderContent(new RenderContext(graphics), mouseX, mouseY, delta);
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

		context.fill(x, y, x + width, y + height, GuiTheme.PANEL);
		context.outline(x, y, width, height, GuiTheme.BORDER);

		context.text(font, page.title(), x + GuiTheme.PADDING, y + 8, GuiTheme.TEXT);
		context.text(font, page.subtitle(), x + GuiTheme.PADDING, y + 20, GuiTheme.TEXT_MUTED);

		page.render(context, font, x + GuiTheme.PADDING, y + 36, width - GuiTheme.PADDING * 2,
			height - 44, mouseX, mouseY);

		StatusIndicator.render(context, font, 8, this.height - 30);
		notifications.render(context, font, this.width - GuiTheme.PADDING, GuiTheme.PADDING, 170);
	}

	private int contentX() {
		return GuiTheme.NAV_WIDTH + GuiTheme.PADDING;
	}

	private int contentY() {
		return GuiTheme.PADDING;
	}

	private int contentWidth() {
		return Math.max(80, this.width - contentX() - GuiTheme.PADDING);
	}

	private int contentHeight() {
		return Math.max(60, this.height - contentY() - GuiTheme.PADDING);
	}

	private static Font font() {
		return Minecraft.getInstance().font;
	}
}
