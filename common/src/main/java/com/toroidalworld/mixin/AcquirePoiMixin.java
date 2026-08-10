package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.probe.ReseatProbe;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;

// The path is built to the copy of the POI nearest the mob, which past the bounds is a phantom position outside the
// world (PathNavigationMixin unwraps the targets before pathfinding). AcquirePoi then looks that position up, takes the
// POI and writes it into the acquired memory as-is — but the POI, and every later distance check that walks the villager
// to it (GoToPotentialJobSite, AssignProfessionFromJobSite), lives at the real position inside the world. Left unwrapped
// the lookup finds nothing, the claim silently fails and the villager never gets a job across the seam. Folding the
// claimed target back into the bounds makes the lookup, the take and the stored memory all land on the actual POI.
//
// One point fixes all three uses: the wrapped value is stored into a local that feeds getType, take and GlobalPos.of.
@Mixin(AcquirePoi.class)
public class AcquirePoiMixin {
    @ModifyExpressionValue(
            method = "lambda$create$6",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/pathfinder/Path;getTarget()Lnet/minecraft/core/BlockPos;"))
    private static @Nullable BlockPos toroidal$wrapClaimedPoi(@Nullable BlockPos target,
            @Local(argsOnly = true) ServerLevel level) {
        if (target == null) {
            return null;
        }
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return ReseatProbe.decided(level, ReseatProbe.POI_CLAIM, target,
                transformer == null ? target : transformer.blocks.wrap(target));
    }
}
