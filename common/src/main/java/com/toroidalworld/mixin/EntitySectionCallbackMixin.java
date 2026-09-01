package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;

@Mixin(targets = "net/minecraft/world/level/entity/PersistentEntitySectionManager$Callback")
public class EntitySectionCallbackMixin {
    @Shadow
    @Final
    private EntityAccess entity;

    @ModifyExpressionValue(
            method = "onMove",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;asLong(Lnet/minecraft/core/BlockPos;)J"))
    private long toroidal$fileInPhysicalSection(long sectionKey) {
        if (!(this.entity instanceof Entity actualEntity)) {
            return sectionKey;
        }

        WorldFold transformer = ((TransformerSource) actualEntity).toroidal$wrappedTransformer();
        return transformer == null ? sectionKey : transformer.foldSectionNode(sectionKey);
    }
}
