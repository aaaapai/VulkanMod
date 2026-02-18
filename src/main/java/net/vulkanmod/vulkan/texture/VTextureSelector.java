package net.vulkanmod.vulkan.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;

import java.nio.ByteBuffer;

public abstract class VTextureSelector {
    public static final int SIZE = 32;

    private static final VulkanImage[] boundTextures = new VulkanImage[SIZE];

    private static final int[] levels = new int[SIZE];

    private static final VulkanImage whiteTexture = VulkanImage.createWhiteTexture();

    private static int activeTexture = 0;

    public static void bindTexture(VulkanImage texture) {
        boundTextures[0] = texture;
    }

    public static void bindTexture(int i, VulkanImage texture) {
        if(i < 0 || i >= SIZE) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", i, SIZE - 1));
            return;
        }

        boundTextures[i] = texture;
        levels[i] = -1;
    }

    public static void bindImage(int i, VulkanImage texture, int level) {
        if(i < 0 || i > 7) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", i, SIZE - 1));
            return;
        }

        boundTextures[i] = texture;
        levels[i] = level;
    }

    public static void uploadSubTexture(int mipLevel, int width, int height, int xOffset, int yOffset, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        VulkanImage texture = boundTextures[activeTexture];

        if(texture == null)
            throw new NullPointerException("Texture is null at index: " + activeTexture);

        texture.uploadSubTextureAsync(mipLevel, width, height, xOffset, yOffset, unpackSkipRows, unpackSkipPixels, unpackRowLength, buffer);
    }

    public static int getTextureIdx(String name) {
        if (name.startsWith("Sampler")) {
            String numStr = name.substring(7);
            try {
                int idx = Integer.parseInt(numStr);
                if (idx >= 0 && idx < SIZE) return idx;
            } catch (NumberFormatException ignored) {}
        }
        return switch (name) {
            case "DiffuseSampler" -> 0;
            default -> throw new IllegalStateException("Unknown sampler name: " + name);
        };
    }

    public static void bindShaderTextures(Pipeline pipeline) {
        var imageDescriptors = pipeline.getImageDescriptors();

        for (ImageDescriptor state : imageDescriptors) {
            int idx = state.imageIdx;

            // RenderSystem.shaderTextures[] is only 12 elements (vanilla MC limit).
            // For indices >= 12 (used by shader mods like Iris), skip the RenderSystem
            // lookup and use whatever texture is already bound in VTextureSelector
            // (Iris binds its textures via GlStateManager._bindTexture before draw).
            if (idx >= 12) {
                // If nothing is bound yet at this index, bind a fallback
                if (idx < SIZE && boundTextures[idx] == null) {
                    GlTexture fallback = GlTexture.getTexture(MissingTextureAtlasSprite.getTexture().getId());
                    if (fallback != null && fallback.getVulkanImage() != null) {
                        boundTextures[idx] = fallback.getVulkanImage();
                    }
                }
                continue;
            }

            final int shaderTexture = RenderSystem.getShaderTexture(idx);

            // If RenderSystem has no texture set (returns 0) and we already have a
            // texture bound at this index (e.g. from Iris), keep the existing binding
            if (shaderTexture == 0 && idx < SIZE && boundTextures[idx] != null) {
                continue;
            }

            GlTexture texture = GlTexture.getTexture(shaderTexture);

            if (texture != null && texture.getVulkanImage() != null) {
                VTextureSelector.bindTexture(idx, texture.getVulkanImage());
            }
            else {
                 texture = GlTexture.getTexture(MissingTextureAtlasSprite.getTexture().getId());
                VTextureSelector.bindTexture(idx, texture.getVulkanImage());
            }
        }
    }

    public static VulkanImage getImage(int i) {
        return boundTextures[i];
    }

    public static void setLightTexture(VulkanImage texture) {
        boundTextures[2] = texture;
    }

    public static void setOverlayTexture(VulkanImage texture) {
        boundTextures[1] = texture;
    }

    public static void setActiveTexture(int activeTexture) {
        if(activeTexture < 0 || activeTexture >= SIZE) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", activeTexture, SIZE - 1));
        }

        VTextureSelector.activeTexture = activeTexture;
    }

    public static VulkanImage getBoundTexture(int i) { return boundTextures[i]; }

    public static VulkanImage getWhiteTexture() { return whiteTexture; }
}
