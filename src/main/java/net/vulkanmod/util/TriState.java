package net.vulkanmod.util;

/**
 * Minimal tri-state representation used by VulkanMod's rendering helpers.
 */
public enum TriState {
    DEFAULT,
    TRUE,
    FALSE;

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isFalse() {
        return this == FALSE;
    }
}
