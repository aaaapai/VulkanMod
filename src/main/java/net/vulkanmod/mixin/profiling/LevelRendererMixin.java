package net.vulkanmod.mixin.profiling;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.vulkanmod.render.core.backend.BackendManager;
import net.vulkanmod.render.core.backend.FrameGraphContext;
import net.vulkanmod.render.profiling.Profiler;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow @Final private LevelTargetBundle targets;

    @Unique
    private static final Map<String, String> VK$PASS_LABELS = Map.of(
            "clear", "Clear",
            "sky", "Sky",
            "main", "Geometry",
            "particles", "Particles",
            "clouds", "Clouds",
            "weather", "Weather",
            "late_debug", "Debug"
    );

    @Inject(method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            at = @At("HEAD"))
    private void vk$pushRenderLevel(GraphicsResourceAllocator graphicsResourceAllocator,
                                    DeltaTracker deltaTracker,
                                    boolean renderBlockOutline,
                                    Camera camera,
                                    Matrix4f frustumMatrix,
                                    Matrix4f projectionMatrix,
                                    Matrix4f cullingProjectionMatrix,
                                    GpuBufferSlice shaderFog,
                                    Vector4f fogColor,
                                    boolean renderSky,
                                    CallbackInfo ci) {
        Profiler.getMainProfiler().push("LevelRenderer");
        BackendManager.get().beginFrameGraph(new FrameGraphContext(
                graphicsResourceAllocator,
                deltaTracker,
                renderBlockOutline,
                camera,
                frustumMatrix,
                projectionMatrix,
                cullingProjectionMatrix,
                shaderFog,
                fogColor,
                renderSky,
                this.targets,
                null
        ));
    }

    @Inject(method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            at = @At("RETURN"))
    private void vk$popRenderLevel(GraphicsResourceAllocator graphicsResourceAllocator,
                                   DeltaTracker deltaTracker,
                                   boolean renderBlockOutline,
                                   Camera camera,
                                   Matrix4f frustumMatrix,
                                   Matrix4f projectionMatrix,
                                   Matrix4f cullingProjectionMatrix,
                                   GpuBufferSlice shaderFog,
                                   Vector4f fogColor,
                                   boolean renderSky,
                                   CallbackInfo ci) {
        Profiler.getMainProfiler().pop();
    }

    @ModifyArg(method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V"),
            index = 1)
    private FrameGraphBuilder.Inspector vk$wrapInspector(FrameGraphBuilder.Inspector inspector) {
        Profiler profiler = Profiler.getMainProfiler();
        return new FrameGraphBuilder.Inspector() {
            @Override
            public void beforeExecutePass(String name) {
                if (inspector != null) {
                    inspector.beforeExecutePass(name);
                }
                profiler.push(VK$PASS_LABELS.getOrDefault(name, name));
            }

            @Override
            public void afterExecutePass(String name) {
                profiler.pop();
                if (inspector != null) {
                    inspector.afterExecutePass(name);
                }
            }
        };
    }

    @ModifyVariable(method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;<init>()V", shift = At.Shift.AFTER),
            ordinal = 0)
    private FrameGraphBuilder vk$captureBuilder(FrameGraphBuilder builder) {
        BackendManager.get().attachFrameGraphBuilder(builder);
        return builder;
    }
}
