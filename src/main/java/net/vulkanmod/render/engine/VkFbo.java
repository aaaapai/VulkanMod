package net.vulkanmod.render.engine;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.util.ARGB;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;

public class VkFbo {
    final int glId;
    final VkGpuTexture colorAttachment;
    final VkGpuTexture depthAttachment;

    int clear = 0;
    int clearColor = 0;
    float clearDepth = 0.0f;

    protected VkFbo(VkGpuTexture colorAttachment, VkGpuTexture depthAttachment) {
        this.glId = GlStateManager.glGenFramebuffers();
        this.colorAttachment = colorAttachment;
        this.depthAttachment = depthAttachment;

        // Direct access
        VkGlFramebuffer fbo = VkGlFramebuffer.getFramebuffer(this.glId);

        fbo.setAttachmentTexture(GL33.GL_COLOR_ATTACHMENT0, colorAttachment.id);
        if (depthAttachment != null) {
            fbo.setAttachmentTexture(GL33.GL_DEPTH_ATTACHMENT, depthAttachment.id);
        }
    }

    protected void bind() {
//        VkGlFramebuffer glFramebuffer = VkGlFramebuffer.getFramebuffer(this.glId);
//        VkGlFramebuffer.beginRendering(glFramebuffer);
//
//        Framebuffer framebuffer = glFramebuffer.getFramebuffer();
//        try (MemoryStack stack = stackPush()) {
//            framebuffer.beginRenderPass(currentCmdBuffer, renderPass, stack);
//        }

        VkGlFramebuffer.bindFramebuffer(GL33.GL_FRAMEBUFFER, this.glId);
        clearAttachments();
    }

    protected void clearAttachments() {
        if (clear != 0) {
            VRenderSystem.clearDepth(clearDepth);
            VRenderSystem.setClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));
            Renderer.clearAttachments(clear);

            clear = 0;
        }
    }

    protected void close() {
        VkGlFramebuffer.deleteFramebuffer(this.glId);
    }

    public boolean needsClear() {
        return this.clear != 0;
    }
}
