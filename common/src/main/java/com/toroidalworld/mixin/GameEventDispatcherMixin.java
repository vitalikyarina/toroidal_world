package com.toroidalworld.mixin;

import java.util.Comparator;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.accessors.RecipientPositionHolder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;

@Mixin(GameEventDispatcher.class)
public class GameEventDispatcherMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @WrapOperation(
            method = "handleGameEventMessagesInQueue",
            at = @At(value = "INVOKE", target = "Ljava/util/Collections;sort(Ljava/util/List;)V"))
    private void toroidal$orderThroughSeam(List<GameEvent.ListenerInfo> listenerInfos, Operation<Void> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            original.call(listenerInfos);
            return;
        }

        listenerInfos.sort(Comparator.comparingDouble(info -> SeamRange.sqr(this.level,
                info.source(), ((RecipientPositionHolder) (Object) info).toroidal$recipientPos())));
    }
}
