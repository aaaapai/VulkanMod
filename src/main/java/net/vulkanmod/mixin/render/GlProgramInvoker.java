package net.vulkanmod.mixin.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.opengl.GlProgram;

@Mixin(GlProgram.class)
public interface GlProgramInvoker {
    @Invoker("<init>")
    static GlProgram vulkanmod$create(int programId, String debugLabel) {
        throw new AssertionError();
    }
}