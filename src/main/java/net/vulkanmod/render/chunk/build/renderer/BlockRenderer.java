package net.vulkanmod.render.chunk.build.renderer;

import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.color.BlockColorsExtended;
import net.vulkanmod.render.chunk.build.RenderRegion;
import net.vulkanmod.render.chunk.build.light.LightPipeline;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.chunk.build.pipeline.helper.ColorHelper;
import net.vulkanmod.render.chunk.build.pipeline.mesh.EncodingFormat;
import net.vulkanmod.render.chunk.build.pipeline.mesh.MutableQuadViewImpl;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.chunk.cull.QuadFacing;
import net.vulkanmod.render.material.RenderMaterial;
import net.vulkanmod.render.material.RenderMaterialRegistry;
import net.vulkanmod.render.model.quad.ModelQuadView;
import net.vulkanmod.render.model.quad.QuadUtils;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.render.vertex.format.I32_SNorm;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class BlockRenderer {
    private static final Direction[] DIRECTIONS = Direction.values();

    private final MutableQuadViewImpl editorQuad = new MutableQuadViewImpl() {
        {
            data = new int[EncodingFormat.TOTAL_STRIDE];
            clear();
        }

        @Override
        public void emitDirectly() {
            // not used – quads are consumed immediately
        }
    };

    private final Object2ByteLinkedOpenHashMap<OcclusionKey> occlusionCache = new Object2ByteLinkedOpenHashMap<>(2048, 0.25F) {
        @Override
        protected void rehash(int i) {
        }
    };
    private final OcclusionKey lookupKey = new OcclusionKey();

    private final RandomSource random = RandomSource.create();
    private final QuadLightData quadLightData = new QuadLightData();
    private final LightPipeline flatLightPipeline;
    private final LightPipeline smoothLightPipeline;
    private final net.vulkanmod.render.chunk.build.color.BlockColorRegistry blockColorRegistry;

    private BuilderResources resources;
    private BlockAndTintGetter renderRegion;
    private boolean enableCulling = true;

    private BlockState blockState;
    private BlockPos blockPos;
    private boolean useAO;

    private long seed;
    private TerrainRenderType renderType;
    private TerrainBuilder terrainBuilder;
    private Vector3f currentPos;

    private final boolean backFaceCulling = Initializer.CONFIG.backFaceCulling;

    public BlockRenderer(LightPipeline flatLightPipeline, LightPipeline smoothLightPipeline) {
        this.flatLightPipeline = flatLightPipeline;
        this.smoothLightPipeline = smoothLightPipeline;
        this.occlusionCache.defaultReturnValue((byte) 127);
        this.blockColorRegistry = BlockColorsExtended.from(Minecraft.getInstance().getBlockColors()).getColorResolverMap();
    }

    public void setResources(BuilderResources resources) {
        this.resources = resources;
    }

    public void prepareForWorld(RenderRegion region, boolean enableCulling) {
        this.renderRegion = region;
        this.enableCulling = enableCulling;
    }

    public void renderBlock(BlockState blockState, BlockPos blockPos, Vector3f pos) {
        if (this.renderRegion == null) {
            return;
        }

        this.blockState = blockState;
        this.blockPos = blockPos;
        this.currentPos = pos;
        this.seed = blockState.getSeed(blockPos);
        this.random.setSeed(this.seed);
        this.useAO = Minecraft.useAmbientOcclusion() && blockState.getLightEmission() == 0;

        TerrainRenderType baseType = TerrainRenderType.get(ItemBlockRenderTypes.getChunkRenderType(blockState));
        baseType = TerrainRenderType.getRemapped(baseType);
        this.renderType = baseType;
        this.terrainBuilder = this.resources.builderPack.builder(baseType);
        this.terrainBuilder.setBlockAttributes(blockState);

        BlockAndTintGetter region = this.renderRegion;
        Vec3 offset = blockState.getOffset(region, blockPos);
        pos.add((float) offset.x, (float) offset.y, (float) offset.z);

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockStateModel stateModel = dispatcher.getBlockModel(blockState);

        this.cullCompletionFlags = 0;
        this.cullResultFlags = 0;

        List<BlockModelPart> parts = new ObjectArrayList<>();
        stateModel.collectParts(this.random, parts);

        for (BlockModelPart part : parts) {
            boolean partUsesAo = part.useAmbientOcclusion();
            RenderMaterial baseMaterial = partUsesAo ? RenderMaterialRegistry.STANDARD_MATERIAL : RenderMaterialRegistry.NO_AO_MATERIAL;
            emitQuads(part.getQuads(null), baseMaterial, partUsesAo, null);

            for (Direction direction : DIRECTIONS) {
                if (!shouldRenderFace(direction)) {
                    continue;
                }

                emitQuads(part.getQuads(direction), baseMaterial, partUsesAo, direction);
            }
        }
    }

    private void emitQuads(List<BakedQuad> quads, RenderMaterial baseMaterial, boolean partUsesAo, @Nullable Direction cullFace) {
        if (quads == null || quads.isEmpty()) {
            return;
        }

        for (BakedQuad bakedQuad : quads) {
            renderQuad(bakedQuad, baseMaterial, partUsesAo, cullFace);
        }
    }

    private void renderQuad(BakedQuad bakedQuad, RenderMaterial baseMaterial, boolean partUsesAo, @Nullable Direction cullFace) {
        MutableQuadViewImpl quad = this.editorQuad;
        quad.clear();

        RenderMaterial material = bakedQuad.shade()
            ? baseMaterial
            : RenderMaterialRegistry.disableDiffuse(baseMaterial, true);

        quad.fromVanilla(bakedQuad, material, cullFace);

        colorizeQuad(quad, quad.colorIndex());

        boolean emissive = bakedQuad.lightEmission() > 0;
        boolean applyAo = this.useAO && partUsesAo;
        LightPipeline pipeline = applyAo ? this.smoothLightPipeline : this.flatLightPipeline;

        shadeQuad(quad, pipeline, emissive);
        copyLightData(quad);
        bufferQuad(this.terrainBuilder, this.currentPos, quad, this.quadLightData);
    }

    private void colorizeQuad(MutableQuadViewImpl quad, int colorIndex) {
        if (colorIndex == -1) {
            return;
        }

        BlockColor blockColor = this.blockColorRegistry.getBlockColor(this.blockState.getBlock());
        int color = blockColor != null ? blockColor.getColor(this.blockState, this.renderRegion, this.blockPos, colorIndex) : -1;
        color = 0xFF000000 | color;

        for (int i = 0; i < 4; i++) {
            quad.color(i, ColorHelper.multiplyColor(color, quad.color(i)));
        }
    }

    private void shadeQuad(MutableQuadViewImpl quad, LightPipeline pipeline, boolean emissive) {
        pipeline.calculate(quad, this.blockPos, this.quadLightData, quad.cullFace(), quad.lightFace(), quad.hasShade());

        if (emissive) {
            for (int i = 0; i < 4; i++) {
                quad.color(i, ColorHelper.multiplyRGB(quad.color(i), this.quadLightData.br[i]));
                this.quadLightData.lm[i] = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
            }
        } else {
            for (int i = 0; i < 4; i++) {
                quad.color(i, ColorHelper.multiplyRGB(quad.color(i), this.quadLightData.br[i]));
                this.quadLightData.lm[i] = ColorHelper.maxBrightness(quad.lightmap(i), this.quadLightData.lm[i]);
            }
        }
    }

    private void copyLightData(MutableQuadViewImpl quad) {
        for (int i = 0; i < 4; i++) {
            quad.lightmap(i, this.quadLightData.lm[i]);
        }
    }

    private void bufferQuad(TerrainBuilder terrainBuilder, Vector3f pos, ModelQuadView quad, QuadLightData quadLightData) {
        QuadFacing quadFacing = quad.getQuadFacing();

        if (this.renderType == TerrainRenderType.TRANSLUCENT || !this.backFaceCulling) {
            quadFacing = QuadFacing.UNDEFINED;
        }

        TerrainBufferBuilder bufferBuilder = terrainBuilder.getBufferBuilder(quadFacing.ordinal());

        Vec3i normal = quad.getFacingDirection().getNormal();
        int packedNormal = I32_SNorm.packNormal(normal.getX(), normal.getY(), normal.getZ());

        float[] brightnessArr = quadLightData.br;
        int[] lights = quadLightData.lm;

        int idx = QuadUtils.getIterationStartIdx(brightnessArr, lights);

        bufferBuilder.ensureCapacity();

        for (byte i = 0; i < 4; ++i) {
            final float x = pos.x() + quad.getX(idx);
            final float y = pos.y() + quad.getY(idx);
            final float z = pos.z() + quad.getZ(idx);

            final int quadColor = quad.getColor(idx);
            int color = ColorUtil.ARGB.toRGBA(quadColor);

            final int light = lights[idx];
            final float u = quad.getU(idx);
            final float v = quad.getV(idx);

            bufferBuilder.vertex(x, y, z, color, u, v, light, packedNormal);

            idx = (idx + 1) & 0b11;
        }
    }

    private boolean shouldRenderFace(@Nullable Direction face) {
        if (face == null || !this.enableCulling) {
            return true;
        }

        int mask = 1 << face.get3DDataValue();
        if ((this.cullCompletionFlags & mask) == 0) {
            this.cullCompletionFlags |= mask;

            if (faceNotOccluded(this.blockState, face)) {
                this.cullResultFlags |= mask;
                return true;
            }

            return false;
        }

        return (this.cullResultFlags & mask) != 0;
    }

    private boolean faceNotOccluded(BlockState state, Direction face) {
        BlockGetter getter = this.renderRegion;
        BlockPos adjPos = this.tempPos.setWithOffset(this.blockPos, face);
        BlockState adjacent = getter.getBlockState(adjPos);

        if (state.skipRendering(adjacent, face)) {
            return false;
        }

        if (adjacent.canOcclude()) {
            var shape = state.getFaceOcclusionShape(getter, this.blockPos, face);
            if (shape.isEmpty()) {
                return true;
            }

            var adjShape = adjacent.getFaceOcclusionShape(getter, adjPos, face.getOpposite());
            if (adjShape.isEmpty()) {
                return true;
            }

            if (shape == net.minecraft.world.phys.shapes.Shapes.block() && adjShape == net.minecraft.world.phys.shapes.Shapes.block()) {
                return false;
            }

            OcclusionKey key = this.lookupKey.set(state, adjacent, face);
            byte cached = this.occlusionCache.getAndMoveToFirst(key);
            if (cached != 127) {
                return cached != 0;
            }

            boolean result = net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape, adjShape, net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST);
            if (this.occlusionCache.size() == 2048) {
                this.occlusionCache.removeLastByte();
            }

            this.occlusionCache.putAndMoveToFirst(new OcclusionKey(state, adjacent, face), (byte) (result ? 1 : 0));
            return result;
        }

        return true;
    }

    private final BlockPos.MutableBlockPos tempPos = new BlockPos.MutableBlockPos();
    private int cullCompletionFlags;
    private int cullResultFlags;

    public void clearCache() {
        this.occlusionCache.clear();
    }

    private static final class OcclusionKey {
        private BlockState state;
        private BlockState adjacent;
        private Direction face;

        OcclusionKey() {
        }

        OcclusionKey(BlockState state, BlockState adjacent, Direction face) {
            this.state = state;
            this.adjacent = adjacent;
            this.face = face;
        }

        OcclusionKey set(BlockState state, BlockState adjacent, Direction face) {
            this.state = state;
            this.adjacent = adjacent;
            this.face = face;
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof OcclusionKey other)) {
                return false;
            }
            return this.state == other.state && this.adjacent == other.adjacent && this.face == other.face;
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(this.state);
            result = 31 * result + System.identityHashCode(this.adjacent);
            result = 31 * result + this.face.ordinal();
            return result;
        }
    }
}
