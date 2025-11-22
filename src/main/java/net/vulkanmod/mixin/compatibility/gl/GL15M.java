package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.VkGlBuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(GL15.class)
public class GL15M {

    /**
     * @author VulkanMod
     * @reason Provide deterministic buffer ids managed by VkGlBuffer.
     */
    @Overwrite(remap = false)
    @NativeType("void")
    public static int glGenBuffers() {
        return VkGlBuffer.glGenBuffers();
    }

    /**
     * @author VulkanMod
     * @reason Redirect binds to the emulated Vulkan buffer table.
     */
    @Overwrite(remap = false)
    public static void glBindBuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int buffer) {
        VkGlBuffer.glBindBuffer(target, buffer);
    }

    /**
     * @author VulkanMod
     * @reason Upload buffer contents through the Vulkan staging path.
     */
    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") ByteBuffer data, @NativeType("GLenum") int usage) {
        VkGlBuffer.glBufferData(target, data, usage);
    }

    /**
     * @author VulkanMod
     * @reason Size-only overload funnels through the same Vulkan pathway.
     */
    @Overwrite(remap = false)
    public static void glBufferData(int i, long l, int j) {
        VkGlBuffer.glBufferData(i, l, j);
    }

    /**
     * @author VulkanMod
     * @reason Map calls expose the Vulkan upload buffer to legacy callers.
     */
    @Overwrite(remap = false)
    @NativeType("void *")
    public static ByteBuffer glMapBuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int access) {
        return VkGlBuffer.glMapBuffer(target, access);
    }

    /**
     * @author VulkanMod
     * @reason Support LWJGL's length-aware overload with the same backing store.
     */
    @Overwrite(remap = false)
    @Nullable
    @NativeType("void *")
    public static ByteBuffer glMapBuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int access, long length, @Nullable ByteBuffer old_buffer) {
        return VkGlBuffer.glMapBuffer(target, access);
    }

    /**
     * @author VulkanMod
     * @reason Route unmap requests to VkGlBuffer to flush staged ranges.
     */
    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glUnmapBuffer(@NativeType("GLenum") int target) {
        return VkGlBuffer.glUnmapBuffer(target);
    }

    /**
     * @author VulkanMod
     * @reason Destroy buffers via the Vulkan memory manager.
     */
    @Overwrite(remap = false)
    public static void glDeleteBuffers(int i) {
        VkGlBuffer.glDeleteBuffers(i);
    }

    /**
     * @author VulkanMod
     * @reason Batched deletions also need to hit the Vulkan-side tables.
     */
    @Overwrite(remap = false)
    public static void glDeleteBuffers(@NativeType("GLuint const *") IntBuffer buffers) {
        VkGlBuffer.glDeleteBuffers(buffers);
    }
}
