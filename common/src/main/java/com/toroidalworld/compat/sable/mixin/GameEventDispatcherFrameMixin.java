package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SeamFrame;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.phys.Vec3;

@Mixin(GameEventDispatcher.class)
public abstract class GameEventDispatcherFrameMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @WrapMethod(method = "post")
    private void toroidal$frameOnGameEvent(Holder<GameEvent> event, Vec3 source, GameEvent.Context context, Operation<Void> original) {
        SeamFrame.run(this.level, () -> source, () -> original.call(event, source, context));
    }
}
