package com.toroidalworld.client.shape;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ScrollableContents extends AbstractContainerWidget {
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");
    private static final ResourceLocation SCROLLER_BACKGROUND_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/scroller_background");

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_SPACING = 4;
    private static final int MIN_SCROLLER_HEIGHT = 32;
    private static final double SCROLL_RATE = 10.0;

    private final Layout contents;
    private final List<AbstractWidget> children = new ArrayList<>();

    private double scrollAmount;
    private boolean scrolling;

    public ScrollableContents(Layout contents) {
        super(0, 0, contents.getWidth(), contents.getHeight(), CommonComponents.EMPTY);
        this.contents = contents;
    }

    public void setMaxHeight(int maxHeight) {
        this.contents.arrangeElements();
        this.children.clear();
        this.contents.visitWidgets(this.children::add);
        this.setWidth(this.contents.getWidth() + 2 * scrollbarReserve());
        this.setHeight(Math.min(this.contents.getHeight(), maxHeight));
        this.setScrollAmount(this.scrollAmount);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.contents.setX(x + scrollbarReserve());
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.contents.setY(y - (int) this.scrollAmount);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        super.setFocused(focused);
        if (focused == null) {
            return;
        }

        ScreenRectangle rectangle = focused.getRectangle();
        int above = rectangle.top() - this.getY();
        int below = rectangle.bottom() - (this.getY() + this.getHeight());
        if (above < 0) {
            this.setScrollAmount(this.scrollAmount + above);
        } else if (below > 0) {
            this.setScrollAmount(this.scrollAmount + below);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.overScrollbar(mouseX, mouseY)) {
            this.scrolling = button == 0;
            return this.scrolling;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.scrolling) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        int travel = Math.max(1, this.getHeight() - this.scrollerHeight());
        this.setScrollAmount(this.scrollAmount + dragY * this.maxScrollAmount() / travel);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.setScrollAmount(this.scrollAmount - scrollY * SCROLL_RATE);
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(this.getX(), this.getY(),
                this.getX() + this.getWidth(), this.getY() + this.getHeight());
        for (AbstractWidget child : this.children) {
            child.render(graphics, mouseX, mouseY, partialTick);
        }

        graphics.disableScissor();
        this.renderScrollbar(graphics);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int maxScroll = this.maxScrollAmount();
        if (maxScroll <= 0) {
            return;
        }

        int scrollerHeight = this.scrollerHeight();
        int x = this.getX() + this.getWidth() - SCROLLBAR_WIDTH;
        int y = this.getY() + (int) this.scrollAmount * (this.getHeight() - scrollerHeight) / maxScroll;
        RenderSystem.enableBlend();
        graphics.blitSprite(SCROLLER_BACKGROUND_SPRITE, x, this.getY(), SCROLLBAR_WIDTH, this.getHeight());
        graphics.blitSprite(SCROLLER_SPRITE, x, y, SCROLLBAR_WIDTH, scrollerHeight);
        RenderSystem.disableBlend();
    }

    private void setScrollAmount(double amount) {
        this.scrollAmount = Mth.clamp(amount, 0.0, this.maxScrollAmount());
        this.contents.setY(this.getY() - (int) this.scrollAmount);
    }

    private int maxScrollAmount() {
        return Math.max(0, this.contents.getHeight() - this.getHeight());
    }

    private int scrollerHeight() {
        int contentHeight = Math.max(1, this.contents.getHeight());
        return Mth.clamp(this.getHeight() * this.getHeight() / contentHeight, MIN_SCROLLER_HEIGHT, this.getHeight());
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        return this.maxScrollAmount() > 0
                && mouseX >= this.getX() + this.getWidth() - SCROLLBAR_WIDTH
                && mouseX < this.getX() + this.getWidth()
                && mouseY >= this.getY()
                && mouseY < this.getY() + this.getHeight();
    }

    private static int scrollbarReserve() {
        return SCROLLBAR_SPACING + SCROLLBAR_WIDTH;
    }
}
