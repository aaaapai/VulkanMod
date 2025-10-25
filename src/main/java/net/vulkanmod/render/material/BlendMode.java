package net.vulkanmod.render.material;

/**
 * Blend mode indicator used by VulkanMod's material system.
 * <p>
 * The enum is intentionally small – only the modes referenced by
 * the existing chunk and item renderers are implemented. Additional
 * modes can be added later without breaking the binary layout used
 * by encoded quads.
 */
public enum BlendMode {
    DEFAULT,
    CUTOUT,
    CUTOUT_MIPPED,
    TRANSLUCENT;

    /**
     * Returns {@code true} when this mode should render with translucency.
     */
    public boolean isTranslucent() {
        return this == TRANSLUCENT;
    }
}