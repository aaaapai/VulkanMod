package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.render.core.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(value = GlStateManager.class, remap = false)
public class GlStateManagerM {

    /**
     * @author
     * @reason Route texture binding through the Vulkan-backed texture map.
     */
    @Overwrite(remap = false)
    public static void _bindTexture(int id) {
        VkGlTexture.bindTexture(id);
    }

    /**
     * @author
     * @reason Forward scissor disables to the Vulkan renderer.
     */
    @Overwrite(remap = false)
    public static void _disableScissorTest() {
        Renderer.resetScissor();
    }

    /**
     * @author
     * @reason Scissor state is tracked on the Vulkan side, no GL enable is required.
     */
    @Overwrite(remap = false)
    public static void _enableScissorTest() {
    }

    /**
     * @author
     * @reason Apply scissor bounds via the Vulkan renderer.
     */
    @Overwrite(remap = false)
    public static void _scissorBox(int x, int y, int width, int height) {
        Renderer.setScissor(x, y, width, height);
    }

    /**
     * @author
     * @reason Keep depth testing state in sync with the Vulkan pipeline.
     */
    @Overwrite(remap = false)
    public static void _disableDepthTest() {
        VRenderSystem.disableDepthTest();
    }

    /**
     * @author
     * @reason Keep depth testing state in sync with the Vulkan pipeline.
     */
    @Overwrite(remap = false)
    public static void _enableDepthTest() {
        VRenderSystem.enableDepthTest();
    }

    /**
     * @author
     * @reason Mirror the configured depth function for the Vulkan backend.
     */
    @Overwrite(remap = false)
    public static void _depthFunc(int function) {
        VRenderSystem.depthFunc(function);
    }

    /**
     * @author
     * @reason Track depth mask changes inside the Vulkan renderer.
     */
    @Overwrite(remap = false)
    public static void _depthMask(boolean mask) {
        VRenderSystem.depthMask(mask);
    }

    /**
     * @author
     * @reason Disable blending within the Vulkan state manager.
     */
    @Overwrite(remap = false)
    public static void _disableBlend() {
        VRenderSystem.disableBlend();
    }

    /**
     * @author
     * @reason Enable blending within the Vulkan state manager.
     */
    @Overwrite(remap = false)
    public static void _enableBlend() {
        VRenderSystem.enableBlend();
    }

    /**
     * @author
     * @reason Propagate blend parameters to the Vulkan pipeline.
     */
    @Overwrite(remap = false)
    public static void _blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        VRenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }

    /**
     * @author
     * @reason Clear requests are handled by Vulkan rather than GL.
     */
    @Overwrite(remap = false)
    public static void _clear(int mask) {
        VRenderSystem.clear(mask);
    }

    /**
     * @author
     * @reason Forward viewport updates to Vulkan.
     */
    @Redirect(
            method = "_viewport",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glViewport(IIII)V"),
            remap = false
    )
    private static void vulkanmod$setViewport(int x, int y, int width, int height) {
        Renderer.setViewport(x, y, width, height);
    }

    /**
     * @author
     * @reason No GL error reporting when running on Vulkan.
     */
    @Overwrite(remap = false)
    public static int _getError() {
        return 0;
    }

    /**
     * @author
     * @reason Upload textures into Vulkan-backed images.
     */
    @Overwrite(remap = false)
    public static void _texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
        RenderSystem.assertOnRenderThread();
        VkGlTexture.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
    }

    /**
     * @author
     * @reason Upload textures into Vulkan-backed images.
     */
    @Overwrite(remap = false)
    public static void _texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, long pixels) {
        RenderSystem.assertOnRenderThread();
        VkGlTexture.texSubImage2D(target, level, offsetX, offsetY, width, height, format, type, pixels);
    }

    /**
     * @author
     * @reason Upload textures into Vulkan-backed images.
     */
    @Overwrite(remap = false)
    public static void _texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, ByteBuffer pixels) {
        RenderSystem.assertOnRenderThread();
        VkGlTexture.texSubImage2D(target, level, offsetX, offsetY, width, height, format, type, pixels);
    }

    /**
     * @author
     * @reason Track active texture unit inside the Vulkan binding tables.
     */
    @Overwrite(remap = false)
    public static void _activeTexture(int texture) {
        VkGlTexture.activeTexture(texture);
    }

    @Inject(method = "_texParameter(III)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$texParameterInt(int target, int pname, int param, CallbackInfo ci) {
        VkGlTexture.texParameteri(target, pname, param);
        ci.cancel();
    }

    /**
     * @author
     * @reason Maintain pixel store state for Vulkan texture uploads.
     */
    @Overwrite(remap = false)
    public static void _pixelStore(int pname, int param) {
        VkGlTexture.pixelStoreI(pname, param);
    }

    /**
     * @author
     * @reason Allocate Vulkan texture handles instead of GL textures.
     */
    @Overwrite(remap = false)
    public static int _genTexture() {
        return VkGlTexture.genTextureId();
    }

    /**
     * @author
     * @reason Release Vulkan texture handles when GL deletes textures.
     */
    @Overwrite(remap = false)
    public static void _deleteTexture(int id) {
        VkGlTexture.glDeleteTextures(id);
    }

    /**
     * @author
     * @reason Bind framebuffers through the Vulkan compatibility layer.
     */
    @Overwrite(remap = false)
    public static void _glBindFramebuffer(int target, int framebuffer) {
        VkGlFramebuffer.bindFramebuffer(target, framebuffer);
    }

    /**
     * @author
     * @reason Create framebuffer identifiers provided by the Vulkan layer.
     */
    @Overwrite(remap = false)
    public static int glGenFramebuffers() {
        return VkGlFramebuffer.genFramebufferId();
    }

    /**
     * @author
     * @reason Attach textures managed by Vulkan to framebuffer slots.
     */
    @Overwrite(remap = false)
    public static void _glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        VkGlFramebuffer.framebufferTexture2D(target, attachment, textarget, texture, level);
    }

    /**
     * @author
     * @reason Present operations are handled explicitly; default GL blit is unused.
     */
    @Overwrite(remap = false)
    public static void _glBlitFrameBuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        VkGlFramebuffer.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    /**
     * @author
     * @reason Ensure Vulkan framebuffers are cleaned alongside GL calls.
     */
    @Overwrite(remap = false)
    public static void _glDeleteFramebuffers(int framebuffer) {
        VkGlFramebuffer.deleteFramebuffer(framebuffer);
    }

    /**
     * @author
     * @reason Cull state is tracked on the Vulkan renderer.
     */
    @Overwrite(remap = false)
    public static void _enableCull() {
        VRenderSystem.enableCull();
    }

    /**
     * @author
     * @reason Cull state is tracked on the Vulkan renderer.
     */
    @Overwrite(remap = false)
    public static void _disableCull() {
        VRenderSystem.disableCull();
    }
}
