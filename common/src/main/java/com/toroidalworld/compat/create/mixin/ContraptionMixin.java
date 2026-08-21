package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

@Mixin(value = Contraption.class, remap = false)
public class ContraptionMixin {
    @Unique
    private static final String CONTROLLER_KEY = "Controller";

    @Unique
    private static final String LAST_KNOWN_POS_KEY = "LastKnownPos";

    @Shadow
    public BlockPos anchor;

    @Shadow
    protected Multimap<BlockPos, StructureBlockInfo> capturedMultiblocks;

    @ModifyExpressionValue(
            method = "captureMultiblock",
            at = @At(value = "INVOKE",
                    target = "Lnet/createmod/catnip/nbt/NBTHelper;readBlockPos(Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldCapturedController(BlockPos stored, BlockPos localPos, StructureBlockInfo info,
            BlockEntity be) {
        return CreateSeamFold.foldPosition(be.getLevel(), localPos.offset(anchor), stored);
    }

    @Inject(method = "addBlocksToWorld",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lcom/simibubi/create/content/contraptions/Contraption;translateMultiblockControllers(Lcom/simibubi/create/content/contraptions/StructureTransform;)V"))
    private void toroidal$canonicalizeTranslatedControllers(Level world, StructureTransform transform,
            CallbackInfo ci) {
        for (StructureBlockInfo info : capturedMultiblocks.values()) {
            CompoundTag nbt = info.nbt();
            // The rotation branch leaves Controller as the capture-time local and marks the part with LastKnownPos,
            // and addBlocksToWorld drops that Controller outright — canonicalizing a local would corrupt it.
            if (nbt == null || !nbt.contains(CONTROLLER_KEY) || nbt.contains(LAST_KNOWN_POS_KEY)) {
                continue;
            }

            BlockPos stored = NBTHelper.readBlockPos(nbt, CONTROLLER_KEY);
            BlockPos canonical = CreateSeamFold.canonical(world, stored);
            if (canonical != stored) {
                nbt.put(CONTROLLER_KEY, NbtUtils.writeBlockPos(canonical));
            }
        }
    }
}
