package net.vulkanmod.mixin.render.accessor;

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice$ShaderCompilationKey", remap = false)
public interface ShaderCompilationKeyAccessor {
    @Accessor("id")
    ResourceLocation vulkanmod$getId();

    @Accessor("type")
    ShaderType vulkanmod$getType();

    @Accessor("defines")
    ShaderDefines vulkanmod$getDefines();
}
