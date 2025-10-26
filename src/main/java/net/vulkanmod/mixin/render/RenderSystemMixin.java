package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.function.BiFunction;

@Mixin(value = RenderSystem.class, remap = false)
public abstract class RenderSystemMixin {

    @Shadow
    @Final
    private static Matrix4fStack modelViewStack;
    @Shadow
    private static Matrix4f textureMatrix;
    @Shadow
    private static @Nullable Thread renderThread;
    @Shadow
    @Nullable
    private static GpuBufferSlice shaderFog;
    @Shadow
    @Nullable
    private static GpuBufferSlice shaderLightDirections;
    @Shadow
    @Nullable
    private static GpuBufferSlice projectionMatrixBuffer;
    @Shadow
    private static ProjectionType projectionType;
    @Shadow
    private static ProjectionType savedProjectionType;
    @Shadow
    @Nullable
    private static GpuBufferSlice savedProjectionMatrixBuffer;

    @Shadow
    public static void assertOnRenderThread() {
    }

    @Inject(method = "initRenderer(JIZLjava/util/function/BiFunction;Z)V", at = @At("TAIL"))
    private static void hookInitRenderer(long window, int debugVerbosity, boolean debugSync,
                                         BiFunction<ResourceLocation, com.mojang.blaze3d.shaders.ShaderType, String> shaderSourceGetter,
                                         boolean enableDebugLabels, CallbackInfo ci) {
        VRenderSystem.setWindow(window);
        VRenderSystem.initRenderer();

        if (renderThread != null) {
            renderThread.setPriority(Thread.NORM_PRIORITY + 2);
        }
    }

    @Overwrite(remap = false)
    public static void setupDefaultState() {
    }

    @Inject(method = "setProjectionMatrix", at = @At("TAIL"))
    private static void captureProjection(GpuBufferSlice buffer, ProjectionType type, CallbackInfo ci) {
        updateProjection(buffer);
    }

    @Inject(method = "restoreProjectionMatrix", at = @At("TAIL"))
    private static void captureRestoredProjection(CallbackInfo ci) {
        if (projectionMatrixBuffer != null) {
            updateProjection(projectionMatrixBuffer);
        }
    }

    @Inject(method = "setShaderFog", at = @At("TAIL"))
    private static void captureFog(GpuBufferSlice buffer, CallbackInfo ci) {
        shaderFog = buffer;
    }

    @Inject(method = "setShaderLights", at = @At("TAIL"))
    private static void captureLights(GpuBufferSlice buffer, CallbackInfo ci) {
        shaderLightDirections = buffer;
        if (buffer != null) {
            updateLights(buffer);
        }
    }

    @Inject(method = "setTextureMatrix", at = @At("TAIL"))
    private static void propagateTextureMatrix(Matrix4f matrix, CallbackInfo ci) {
        VRenderSystem.setTextureMatrix(matrix);
    }

    @Inject(method = "resetTextureMatrix", at = @At("TAIL"))
    private static void propagateTextureReset(CallbackInfo ci) {
        VRenderSystem.setTextureMatrix(textureMatrix);
    }

    @Unique
    private static void updateProjection(GpuBufferSlice slice) {
        try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(slice, true, false)) {
            FloatBuffer buffer = view.data().order(ByteOrder.nativeOrder()).asFloatBuffer();
            FloatBuffer copy = MemoryUtil.memAllocFloat(16);
            try {
                for (int i = 0; i < 16; i++) {
                    copy.put(i, buffer.get(i));
                }
                copy.position(0);
                Matrix4f matrix = new Matrix4f();
                matrix.set(copy);
                VRenderSystem.applyProjectionMatrix(matrix);
                VRenderSystem.calculateMVP();
            } finally {
                MemoryUtil.memFree(copy);
            }
        }
    }

    @Unique
    private static void updateLights(GpuBufferSlice slice) {
        try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(slice, true, false)) {
            FloatBuffer buffer = view.data().order(ByteOrder.nativeOrder()).asFloatBuffer();
            float lx0 = buffer.get(0);
            float ly0 = buffer.get(1);
            float lz0 = buffer.get(2);
            float lx1 = buffer.get(4);
            float ly1 = buffer.get(5);
            float lz1 = buffer.get(6);
            VRenderSystem.setShaderLights(lx0, ly0, lz0, lx1, ly1, lz1);
        }
    }
}
