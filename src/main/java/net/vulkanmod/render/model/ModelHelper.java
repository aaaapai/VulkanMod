package net.vulkanmod.render.model;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Simple helper mirroring the handful of face-index utilities that were
 * previously provided by Fabric's ModelHelper.
 */
public final class ModelHelper {
    public static final int NULL_FACE_ID = Direction.values().length;

    private static final Direction[] DIRECTIONS = Direction.values();

    private ModelHelper() {
    }

    public static int toFaceIndex(@Nullable Direction face) {
        return face == null ? NULL_FACE_ID : face.get3DDataValue();
    }

    @Nullable
    public static Direction faceFromIndex(int index) {
        if (index < 0 || index >= NULL_FACE_ID) {
            return null;
        }

        return DIRECTIONS[index];
    }
}
