package net.vulkanmod.mixin.render.entity.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.vulkanmod.interfaces.ModelPartCubeMixed;
import net.vulkanmod.render.model.CubeModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ModelPart.Cube.class)
public class ModelPartCubeM implements ModelPartCubeMixed {

    @Shadow
    @Final
    public ModelPart.Polygon[] polygons;
    CubeModel cube;

    @Inject(method = "<init>", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/model/geom/ModelPart$Cube;polygons:[Lnet/minecraft/client/model/geom/ModelPart$Polygon;",
            ordinal = 0, shift = At.Shift.AFTER))
    private void getVertices(int i, int j, float f, float g, float h, float k, float l, float m, float n, float o, float p, boolean bl, float q, float r, Set<Direction> set, CallbackInfo ci) {
        CubeModel cube = new CubeModel();
        cube.setVertices(i, j, f, g, h, k, l, m, n, o, p, bl, q, r, set);
        this.cube = cube;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void debug(int i, int j, float f, float g, float h, float k, float l, float m, float n, float o, float p, boolean bl, float q, float r, Set<Direction> set, CallbackInfo ci) {
        // Debug
        CubeModel.Polygon[] polygons = cube.getPolygons();
        for (int i1 = 0; i1 < polygons.length; i1++) {
            var v = polygons[i1].vertices();
            var v2 = this.polygons[i1].vertices();

            for (int s = 0; s < v.length; s++) {
                if (v[s].u() != v2[s].u() || v[s].v() != v2[s].v()) {
                    System.nanoTime();
                }
            }
        }
    }


    @Override
    public CubeModel getCubeModel() {
        return this.cube;
    }
}
