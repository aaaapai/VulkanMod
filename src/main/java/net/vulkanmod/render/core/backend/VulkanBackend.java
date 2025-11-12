package net.vulkanmod.render.core.backend;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.SortedSet;

/**
 * Thin adapter that forwards LevelRenderer lifecycle events into {@link WorldRenderer}.
 * The long-term goal is to let this class own every Vulkan-specific render pass so mixins simply
 * call into it instead of duplicating logic across injection points.
 */
final class VulkanBackend implements RenderBackend {

    private @Nullable WorldRenderer worldRenderer;
    private @Nullable FrameGraphContext currentContext;

    @Override
    public void initialize(LevelRenderer levelRenderer, RenderBuffers renderBuffers) {
        this.worldRenderer = WorldRenderer.init(renderBuffers);
    }

    @Override
    public void setLevel(@Nullable ClientLevel level) {
        if (this.worldRenderer != null) {
            this.worldRenderer.setLevel(level);
        }
    }

    @Override
    public void allChanged() {
        if (this.worldRenderer != null) {
            this.worldRenderer.allChanged();
        }
    }

    @Override
    public void beginFrameGraph(FrameGraphContext context) {
        this.currentContext = context;
        // Subsequent iterations will use this snapshot to enqueue dedicated Vulkan passes.
    }

    @Override
    public void attachFrameGraphBuilder(FrameGraphBuilder builder) {
        if (this.currentContext == null) {
            return;
        }
        this.currentContext = new FrameGraphContext(
                this.currentContext.allocator(),
                this.currentContext.deltaTracker(),
                this.currentContext.renderBlockOutline(),
                this.currentContext.camera(),
                this.currentContext.modelViewMatrix(),
                this.currentContext.projectionMatrix(),
                this.currentContext.cullingProjectionMatrix(),
                this.currentContext.shaderFog(),
                this.currentContext.fogColor(),
                this.currentContext.renderSky(),
                this.currentContext.targets(),
                builder
        );
        this.injectBackendPasses(builder);
    }

    @Override
    public void renderBlockEntities(PoseStack poseStack,
                                    Vec3 cameraPosition,
                                    Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress,
                                    float frameTime,
                                    GpuBufferSlice shaderFog) {
        if (this.worldRenderer == null) {
            return;
        }

        this.worldRenderer.renderBlockEntities(
                poseStack,
                cameraPosition.x(),
                cameraPosition.y(),
                cameraPosition.z(),
                destructionProgress,
                frameTime
        );
    }

    @Override
    public boolean isSectionCompiled(BlockPos blockPos) {
        return this.worldRenderer != null && this.worldRenderer.isSectionCompiled(blockPos);
    }

    @Override
    public void setSectionDirty(int x, int y, int z) {
        if (this.worldRenderer != null) {
            this.worldRenderer.setSectionDirty(x, y, z, true);
        }
    }

    @Override
    public String getSectionStatistics() {
        return this.worldRenderer != null ? this.worldRenderer.getChunkStatistics() : "";
    }

    @Override
    public boolean hasRenderedAllSections() {
        if (this.worldRenderer == null) {
            return true;
        }
        return !this.worldRenderer.graphNeedsUpdate() && this.worldRenderer.getTaskDispatcher().isIdle();
    }

    @Override
    public int countRenderedSections() {
        return this.worldRenderer != null ? this.worldRenderer.getVisibleSectionsCount() : 0;
    }

    private void injectBackendPasses(FrameGraphBuilder builder) {
        if (this.worldRenderer == null || this.currentContext == null) {
            return;
        }

        FramePass uploadPass = builder.addPass("vk_chunk_uploads");
        uploadPass.disableCulling();
        uploadPass.executes(() -> this.worldRenderer.uploadSections());

        LevelTargetBundle targets = this.currentContext.targets();
        if (targets == null) {
            return;
        }

        FramePass chunkPass = builder.addPass("vk_chunk_render");
        chunkPass.requires(uploadPass);
        chunkPass.disableCulling();
        targets.main = chunkPass.readsAndWrites(targets.main);
        if (targets.translucent != null) {
            targets.translucent = chunkPass.readsAndWrites(targets.translucent);
        }
        chunkPass.executes(() -> {
            // TODO: invoke Vulkan chunk rendering once GL path is fully replaced.
        });
    }

    @Nullable
    @Override
    public FrameGraphContext currentContext() {
        return currentContext;
    }
}
