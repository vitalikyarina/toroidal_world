package com.toroidalworld.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.accessors.RecipientPositionHolder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.phys.Vec3;

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
        if (transformer != null) {
            listenerInfos.replaceAll(info -> toroidal$nearestCopy(info, transformer));
        }

        original.call(listenerInfos);
    }

    @Unique
    private static GameEvent.ListenerInfo toroidal$nearestCopy(GameEvent.ListenerInfo info, WorldFold transformer) {
        Vec3 recipientPos = ((RecipientPositionHolder) (Object) info).toroidal$recipientPos();
        Vec3 nearest = transformer.nearestCopy(info.source(), recipientPos);
        return nearest == recipientPos
                ? info
                : new GameEvent.ListenerInfo(
                        info.gameEvent(), info.source(), info.context(), info.recipient(), nearest);
    }
}
