package net.vulkanmod.render.engine;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.vulkan.VK10;

@Environment(EnvType.CLIENT)
//public class VkGpuTexture extends GpuTexture {
public class VkGpuTexture extends GlTexture {
    protected VkGlTexture glTexture;
    protected final int id;
    private final Int2ReferenceMap<VkFbo> fboCache = new Int2ReferenceOpenHashMap<>();
    protected boolean closed;
    protected boolean modesDirty = true;

    protected VkGpuTexture(int usage, String string, TextureFormat textureFormat, int width, int height, int layers, int mipLevel, int id, VkGlTexture glTexture) {
        super(usage, string, textureFormat, width, height, layers, mipLevel, id);
        this.id = id;
        this.glTexture = glTexture;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            GlStateManager._deleteTexture(this.id);

            for (VkFbo fbo : this.fboCache.values()) {
                fbo.close();
            }
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

//    public int getFbo(DirectStateAccess directStateAccess, @Nullable GpuTexture gpuTexture) {
//        int i = gpuTexture == null ? 0 : ((VkGpuTexture)gpuTexture).id;
//        return this.fboCache.computeIfAbsent(i, j -> {
//            int k = directStateAccess.createFrameBufferObject();
//            directStateAccess.bindFrameBufferTextures(k, this.id, i, 0, 0);
//            return k;
//        });
//    }

    public void flushModeChanges() {
        if (this.modesDirty) {
//            GlStateManager._texParameter(3553, 10242, GlConst.toGl(this.addressModeU));
//            GlStateManager._texParameter(3553, 10243, GlConst.toGl(this.addressModeV));
//            switch (this.minFilter) {
//                case NEAREST:
//                    GlStateManager._texParameter(3553, 10241, this.useMipmaps ? 9986 : 9728);
//                    break;
//                case LINEAR:
//                    GlStateManager._texParameter(3553, 10241, this.useMipmaps ? 9987 : 9729);
//            }
//
//            switch (this.magFilter) {
//                case NEAREST:
//                    GlStateManager._texParameter(3553, 10240, 9728);
//                    break;
//                case LINEAR:
//                    GlStateManager._texParameter(3553, 10240, 9729);
//            }

            byte samplerFlags;
            samplerFlags = magFilter == FilterMode.LINEAR ? SamplerManager.LINEAR_FILTERING_BIT : 0;

            // TODO: split min filtering

            if (this.useMipmaps) {
                samplerFlags |= SamplerManager.USE_MIPMAPS_BIT | SamplerManager.MIPMAP_LINEAR_FILTERING_BIT;
            }

            glTexture.getVulkanImage().updateTextureSampler(this.getMipLevels(), samplerFlags);

            this.modesDirty = false;
        }
    }

    public int glId() {
        return this.id;
    }

    @Override
    public void setAddressMode(AddressMode addressMode, AddressMode addressMode2) {
        super.setAddressMode(addressMode, addressMode2);
        this.modesDirty = true;
    }

    @Override
    public void setTextureFilter(FilterMode filterMode, FilterMode filterMode2, boolean bl) {
        super.setTextureFilter(filterMode, filterMode2, bl);
        this.modesDirty = true;
    }

    @Override
    public void setUseMipmaps(boolean bl) {
        super.setUseMipmaps(bl);
        this.modesDirty = true;
    }

    public VkFbo getFbo(@Nullable GpuTexture depthAttachment) {
        int depthAttachmentId = depthAttachment == null ? 0 : ((VkGpuTexture)depthAttachment).id;
        return this.fboCache.computeIfAbsent(depthAttachmentId, j -> new VkFbo(this, (VkGpuTexture) depthAttachment));
    }

    public VulkanImage getVulkanImage() {
        return glTexture.getVulkanImage();
    }

    public static TextureFormat textureFormat(int format) {
        return switch (format) {
            case VK10.VK_FORMAT_R8G8B8A8_UNORM, VK10.VK_FORMAT_B8G8R8A8_UNORM -> TextureFormat.RGBA8;
            case VK10.VK_FORMAT_R8_UNORM -> TextureFormat.RED8;
            case VK10.VK_FORMAT_D32_SFLOAT -> TextureFormat.DEPTH32;
            default -> throw new IllegalStateException("Unexpected value: " + format);
        };
    }

    public static int vkFormat(TextureFormat textureFormat) {
        return switch (textureFormat) {
            case RGBA8 -> VK10.VK_FORMAT_R8G8B8A8_UNORM;
            case RED8 -> VK10.VK_FORMAT_R8_UNORM;
            case RED8I -> VK10.VK_FORMAT_R8_SINT;
            case DEPTH32 -> VK10.VK_FORMAT_D32_SFLOAT;
        };
    }

    public static int vkImageViewType(int usage) {
        int viewType;
        if ((usage & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0) {
            viewType = VK10.VK_IMAGE_VIEW_TYPE_CUBE;
        }
        else {
            viewType = VK10.VK_IMAGE_VIEW_TYPE_2D;
        }

        return viewType;
    }
}

