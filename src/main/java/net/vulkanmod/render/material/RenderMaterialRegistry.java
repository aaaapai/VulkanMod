package net.vulkanmod.render.material;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.vulkanmod.util.TriState;

/**
 * Central registry for {@link RenderMaterial} instances. Quads encode the
 * registry index to avoid storing large structures per vertex.
 */
public final class RenderMaterialRegistry {
    public static final RenderMaterial STANDARD_MATERIAL;
    public static final RenderMaterial NO_AO_MATERIAL;
    private static final Int2ObjectMap<RenderMaterial> BY_ID = new Int2ObjectArrayMap<>();
    private static final Object2IntMap<RenderMaterial> TO_ID = new Object2IntOpenHashMap<>();
    private static int nextId;

    static {
        TO_ID.defaultReturnValue(-1);

        STANDARD_MATERIAL = intern(new RenderMaterial(BlendMode.DEFAULT,
                false,
                false,
                false,
                TriState.DEFAULT,
                TriState.DEFAULT,
                ShadeMode.VANILLA));

        NO_AO_MATERIAL = intern(new RenderMaterial(BlendMode.DEFAULT,
                false,
                false,
                false,
                TriState.FALSE,
                TriState.DEFAULT,
                ShadeMode.VANILLA));
    }

    private RenderMaterialRegistry() {
    }

    public static synchronized RenderMaterial intern(RenderMaterial material) {
        int id = TO_ID.getInt(material);
        if (id != -1) {
            return BY_ID.get(id);
        }

        id = nextId++;
        TO_ID.put(material, id);
        BY_ID.put(id, material);
        return material;
    }

    public static synchronized int getId(RenderMaterial material) {
        int id = TO_ID.getInt(material);
        if (id == -1) {
            id = nextId++;
            TO_ID.put(material, id);
            BY_ID.put(id, material);
        }
        return id;
    }

    public static RenderMaterial fromId(int id) {
        RenderMaterial material = BY_ID.get(id);
        if (material == null) {
            throw new IllegalStateException("Unknown material id " + id);
        }

        return material;
    }

    public static RenderMaterial disableDiffuse(RenderMaterial material, boolean disable) {
        return material.withDisableDiffuse(disable);
    }
}
