package net.vulkanmod.render.core.backend;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelTargetBundle;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of the data Mojang provides while building a frame.
 * The backend can inspect or fork the {@link FrameGraphBuilder} to insert Vulkan-only passes.
 */
public record FrameGraphContext(GraphicsResourceAllocator allocator,
                                DeltaTracker deltaTracker,
                                boolean renderBlockOutline,
                                Camera camera,
                                Matrix4f modelViewMatrix,
                                Matrix4f projectionMatrix,
                                Matrix4f cullingProjectionMatrix,
                                GpuBufferSlice shaderFog,
                                Vector4f fogColor,
                                boolean renderSky,
                                LevelTargetBundle targets,
                                @Nullable FrameGraphBuilder builder) {
}
