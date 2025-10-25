package net.vulkanmod.mixin.matrix;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(PoseStack.Pose.class)
public interface PoseAccessor {

    @Accessor("trustedNormals")
    boolean trustedNormals();
}