package com.toroidalworld.compat.create.mixin;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.toroidalworld.compat.create.CanonicalPositionKeys;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(targets = "com.simibubi.create.content.fluids.FluidNetwork", remap = false)
public class FluidNetworkMixin {
    @Shadow
    Set<BlockPos> visited;

    @Shadow
    Map<BlockPos, WeakReference<FluidTransportBehaviour>> cache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$canonicaliseKeys(Level world, BlockFace location, Supplier<?> sourceSupplier,
            CallbackInfo ci) {
        this.visited = CanonicalPositionKeys.set(world);
        this.cache = CanonicalPositionKeys.map(world);
    }
}
