package com.toroidalworld.mixin;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;

@Mixin(StructureBlockEntity.class)
public class StructureBlockEntityMixin {
    @ModifyReturnValue(method = "getRelatedCorners", at = @At("RETURN"))
    private Stream<BlockPos> toroidal$cornersInOwnFrame(Stream<BlockPos> corners) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        WorldFold transformer = level == null ? null : WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return corners;
        }

        BlockPos anchor = self.getBlockPos();
        return corners.map(corner -> transformer.nearestCopy(anchor, corner));
    }
}
