package net.vulkanmod.mixin.render;

import net.vulkanmod.gl.VkGlFramebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "com.mojang.blaze3d.opengl.DirectStateAccess$Emulated", remap = false)
public abstract class DirectStateAccessEmulatedMixin {

    @Overwrite(remap = false)
    public int createFrameBufferObject() {
        return VkGlFramebuffer.genFramebufferId();
    }

    @Overwrite(remap = false)
    public void bindFrameBufferTextures(int framebuffer, int colorTexture, int depthTexture, int level, int bindTarget) {
        if (framebuffer == 0) {
            VkGlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            return;
        }

        VkGlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);

        if (colorTexture != 0) {
            VkGlFramebuffer.framebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTexture, level);
        }

        if (depthTexture != 0) {
            VkGlFramebuffer.framebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, level);
        }

        if (bindTarget != 0) {
            VkGlFramebuffer.bindFramebuffer(bindTarget, framebuffer);
        } else {
            VkGlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
    }

    @Overwrite(remap = false)
    public void blitFrameBuffers(int srcFramebuffer, int dstFramebuffer,
                                 int srcX0, int srcY0, int srcX1, int srcY1,
                                 int dstX0, int dstY0, int dstX1, int dstY1,
                                 int mask, int filter) {
        VkGlFramebuffer.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcFramebuffer);
        VkGlFramebuffer.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dstFramebuffer);
        VkGlFramebuffer.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        VkGlFramebuffer.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        VkGlFramebuffer.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
    }
}
