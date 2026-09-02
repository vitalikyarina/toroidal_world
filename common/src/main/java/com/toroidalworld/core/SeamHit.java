package com.toroidalworld.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SeamHit {
    public static BlockHitResult reseat(BlockHitResult hit, WorldFold.Folded<BlockPos> foldedBlock) {
        BlockPos block = foldedBlock.value();
        if (foldedBlock.isIdentity() && block.equals(hit.getBlockPos())) {
            return hit;
        }

        FoldOrientation orientation = foldedBlock.orientation();
        Vec3 offset = orientation.applyToBlockOffset(
                hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos())));
        Vec3 location = Vec3.atLowerCornerOf(block).add(offset);
        Direction face = orientation.applyToFace(hit.getDirection());

        return hit.getType() == HitResult.Type.MISS
                ? BlockHitResult.miss(location, face, block)
                : new BlockHitResult(location, face, block, hit.isInside());
    }

    private SeamHit() {
    }
}
