package net.vulkanmod.render.chunk.build.frapi.mesh;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;

public interface ExtendedQuadEmitter {

    static ExtendedQuadEmitter of(QuadEmitter quadEmitter) {
        return (ExtendedQuadEmitter) quadEmitter;
    }

//    default void emitBlockQuads(QuadEmitter emitter, BakedModel model, BlockState state, Supplier<RandomSource> randomSupplier, Predicate<@Nullable Direction> cullTest) {
//        VanillaModelEncoder.emitBlockQuads(emitter, model, state, randomSupplier, cullTest);
//    }
//
//    default void emitItemQuads(QuadEmitter emitter,BakedModel model, BlockState state, Supplier<RandomSource> randomSupplier) {
//        VanillaModelEncoder.emitItemQuads(emitter, model, state, randomSupplier);
//    }
}
