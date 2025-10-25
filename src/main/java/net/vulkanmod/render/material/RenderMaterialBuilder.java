package net.vulkanmod.render.material;

import net.vulkanmod.util.TriState;

/**
 * Fluent builder used to construct {@link RenderMaterial} instances.
 * Works similarly to Fabric's MaterialFinder but only exposes the knobs
 * relied upon by VulkanMod.
 */
public final class RenderMaterialBuilder {
    private BlendMode blendMode = BlendMode.DEFAULT;
    private boolean disableColorIndex;
    private boolean emissive;
    private boolean disableDiffuse;
    private TriState ambientOcclusion = TriState.DEFAULT;
    private TriState glint = TriState.DEFAULT;
    private ShadeMode shadeMode = ShadeMode.VANILLA;

    public RenderMaterialBuilder blendMode(BlendMode blendMode) {
        this.blendMode = blendMode;
        return this;
    }

    public RenderMaterialBuilder disableColorIndex(boolean disable) {
        this.disableColorIndex = disable;
        return this;
    }

    public RenderMaterialBuilder emissive(boolean emissive) {
        this.emissive = emissive;
        return this;
    }

    public RenderMaterialBuilder disableDiffuse(boolean disable) {
        this.disableDiffuse = disable;
        return this;
    }

    public RenderMaterialBuilder ambientOcclusion(TriState ao) {
        this.ambientOcclusion = ao;
        return this;
    }

    public RenderMaterialBuilder glint(TriState glintMode) {
        this.glint = glintMode;
        return this;
    }

    public RenderMaterialBuilder shadeMode(ShadeMode shadeMode) {
        this.shadeMode = shadeMode;
        return this;
    }

    public RenderMaterialBuilder copyFrom(RenderMaterial material) {
        this.blendMode = material.blendMode();
        this.disableColorIndex = material.disableColorIndex();
        this.emissive = material.emissive();
        this.disableDiffuse = material.disableDiffuse();
        this.ambientOcclusion = material.ambientOcclusion();
        this.glint = material.glint();
        this.shadeMode = material.shadeMode();
        return this;
    }

    public RenderMaterialBuilder clear() {
        this.blendMode = BlendMode.DEFAULT;
        this.disableColorIndex = false;
        this.emissive = false;
        this.disableDiffuse = false;
        this.ambientOcclusion = TriState.DEFAULT;
        this.glint = TriState.DEFAULT;
        this.shadeMode = ShadeMode.VANILLA;
        return this;
    }

    public RenderMaterial build() {
        return RenderMaterialRegistry.intern(new RenderMaterial(this.blendMode,
                                                                this.disableColorIndex,
                                                                this.emissive,
                                                                this.disableDiffuse,
                                                                this.ambientOcclusion,
                                                                this.glint,
                                                                this.shadeMode));
    }
}
