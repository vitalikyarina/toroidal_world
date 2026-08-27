package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.toroidalworld.compat.create.ChassisWalkFrame;
import com.toroidalworld.compat.create.CreateMultiblockFold;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Contraption.class, remap = false)
public class ContraptionMixin {
    @Shadow
    public BlockPos anchor;

    @Shadow
    public AABB bounds;

    @Shadow
    protected List<AABB> superglue;

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

    @WrapMethod(method = "moveChassis")
    private boolean toroidal$walkChassisInTheAssemblyFrame(Level world, BlockPos pos, Direction movementDirection,
            Queue<BlockPos> frontier, Set<BlockPos> visited, Operation<Boolean> original) {
        return ChassisWalkFrame.withAnchor(world, pos,
                () -> original.call(world, pos, movementDirection, frontier, visited));
    }

    @Inject(method = "removeBlocksFromWorld",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Ljava/util/Set;forEach(Ljava/util/function/Consumer;)V"),
            require = 1,
            allow = 1)
    private void toroidal$foldGlueIntoTheLocalFrame(Level world, BlockPos offset, CallbackInfo ci) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(world);
        if (transformer == null) {
            return;
        }

        Vec3 center = bounds == null ? Vec3.ZERO : bounds.getCenter();
        superglue.replaceAll(box -> transformer.foldBox(center, box).value());
    }

    @ModifyExpressionValue(method = "addBlocksToWorld",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/StructureTransform;apply(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$canonicalDisassemblyTarget(BlockPos target, @Local(argsOnly = true) Level world) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return target;
        }

        return CreateSeamFold.canonical(serverLevel, target);
    }

    @Inject(method = "addBlocksToWorld",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lcom/simibubi/create/content/contraptions/Contraption;translateMultiblockControllers(Lcom/simibubi/create/content/contraptions/StructureTransform;)V"))
    private void toroidal$canonicalizeTranslatedControllers(Level world, StructureTransform transform,
            CallbackInfo ci) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }

        for (StructureBlockInfo info : capturedMultiblocks.values()) {
            CompoundTag nbt = info.nbt();
            // The rotation branch leaves Controller as the capture-time local and marks the part with LastKnownPos,
            // and addBlocksToWorld drops that Controller outright — canonicalizing a local would corrupt it.
            if (nbt == null || !nbt.contains(CreateMultiblockFold.CONTROLLER_KEY)
                    || nbt.contains(CreateMultiblockFold.LAST_KNOWN_POS_KEY)) {
                continue;
            }

            BlockPos stored = NBTHelper.readBlockPos(nbt, CreateMultiblockFold.CONTROLLER_KEY);
            BlockPos canonical = CreateSeamFold.canonical(serverLevel, stored);
            if (canonical != stored) {
                nbt.put(CreateMultiblockFold.CONTROLLER_KEY, NbtUtils.writeBlockPos(canonical));
            }
        }
    }
}
