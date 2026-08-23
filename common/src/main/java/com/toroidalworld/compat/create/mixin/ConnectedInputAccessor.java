package com.toroidalworld.compat.create.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;

import net.minecraft.core.BlockPos;

@Mixin(value = ConnectedInputHandler.ConnectedInput.class, remap = false)
public interface ConnectedInputAccessor {
    @Accessor("data")
    List<BlockPos> toroidal$data();

    @Accessor("isController")
    boolean toroidal$isController();
}
