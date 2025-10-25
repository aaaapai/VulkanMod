package net.vulkanmod.render.material;

import net.vulkanmod.util.TriState;

import java.util.Objects;

/**
 * Immutable material descriptor used by the new rendering path.
 * <p>
 * Materials are interned via {@link RenderMaterialRegistry} so instances
 * can be compared with {@code ==} once retrieved from the registry.
 */
public final class RenderMaterial {
    private final BlendMode blendMode;
    private final boolean disableColorIndex;
    private final boolean emissive;
    private final boolean disableDiffuse;
    private final TriState ambientOcclusion;
    private final TriState glint;
    private final ShadeMode shadeMode;

    RenderMaterial(BlendMode blendMode,
                   boolean disableColorIndex,
                   boolean emissive,
                   boolean disableDiffuse,
                   TriState ambientOcclusion,
                   TriState glint,
                   ShadeMode shadeMode) {
        this.blendMode = Objects.requireNonNull(blendMode, "blendMode");
        this.ambientOcclusion = Objects.requireNonNull(ambientOcclusion, "ambientOcclusion");
        this.glint = Objects.requireNonNull(glint, "glint");
        this.shadeMode = Objects.requireNonNull(shadeMode, "shadeMode");
        this.disableColorIndex = disableColorIndex;
        this.emissive = emissive;
        this.disableDiffuse = disableDiffuse;
    }

    public BlendMode blendMode() {
        return this.blendMode;
    }

    public boolean disableColorIndex() {
        return this.disableColorIndex;
    }

    public boolean emissive() {
        return this.emissive;
    }

    public boolean disableDiffuse() {
        return this.disableDiffuse;
    }

    public TriState ambientOcclusion() {
        return this.ambientOcclusion;
    }

    public TriState glint() {
        return this.glint;
    }

    public ShadeMode shadeMode() {
        return this.shadeMode;
    }

    RenderMaterial withDisableDiffuse(boolean disable) {
        if (disable == this.disableDiffuse) {
            return this;
        }

        return RenderMaterialRegistry.intern(new RenderMaterial(this.blendMode,
                                                                this.disableColorIndex,
                                                                this.emissive,
                                                                disable,
                                                                this.ambientOcclusion,
                                                                this.glint,
                                                                this.shadeMode));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof RenderMaterial that)) {
            return false;
        }

        return this.disableColorIndex == that.disableColorIndex
               && this.emissive == that.emissive
               && this.disableDiffuse == that.disableDiffuse
               && this.blendMode == that.blendMode
               && this.ambientOcclusion == that.ambientOcclusion
               && this.glint == that.glint
               && this.shadeMode == that.shadeMode;
    }

    @Override
    public int hashCode() {
        int result = this.blendMode.hashCode();
        result = 31 * result + Boolean.hashCode(this.disableColorIndex);
        result = 31 * result + Boolean.hashCode(this.emissive);
        result = 31 * result + Boolean.hashCode(this.disableDiffuse);
        result = 31 * result + this.ambientOcclusion.hashCode();
        result = 31 * result + this.glint.hashCode();
        result = 31 * result + this.shadeMode.hashCode();
        return result;
    }
}
