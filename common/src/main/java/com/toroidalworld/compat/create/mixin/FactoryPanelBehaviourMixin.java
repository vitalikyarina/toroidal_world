package com.toroidalworld.compat.create.mixin;

import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.toroidalworld.compat.create.CreateFactoryPanelFold;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

@Mixin(value = FactoryPanelBehaviour.class, remap = false)
public abstract class FactoryPanelBehaviourMixin {
    @Shadow
    public Map<FactoryPanelPosition, FactoryPanelConnection> targetedBy;

    @Shadow
    public Map<BlockPos, FactoryPanelConnection> targetedByLinks;

    @Shadow
    public Set<FactoryPanelPosition> targeting;

    @Shadow
    public abstract FactoryPanelBlockEntity panelBE();

    // Neither hook covers the other: on chunk load read has no level yet, and a reload that has one never initializes.
    @Inject(method = "initialize", at = @At("RETURN"))
    private void toroidal$canonicaliseOnInitialize(CallbackInfo ci) {
        toroidal$canonicaliseStoredConnections();
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void toroidal$canonicaliseOnRead(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket,
            CallbackInfo ci) {
        toroidal$canonicaliseStoredConnections();
    }

    @Unique
    private void toroidal$canonicaliseStoredConnections() {
        Level level = panelBE().getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        CreateFactoryPanelFold.canonicalisePanels(level, this.targetedBy);
        CreateFactoryPanelFold.canonicaliseLinks(level, this.targetedByLinks);
        CreateFactoryPanelFold.canonicaliseTargets(level, this.targeting);
    }

    @WrapOperation(method = "moveTo",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"))
    private boolean toroidal$foldRelocationRange(BlockPos stored, Vec3i destination, double range,
            Operation<Boolean> original) {
        @Nullable Level level = panelBE().getLevel();
        if (level == null || !(destination instanceof BlockPos anchor)) {
            return original.call(stored, destination, range);
        }

        return original.call(CreateSeamFold.foldPosition(level, anchor, stored), destination, range);
    }
}
