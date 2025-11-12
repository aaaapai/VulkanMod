package net.vulkanmod.mixin.render.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleM extends Particle {

    @Shadow
    protected float quadSize;
    @Shadow
    protected float rCol;
    @Shadow
    protected float gCol;
    @Shadow
    protected float bCol;
    @Shadow
    protected float alpha;
    @Shadow
    protected float roll;
    @Shadow
    protected float oRoll;

    protected SingleQuadParticleM(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
        super(clientLevel, d, e, f, g, h, i);
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
    }

    @Shadow
    protected abstract float getU0();

    @Shadow
    protected abstract float getU1();

    @Shadow
    protected abstract float getV0();

    @Shadow
    protected abstract float getV1();

    @Shadow
    public abstract float getQuadSize(float f);

    @Shadow
    public abstract SingleQuadParticle.FacingCameraMode getFacingCameraMode();

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V", at = @At("HEAD"), cancellable = true)
    private void vulkanmod$cullParticles(QuadParticleRenderState state, Camera camera, float partialTicks, CallbackInfo ci) {
        double lx = Mth.lerp(partialTicks, this.xo, this.x);
        double ly = Mth.lerp(partialTicks, this.yo, this.y);
        double lz = Mth.lerp(partialTicks, this.zo, this.z);

        if (cull(WorldRenderer.getInstance(), lx, ly, lz)) {
            ci.cancel();
        }
    }

    protected int getLightColor(float f) {
        BlockPos blockPos = BlockPos.containing(this.x, this.y, this.z);
        return this.level.hasChunkAt(blockPos) ? LevelRenderer.getLightColor(this.level, blockPos) : 0;
    }

    private boolean cull(WorldRenderer worldRenderer, double x, double y, double z) {
        RenderSection section = worldRenderer.getSectionGrid().getSectionAtBlockPos((int) x, (int) y, (int) z);
        return section != null && section.getLastFrame() != worldRenderer.getLastFrame();
    }

    /**
     * @author
     * @reason Prevent vanilla from queuing GL particle batches once we skip drawing via {@link #vulkanmod$cullParticles}.
     */
    @Overwrite
    public ParticleRenderType getGroup() {
        return ParticleRenderType.NO_RENDER;
    }
}
