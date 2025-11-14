package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.render.engine.*;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Mixin(PostPass.class)
public abstract class PostPassM {
    @Shadow @Final private String name;
    @Shadow @Final private List<PostPass.Input> inputs;
    @Shadow @Final private ResourceLocation outputTargetId;
    @Shadow @Final private RenderPipeline pipeline;
    @Shadow @Final private MappableRingBuffer infoUbo;
    @Shadow @Final private Map<String, GpuBuffer> customUniforms;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void addToFrame(FrameGraphBuilder frameGraphBuilder, Map<ResourceLocation, ResourceHandle<RenderTarget>> map, GpuBufferSlice gpuBufferSlice) {
        FramePass framePass = frameGraphBuilder.addPass(this.name);

        for (PostPass.Input input : this.inputs) {
            input.addToPass(framePass, map);
        }

        ResourceHandle<RenderTarget> resourceHandle = (ResourceHandle<RenderTarget>)map.computeIfPresent(
                this.outputTargetId, (resourceLocation, resourceHandlex) -> framePass.readsAndWrites(resourceHandlex)
        );
        if (resourceHandle == null) {
            throw new IllegalStateException("Missing handle for target " + this.outputTargetId);
        } else {
            framePass.executes(
                    () -> {
                        RenderTarget renderTarget = resourceHandle.get();
                        RenderSystem.backupProjectionMatrix();
                        RenderSystem.setProjectionMatrix(gpuBufferSlice, ProjectionType.ORTHOGRAPHIC);
                        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                        List<Pair<String, GpuTextureView>> list = this.inputs.stream().map(inputxx -> Pair.of(inputxx.samplerName(), inputxx.texture(map))).toList();

                        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(this.infoUbo.currentBuffer(), false, true)) {
                            Std140Builder std140Builder = Std140Builder.intoBuffer(mappedView.data());
                            std140Builder.putVec2(renderTarget.width, renderTarget.height);

                            for (Pair<String, GpuTextureView> pair : list) {
                                std140Builder.putVec2(pair.getSecond().getWidth(0), pair.getSecond().getHeight(0));
                            }
                        }

                        Renderer.getInstance().endRenderPass();

                        for (var input : this.inputs) {
                            VkGpuTexture gpuTexture = (VkGpuTexture) input.texture(map).texture();
                            gpuTexture.getVulkanImage().readOnlyLayout();
                        }

                        try (RenderPass renderPass = commandEncoder.createRenderPass(
                                () -> "Post pass " + this.name,
                                renderTarget.getColorTextureView(),
                                OptionalInt.empty(),
                                renderTarget.useDepth ? renderTarget.getDepthTextureView() : null,
                                OptionalDouble.empty()
                        )) {
                            renderPass.setPipeline(this.pipeline);
                            RenderSystem.bindDefaultUniforms(renderPass);
                            renderPass.setUniform("SamplerInfo", this.infoUbo.currentBuffer());

                            for (Map.Entry<String, GpuBuffer> entry : this.customUniforms.entrySet()) {
                                renderPass.setUniform((String)entry.getKey(), (GpuBuffer)entry.getValue());
                            }

                            for (Pair<String, GpuTextureView> pair2 : list) {
                                renderPass.bindSampler(pair2.getFirst() + "Sampler", pair2.getSecond());
                            }

                            renderPass.draw(0, 3);
                        }

                        this.infoUbo.rotate();
                        RenderSystem.restoreProjectionMatrix();

                        for (PostPass.Input inputx : this.inputs) {
                            inputx.cleanup(map);
                        }
                    }
            );
        }
    }

}
