package net.vulkanmod.render.core.backend;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.SortedSet;

/**
 * Defines the contract between Minecraft's high-level rendering structures and our Vulkan backend.
 * Centralizing these hooks keeps mixins thin and relocates stateful logic here.
 */
public interface RenderBackend {

    /**
     * Called once the {@link LevelRenderer} finishes construction so the backend can bind to shared buffers.
     */
    void initialize(LevelRenderer levelRenderer, RenderBuffers renderBuffers);

    /**
     * Propagates level changes so chunk caches and staging buffers can reset.
     */
    void setLevel(@Nullable ClientLevel level);

    /**
     * Mirrors {@link LevelRenderer#allChanged()} to flush region caches and rebuild graphs.
     */
    void allChanged();

    /**
     * Gives the backend an opportunity to populate and execute its own passes before the vanilla frame graph runs.
     */
    void beginFrameGraph(FrameGraphContext context);

    /**
     * Provides the freshly created {@link FrameGraphBuilder} so the backend can register passes.
     */
    void attachFrameGraphBuilder(FrameGraphBuilder builder);

    /**
     * Exposes the latest frame context for subsystems that need camera matrices.
     */
    @Nullable
    FrameGraphContext currentContext();

    /**
     * Renders block entities and block breaking overlays via the Vulkan renderer.
     */
    void renderBlockEntities(PoseStack poseStack,
                             Vec3 cameraPosition,
                             Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress,
                             float frameTime,
                             GpuBufferSlice shaderFog);

    boolean isSectionCompiled(BlockPos blockPos);

    void setSectionDirty(int x, int y, int z);

    String getSectionStatistics();

    boolean hasRenderedAllSections();

    int countRenderedSections();
}
