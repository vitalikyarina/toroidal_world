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
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;

// A listener that asks to be served by distance is queued and the queue is sorted before delivery, on a raw Vec3
// distance each record froze at construction. Across the seam that key carries the width of the world, so the nearest
// listener is served last — and for the one kind of listener that asks for this order, the sculk catalyst, the order is
// the whole prize: the first one served takes the dead mob's experience and every later one finds it already spent.
@Mixin(GameEventDispatcher.class)
public class GameEventDispatcherMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @WrapOperation(
            method = "handleGameEventMessagesInQueue",
            at = @At(value = "INVOKE", target = "Ljava/util/Collections;sort(Ljava/util/List;)V"))
    private void toroidal$orderThroughSeam(List<GameEvent.ListenerInfo> listenerInfos, Operation<Void> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            original.call(listenerInfos);
            return;
        }

        listenerInfos.sort(Comparator.comparingDouble(info -> SeamRange.sqr(this.level,
                info.source(), ((RecipientPositionHolder) (Object) info).toroidal$recipientPos())));
    }
}
