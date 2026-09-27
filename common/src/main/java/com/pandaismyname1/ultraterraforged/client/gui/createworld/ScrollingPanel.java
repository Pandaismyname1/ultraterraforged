package com.pandaismyname1.ultraterraforged.client.gui.createworld;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;

/**
 * A vertical list of widgets that scrolls when it doesn't fit. Tabs can only contain widgets, so the list is one
 * widget that positions, clips and forwards input to its children.
 */
public class ScrollingPanel extends AbstractWidget {
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 4;
	private static final int SCROLLBAR_WIDTH = 4;
	// room for the marker that option widgets draw left of themselves when changed
	private static final int LEFT_INSET = 4;

	private final List<AbstractWidget> children = new ArrayList<>();
	private double scroll;
	@Nullable
	private AbstractWidget pressed;

	public ScrollingPanel() {
		super(0, 0, 0, 0, CommonComponents.EMPTY);
	}

	public void setChildren(List<? extends AbstractWidget> children) {
		this.children.clear();
		this.children.addAll(children);
		this.pressed = null;
		this.scroll = Mth.clamp(this.scroll, 0.0D, this.maxScroll());
	}

	public void setBounds(int x, int y, int width, int height) {
		this.setX(x);
		this.setY(y);
		this.width = width;
		this.height = height;
		this.scroll = Mth.clamp(this.scroll, 0.0D, this.maxScroll());
	}

	public void scrollToBottom() {
		this.scroll = this.maxScroll();
	}

	private int contentHeight() {
		return Math.max(0, this.children.size() * (ROW_HEIGHT + ROW_SPACING) - ROW_SPACING);
	}

	private double maxScroll() {
		return Math.max(0, this.contentHeight() - this.height);
	}

	private void layoutChildren() {
		int childWidth = (this.maxScroll() > 0 ? this.width - SCROLLBAR_WIDTH - 2 : this.width) - LEFT_INSET;
		int y = this.getY() - (int) this.scroll;
		for (AbstractWidget child : this.children) {
			child.setX(this.getX() + LEFT_INSET);
			child.setY(y);
			child.setWidth(childWidth);
			child.height = ROW_HEIGHT;
			child.visible = y + ROW_HEIGHT > this.getY() && y < this.getY() + this.height;
			y += ROW_HEIGHT + ROW_SPACING;
		}
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.layoutChildren();
		// children outside the panel must not react to the mouse
		boolean inside = this.isMouseOver(mouseX, mouseY);
		int childMouseX = inside ? mouseX : -1;
		int childMouseY = inside ? mouseY : -1;

		graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
		for (AbstractWidget child : this.children) {
			if (child.visible) {
				child.render(graphics, childMouseX, childMouseY, partialTick);
			}
		}
		graphics.disableScissor();

		double maxScroll = this.maxScroll();
		if (maxScroll > 0) {
			int barX = this.getX() + this.width - SCROLLBAR_WIDTH;
			int thumbHeight = Math.max(16, (int) (this.height * (this.height / (double) this.contentHeight())));
			int thumbY = this.getY() + (int) ((this.height - thumbHeight) * (this.scroll / maxScroll));
			graphics.fill(barX, this.getY(), barX + SCROLLBAR_WIDTH, this.getY() + this.height, 0x40000000);
			graphics.fill(barX, thumbY, barX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFFA0A0A0);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.active || !this.visible || !this.isMouseOver(mouseX, mouseY)) {
			return false;
		}
		this.layoutChildren();
		for (AbstractWidget child : this.children) {
			if (child.visible && child.isMouseOver(mouseX, mouseY) && child.mouseClicked(mouseX, mouseY, button)) {
				this.pressed = child;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.pressed != null && this.pressed.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.pressed != null) {
			AbstractWidget pressed = this.pressed;
			this.pressed = null;
			return pressed.mouseReleased(mouseX, mouseY, button);
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
		if (!this.isMouseOver(mouseX, mouseY) || this.maxScroll() <= 0) {
			return false;
		}
		this.scroll = Mth.clamp(this.scroll - delta * (ROW_HEIGHT + ROW_SPACING), 0.0D, this.maxScroll());
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}
}
