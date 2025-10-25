package net.vulkanmod.config.gui;

import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;

public abstract class GuiElement implements GuiEventListener, NarratableEntry {

    protected int width;
    protected int height;
    public int x;
    public int y;

    protected boolean hovered;
    protected long hoverStartTime;
    protected int hoverTime;
    protected long hoverStopTime;

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void updateState(double mX, double mY) {
        // Update hover
        if (isMouseOver(mX, mY)) {
            if (!this.hovered) {
                this.hoverStartTime = Util.getMillis();
            }

            this.hovered = true;
            this.hoverTime = (int) (Util.getMillis() - this.hoverStartTime);
        } else {
            if (this.hovered) {
                this.hoverStopTime = Util.getMillis();
            }
            this.hovered = false;
            this.hoverTime = 0;
        }
    }

    public float getHoverMultiplier(float time) {
        if (this.hovered) {
            return Math.min(((this.hoverTime) / time), 1.0f);
        }
        else {
            int delta = (int) (Util.getMillis() - this.hoverStopTime);
            return Math.max(1.0f - (delta / time), 0.0f);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        return this.mouseClicked(mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        return this.mouseReleased(mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button());
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        return this.mouseDragged(mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button(), dragX, dragY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        return this.keyPressed(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        return this.keyReleased(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers());
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        return this.charTyped((char)characterEvent.codepoint(), characterEvent.modifiers());
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseY >= this.y
                && mouseX <= (this.x + this.width) && mouseY <= (this.y + this.height);
    }


    @Override
    public ScreenRectangle getRectangle() {
        return GuiEventListener.super.getRectangle();
    }

    @Override
    public void setFocused(boolean bl) {

    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
}
