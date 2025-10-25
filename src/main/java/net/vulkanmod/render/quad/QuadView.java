package net.vulkanmod.render.quad;

import net.minecraft.core.Direction;
import net.vulkanmod.render.material.RenderMaterial;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Lightweight quad view abstraction used by the new renderer. Derived from the
 * Fabric API interface but pared down to the members actually consumed by
 * VulkanMod.
 */
public interface QuadView {
    int VANILLA_VERTEX_STRIDE = 8;
    int VANILLA_QUAD_STRIDE = VANILLA_VERTEX_STRIDE * 4;

    float x(int vertexIndex);

    float y(int vertexIndex);

    float z(int vertexIndex);

    /**
     * Returns the position component given a vertex index and axis (0 = X, 1 = Y, 2 = Z).
     */
    float posByIndex(int vertexIndex, int coordinateIndex);

    Vector3f copyPos(int vertexIndex, @Nullable Vector3f target);

    int color(int vertexIndex);

    float u(int vertexIndex);

    float v(int vertexIndex);

    Vector2f copyUv(int vertexIndex, @Nullable Vector2f target);

    int lightmap(int vertexIndex);

    boolean hasNormal(int vertexIndex);

    float normalX(int vertexIndex);

    float normalY(int vertexIndex);

    float normalZ(int vertexIndex);

    @Nullable
    Vector3f copyNormal(int vertexIndex, @Nullable Vector3f target);

    @Nullable
    Direction cullFace();

    Direction lightFace();

    @Nullable
    Direction nominalFace();

    Vector3f faceNormal();

    RenderMaterial material();

    int colorIndex();

    int tag();

    void toVanilla(int[] target, int targetIndex);
}
