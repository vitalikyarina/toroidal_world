package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFold.Folded;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;

@Mixin(LevelTicks.class)
public class LevelTicksMixin<T> implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Unique
    private WorldFold toroidal$transformer;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    @ModifyVariable(method = "schedule", at = @At("HEAD"), argsOnly = true)
    private ScheduledTick<T> toroidal$fileTickAtPhysicalPos(ScheduledTick<T> tick) {
        if (this.toroidal$level == null) {
            return tick;
        }

        WorldFold transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return tick;
        }

        BlockPos pos = tick.pos();
        if (!transformer.isOver(pos)) {
            return tick;
        }

        return new ScheduledTick<>(tick.type(), transformer.fold(pos), tick.triggerTick(), tick.priority(),
                tick.subTickOrder());
    }

    @Inject(method = "clearArea", at = @At("HEAD"), cancellable = true)
    private void toroidal$clearEachCopyOfTheArea(BoundingBox area, CallbackInfo ci) {
        if (this.toroidal$level == null) {
            return;
        }

        WorldFold transformer = toroidal$transformer();
        if (!transformer.isWrapped() || !transformer.crossesBounds(area)) {
            return;
        }

        ci.cancel();

        @SuppressWarnings("unchecked")
        LevelTicks<T> ticks = (LevelTicks<T>) (Object) this;
        for (Folded<BoundingBox> piece : transformer.split(area)) {
            ticks.clearArea(piece.value());
        }
    }

    @Unique
    private WorldFold toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = WorldLoopAttachments.transformerOf(this.toroidal$level);
        }

        return this.toroidal$transformer;
    }
}
