package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.option.CyclingOption;
import net.vulkanmod.render.shader.CustomRenderPipelines;
import net.vulkanmod.vulkan.util.ColorUtil;

public class CyclingOptionWidget extends OptionWidget<CyclingOption<?>> {
    private Button leftButton;
    private Button rightButton;

    private boolean focused;

    public CyclingOptionWidget(CyclingOption<?> option, int x, int y, int width, int height, Component name) {
        super(x, y, width, height, name);
        this.option = option;
        this.leftButton = new Button(this.controlX, 16, Button.Direction.LEFT);
        this.rightButton = new Button(this.controlX + this.controlWidth - 16, 16, Button.Direction.RIGHT);

//        updateDisplayedValue(option.getValueText());
    }

    @Override
    protected int getYImage(boolean hovered) {
        return 0;
    }

    public void renderControls(double mouseX, double mouseY) {
        this.renderBars();

        this.leftButton.setStatus(option.index() > 0);
        this.rightButton.setStatus(option.index() < option.getValues().length - 1);

        int color = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        Font textRenderer = Minecraft.getInstance().font;
        int x = this.controlX + this.controlWidth / 2;
        int y = this.y + (this.height - 9) / 2;
        GuiRenderer.drawCenteredString(textRenderer, this.getDisplayedValue(), x, y, color);

        this.leftButton.renderButton(mouseX, mouseY);
        this.rightButton.renderButton(mouseX, mouseY);
    }

    public void renderBars() {
        int count = option.getValues().length;
        int current = option.index();

        int margin = 30;
        int padding = 4;

        int barWidth = (this.controlWidth - (2 * margin) - (padding * count)) / count;
        int color = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.4f);
        int activeColor = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 1.0f);

        if (barWidth <= 0)
            return;

        for (int i = 0; i < count; i++) {
            int x0 = this.controlX + margin + i * (barWidth + padding);
            int y0 = this.y + this.height - 5;

            int c = i == current ? activeColor : color;
            GuiRenderer.fill(x0, y0, x0 + barWidth, (int) (y0 + 1.5f), c);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (leftButton.isHovered(mouseX, mouseY)) {
            option.prevValue();
        }
        else if (rightButton.isHovered(mouseX, mouseY)) {
            option.nextValue();
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {

    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {

    }

    @Override
    public void setFocused(boolean bl) {
        this.focused = bl;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    class Button {
        final int ACTIVE_COLOR = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.8f);
        final int HOVERED_COLOR = ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 1.0f);
        final int INACTIVE_COLOR = ColorUtil.ARGB.pack(0.3f, 0.3f, 0.3f, 0.8f);

        int x;
        int width;
        boolean active;
        Direction direction;

        Button(int x, int width, Direction direction) {
            this.x = x;
            this.width = width;
            this.active = true;
            this.direction = direction;
        }

        boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        void setStatus(boolean status) {
            this.active = status;
        }

        void renderButton(double mouseX, double mouseY) {
            float f = this.isHovered(mouseX, mouseY) && this.active ? 5.0f : 4.5f;

            int color;
            if (this.isHovered(mouseX, mouseY) && this.active) {
                color = HOVERED_COLOR;
            }
            else if (this.active) {
                color = ACTIVE_COLOR;
            }
            else {
                color = INACTIVE_COLOR;
            }

            float h = f;
            float w = f - 1.0f;
            float yC = y + height * 0.5f;
            float xC = x + width * 0.5f;

            float[][] vertices;
            if (this.direction == Direction.LEFT) {
                vertices = new float[][]{
                        {xC - w, yC},
                        {xC + w, yC + h},
                        {xC + w, yC - h},
                };
            }
            else {
                vertices = new float[][]{
                        {xC + w, yC},
                        {xC - w, yC - h},
                        {xC - w, yC + h},
                };
            }


            GuiRenderer.submitPolygon(CustomRenderPipelines.GUI_TRIANGLES, TextureSetup.noTexture(), vertices, color);
        }

        enum Direction {
            LEFT,
            RIGHT
        }
    }

}
