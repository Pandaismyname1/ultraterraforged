package com.pandaismyname1.ultraterraforged.client.gui.widget;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;

public class WidgetList<T extends AbstractWidget> extends ContainerObjectSelectionList<WidgetList.Entry<T>> {
	private boolean renderSelected;
	
    // the list spans top to bottom; the screen height isn't needed any more, and stays for the callers
    public WidgetList(Minecraft minecraft, int width, int screenHeight, int top, int bottom, int slotHeight) {
        super(minecraft, width, bottom - top, top, slotHeight);
    }
    
    public void select(T widget) {
    	for(Entry<T> entry : this.children()) {
    		if(entry.widget.equals(widget)) {
    			this.setSelected(entry);
    			return;
    		}
    	}
    }
    
    public <W extends T> W addWidget(W widget) {
        super.addEntry(new Entry<>(widget));
        return widget;
    }

    public void setRenderSelected(boolean renderSelected) {
    	this.renderSelected = renderSelected;
    }
    
    // entries of a container list aren't selectable, so the outline is drawn here
    @Override
    protected void extractItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, Entry<T> entry) {
        if (this.renderSelected && Objects.equals(this.getSelected(), entry)) {
            this.extractSelection(graphics, entry, this.isFocused() ? 0xFFFFFFFF : 0xFF808080);
        }
        super.extractItem(graphics, mouseX, mouseY, partialTicks, entry);
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected int scrollBarX() {
        return this.getRowRight() + 2;
    }

    public static class Entry<T extends AbstractWidget> extends ContainerObjectSelectionList.Entry<Entry<T>> {
        private T widget;

        public Entry(T widget) {
            this.widget = widget;
        }

        public T getWidget() {
        	return this.widget;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(this.widget);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            // the same area 1.21.1 passed to render
            int left = this.getContentX();
            int top = this.getContentY();
            int width = this.getWidth();
            int height = this.getContentHeight();
            int optionWidth = Math.min(396, width);
            int padding = (width - optionWidth) / 2;
            widget.setX(left + padding);
            widget.setY(top);
            widget.visible = true;
            widget.setWidth(optionWidth);
            widget.height = height - 1;
            widget.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }

		@Override
		public List<T> narratables() {
			return Collections.singletonList(this.widget);
		}
    }
}
