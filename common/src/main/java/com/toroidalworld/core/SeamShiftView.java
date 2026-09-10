package com.toroidalworld.core;

import com.toroidalworld.api.v1.SeamShift;
import com.toroidalworld.api.v1.ToroidalShape.Orientation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SeamShiftView implements SeamShift {
    private final DeckTransformation transformation;

    public SeamShiftView(DeckTransformation transformation) {
        this.transformation = transformation;
    }

    @Override
    public boolean isIdentity() {
        return this.transformation.isIdentity();
    }

    @Override
    public Orientation orientation() {
        FoldOrientation orientation = this.transformation.orientation();
        return new Orientation(orientation.flipsX(), orientation.flipsZ());
    }

    @Override
    public Vec3 apply(Vec3 pos) {
        return this.transformation.apply(pos);
    }

    @Override
    public BlockPos apply(BlockPos pos) {
        return this.transformation.apply(pos);
    }

    @Override
    public ChunkPos apply(ChunkPos chunk) {
        return this.transformation.apply(chunk);
    }

    @Override
    public AABB apply(AABB box) {
        return this.transformation.apply(box);
    }
}
