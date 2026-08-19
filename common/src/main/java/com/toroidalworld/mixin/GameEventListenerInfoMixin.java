package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.RecipientPositionHolder;

import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.level.gameevent.GameEvent$ListenerInfo")
public class GameEventListenerInfoMixin implements RecipientPositionHolder {
    @Unique
    private Vec3 toroidal$recipientPos;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$keepRecipientPos(Holder<GameEvent> gameEvent, Vec3 source, GameEvent.Context context,
            GameEventListener recipient, Vec3 recipientPos, CallbackInfo ci) {
        this.toroidal$recipientPos = recipientPos;
    }

    @Override
    public Vec3 toroidal$recipientPos() {
        return this.toroidal$recipientPos;
    }
}
