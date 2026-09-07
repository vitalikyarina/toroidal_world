package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;

import net.minecraft.world.phys.Vec3;

@Mixin(value = SymmetryMirror.class, remap = false)
public interface SymmetryMirrorAccessor {
    @Invoker("create")
    static SymmetryMirror toroidal$create(Integer orientationIndex, Vec3 position, String type, Boolean enable) {
        throw new AssertionError();
    }
}
