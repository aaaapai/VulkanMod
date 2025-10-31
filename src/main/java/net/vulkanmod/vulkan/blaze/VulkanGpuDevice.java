package net.vulkanmod.vulkan.blaze;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8_SINT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;

/**
 * WIP Vulkan-backed replacement for Mojang's {@link com.mojang.blaze3d.opengl.GlDevice}.
 * At this stage the implementation only satisfies the type contracts so we can
 * wire it into RenderSystem; concrete behaviour will be filled in incrementally.
 */
public final class VulkanGpuDevice implements GpuDevice {

    private final long windowHandle;
    private final int debugVerbosity;
    private final boolean debugSync;
    private final boolean debugLabels;
    private final BiFunction<ResourceLocation, ShaderType, String> shaderSourceProvider;

    public VulkanGpuDevice(long windowHandle,
                           int debugVerbosity,
                           boolean debugSync,
                           BiFunction<ResourceLocation, ShaderType, String> shaderSourceProvider,
                           boolean debugLabels) {
        this.windowHandle = windowHandle;
        this.debugVerbosity = debugVerbosity;
        this.debugSync = debugSync;
        this.shaderSourceProvider = shaderSourceProvider;
        this.debugLabels = debugLabels;

        // Ensure Vulkan runtime is initialised. Renderer boot happens elsewhere,
        // but we force creation of the singleton so later calls don't explode.
        Renderer.getInstance();
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public boolean isDebugSyncEnabled() {
        return debugSync;
    }

    public boolean isDebugLabelsEnabled() {
        return debugLabels;
    }

    public BiFunction<ResourceLocation, ShaderType, String> shaderSourceProvider() {
        return shaderSourceProvider;
    }

    @Override
    public CommandEncoder createCommandEncoder() {
        return new VulkanCommandEncoder(this);
    }

    @Override
    public GpuTexture createTexture(java.util.function.Supplier<String> label,
                                    int usage,
                                    TextureFormat format,
                                    int width,
                                    int height,
                                    int depthOrLayers,
                                    int mipLevels) {
        String name = label != null ? label.get() : "unnamed";
        return createTextureInternal(name, usage, format, width, height, depthOrLayers, mipLevels);
    }

    @Override
    public GpuTexture createTexture(String label,
                                    int usage,
                                    TextureFormat format,
                                    int width,
                                    int height,
                                    int depthOrLayers,
                                    int mipLevels) {
        String name = Objects.requireNonNullElse(label, "unnamed");
        return createTextureInternal(name, usage, format, width, height, depthOrLayers, mipLevels);
    }

    private VulkanGpuTexture createTextureInternal(String label,
                                                   int usage,
                                                   TextureFormat format,
                                                   int width,
                                                   int height,
                                                   int depthOrLayers,
                                                   int mipLevels) {
        if (depthOrLayers > 1) {
            throw new UnsupportedOperationException("Array/3D textures are not implemented yet");
        }

        VulkanGpuTexture texture = new VulkanGpuTexture(this, usage, label, format, width, height, depthOrLayers, mipLevels);

        int vkFormat = mapFormat(format);
        int vkUsage = resolveUsage(usage, format);

        VulkanImage image = VulkanImage.builder(width, height)
            .setName(label)
            .setFormat(vkFormat)
            .setMipLevels(mipLevels)
            .setUsage(vkUsage)
            .createVulkanImage();

        texture.attachImage(image);
        texture.setUseMipmaps(mipLevels > 1);
        return texture;
    }

    private static int mapFormat(TextureFormat format) {
        return switch (format) {
            case RGBA8 -> VK_FORMAT_R8G8B8A8_UNORM;
            case RED8 -> VK_FORMAT_R8_UNORM;
            case RED8I -> VK_FORMAT_R8_SINT;
            case DEPTH32 -> VK_FORMAT_D32_SFLOAT;
        };
    }

    private static int resolveUsage(int textureUsage, TextureFormat format) {
        int flags = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT;

        if ((textureUsage & GpuTexture.USAGE_TEXTURE_BINDING) != 0) {
            flags |= VK_IMAGE_USAGE_SAMPLED_BIT;
        }
        if ((textureUsage & GpuTexture.USAGE_RENDER_ATTACHMENT) != 0) {
            if (format.hasDepthAspect()) {
                flags |= VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
            } else {
                flags |= VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
            }
        }

        return flags;
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture) {
        return new VulkanGpuTextureView((VulkanGpuTexture) texture, 0, texture.getMipLevels());
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int levelCount) {
        return new VulkanGpuTextureView((VulkanGpuTexture) texture, baseMipLevel, levelCount);
    }

    @Override
    public VulkanGpuBuffer createBuffer(java.util.function.Supplier<String> label,
                                        int usage,
                                        int size) {
        String name = label != null ? label.get() : "unnamed-buffer";
        return new VulkanGpuBuffer(this, name, usage, size);
    }

    @Override
    public VulkanGpuBuffer createBuffer(java.util.function.Supplier<String> label,
                                        int usage,
                                        java.nio.ByteBuffer initialData) {
        String name = label != null ? label.get() : "unnamed-buffer";
        VulkanGpuBuffer buffer = new VulkanGpuBuffer(this, name, usage, initialData.remaining());
        buffer.upload(initialData);
        return buffer;
    }

    @Override
    public String getImplementationInformation() {
        return "VulkanMod Vulkan backend (WIP)";
    }

    @Override
    public List<String> getLastDebugMessages() {
        return Collections.emptyList();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return debugVerbosity > 0 || debugSync || debugLabels;
    }

    @Override
    public String getVendor() {
        return DeviceManager.deviceProperties != null
            ? Integer.toUnsignedString(DeviceManager.deviceProperties.vendorID(), 16)
            : "unknown";
    }

    @Override
    public String getBackendName() {
        return "Vulkan";
    }

    @Override
    public String getVersion() {
        return "1.2";
    }

    @Override
    public String getRenderer() {
        return DeviceManager.deviceProperties != null
            ? DeviceManager.deviceProperties.deviceNameString()
            : "unknown";
    }

    @Override
    public int getMaxTextureSize() {
        return DeviceManager.deviceProperties != null
            ? DeviceManager.deviceProperties.limits().maxImageDimension2D()
            : 0;
    }

    @Override
    public int getUniformOffsetAlignment() {
        return DeviceManager.deviceProperties != null
            ? (int) DeviceManager.deviceProperties.limits().minUniformBufferOffsetAlignment()
            : 256;
    }

    @Override
    public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline) {
        throw new UnsupportedOperationException("precompilePipeline is not implemented yet");
    }

    @Override
    public CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline,
                                                     BiFunction<ResourceLocation, ShaderType, String> shaderSourceGetter) {
        throw new UnsupportedOperationException("precompilePipeline is not implemented yet");
    }

    @Override
    public void clearPipelineCache() {
        // TODO: integrate with PipelineManager once the Vulkan pipeline cache is hooked up.
    }

    @Override
    public List<String> getEnabledExtensions() {
        return Collections.emptyList();
    }

    @Override
    public void close() {
        Renderer.getInstance().cleanUpResources();
    }
}
