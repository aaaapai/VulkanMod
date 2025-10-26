package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.opengl.GlProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GlProgram.class)
public interface GlProgramInvoker {
    @Invoker("<init>")
    static GlProgram vulkanmod$create(int programId, String debugLabel) {
        throw new AssertionError();
    }
}