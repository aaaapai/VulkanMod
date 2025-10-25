package net.vulkanmod.render.quad;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.vulkanmod.render.material.RenderMaterial;
import org.jetbrains.annotations.Nullable;

public interface MutableQuadView extends QuadView {
    int BAKE_LOCK_UV = 1;
    int BAKE_NORMALIZED = 1 << 1;
    int BAKE_FLIP_U = 1 << 2;
    int BAKE_FLIP_V = 1 << 3;

    MutableQuadView pos(int vertexIndex, float x, float y, float z);

    MutableQuadView color(int vertexIndex, int color);

    MutableQuadView uv(int vertexIndex, float u, float v);

    MutableQuadView spriteBake(TextureAtlasSprite sprite, int bakeFlags);

    MutableQuadView lightmap(int vertexIndex, int lightmap);

    MutableQuadView normal(int vertexIndex, float x, float y, float z);

    MutableQuadView cullFace(@Nullable Direction face);

    MutableQuadView nominalFace(@Nullable Direction face);

    MutableQuadView material(RenderMaterial material);

    MutableQuadView colorIndex(int colorIndex);

    MutableQuadView tag(int tag);

    MutableQuadView copyFrom(QuadView quad);

    MutableQuadView fromVanilla(int[] quadData, int startIndex);

    MutableQuadView fromVanilla(BakedQuad quad, RenderMaterial material, @Nullable Direction cullFace);

    MutableQuadView emit();
}
