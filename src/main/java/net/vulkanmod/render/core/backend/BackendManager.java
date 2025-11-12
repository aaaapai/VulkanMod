package net.vulkanmod.render.core.backend;

import org.jetbrains.annotations.Nullable;

/**
 * Simple registry for the active rendering backend. Right now we only expose a Vulkan implementation,
 * but the indirection keeps the mixins decoupled from concrete classes and eases testing.
 */
public final class BackendManager {

    private static final RenderBackend BACKEND = new VulkanBackend();

    private BackendManager() {
    }

    public static RenderBackend get() {
        return BACKEND;
    }

    @Nullable
    public static FrameGraphContext currentContext() {
        return BACKEND.currentContext();
    }
}
