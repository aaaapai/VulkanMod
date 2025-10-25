package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.opengl.GlShaderModule;
import net.vulkanmod.interfaces.GlShaderModuleExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GlShaderModule.class, remap = false)
public class GlShaderModuleMixin implements GlShaderModuleExt {
    @Unique
    private String vulkanmod$processedSource;

    @Override
    public void vulkanmod$setProcessedSource(String source) {
        this.vulkanmod$processedSource = source;
    }

    @Override
    public String vulkanmod$getProcessedSource() {
        return this.vulkanmod$processedSource;
    }
}
