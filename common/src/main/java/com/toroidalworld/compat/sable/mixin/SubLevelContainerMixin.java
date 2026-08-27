package com.toroidalworld.compat.sable.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.sable.SableSeamQuery;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.world.level.Level;

@Mixin(value = SubLevelContainer.class, remap = false)
public abstract class SubLevelContainerMixin {
    @Shadow
    @Final
    private List<SubLevel> allSubLevels;

    @Shadow
    @Final
    private Level level;

    @ModifyReturnValue(method = "queryIntersecting", at = @At("RETURN"))
    private Iterable<SubLevel> toroidal$intersectAcrossSeam(Iterable<SubLevel> original, BoundingBox3dc bounds) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(this.level);
        if (fold == null) {
            return original;
        }

        return () -> this.allSubLevels.stream()
                .filter(subLevel -> SableSeamQuery.intersects(fold, subLevel.boundingBox(), bounds))
                .iterator();
    }
}
