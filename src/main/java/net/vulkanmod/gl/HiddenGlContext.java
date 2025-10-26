package net.vulkanmod.gl;

import net.vulkanmod.Initializer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

/**
 * Creates a tiny off-screen GLFW window so Mojang's GlDevice can bootstrap
 * without touching the Vulkan-backed main window.
 */
public final class HiddenGlContext {
    private static long glWindow = MemoryUtil.NULL;

    private HiddenGlContext() {
    }

    public static long getHandle() {
        if (glWindow != MemoryUtil.NULL) {
            return glWindow;
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);

        glWindow = GLFW.glfwCreateWindow(1, 1, "VulkanMod GL Stub", MemoryUtil.NULL, MemoryUtil.NULL);
        if (glWindow == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create stub GLFW window for GlDevice");
        }

        return glWindow;
    }

    public static void destroy() {
        if (glWindow != MemoryUtil.NULL) {
            GLFW.glfwDestroyWindow(glWindow);
            glWindow = MemoryUtil.NULL;
            Initializer.LOGGER.info("Destroyed VulkanMod stub GL context");
        }
    }
}
