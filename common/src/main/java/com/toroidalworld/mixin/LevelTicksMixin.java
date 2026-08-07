package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;

// A scheduled tick is filed in a container keyed by its own position, and the container is only ever drained for a
// chunk the world holds — so a tick named a step past the bounds is filed under a chunk nothing ticks, and simply
// never runs. Wrapping the position as the tick enters makes that the level's one invariant: every tick it holds is
// filed against the ground it really means. A level keeps one of these for blocks and one for fluids, and both are
// this class, so the same wrap serves lava creeping across the seam and a repeater alike.
//
// This is the single way in. Everything scheduled the ordinary way goes through ScheduledTickAccess.scheduleTick,
// which ends at schedule() below; and so does /clone's copyAreaFrom, which builds each copied tick itself from a
// source position plus an offset and hands it straight here — a destination running across the seam would otherwise
// file its far half where nothing will ever drain it: a cloned clock that never wakes.
//
// What copyAreaFrom *reads* is not covered, deliberately. It gathers the ticks to copy through
// allContainers.get(ChunkPos.pack(x, z)) on the raw source region, and a key past the bounds is never in that map, so
// a source crossing the seam offers up only the sliver of itself that falls inside its own numeric range. An ordinary
// /clone loses nothing to that: blocks land with UPDATE_CLIENTS alone, onPlace runs, and whatever wanted a pending
// tick arms a fresh one. Only /clone … strict does, which places with UPDATE_SKIP_ALL_SIDEEFFECTS precisely so that
// nothing wakes up — and even there the loss is a remaining delay, on the crossing of two rare things. Covering it
// would mean cutting the source region into the pieces the world really holds and copying from each in turn.
//
// The level is bound rather than looked up: vanilla's tick container never sees the world it belongs to.
@Mixin(LevelTicks.class)
public class LevelTicksMixin<T> implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Unique
    private WorldLoopTransformer toroidal$transformer;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    @ModifyVariable(method = "schedule", at = @At("HEAD"), argsOnly = true)
    private ScheduledTick<T> toroidal$fileTickAtPhysicalPos(ScheduledTick<T> tick) {
        if (this.toroidal$level == null) {
            return tick;
        }

        WorldLoopTransformer transformer = toroidal$transformer();
        if (!transformer.isWrapped()) {
            return tick;
        }

        BlockPos pos = tick.pos();
        if (!transformer.coords.x.isOver(pos.getX()) && !transformer.coords.z.isOver(pos.getZ())) {
            return tick;
        }

        return new ScheduledTick<>(tick.type(), transformer.blocks.wrap(pos), tick.triggerTick(), tick.priority(),
                tick.subTickOrder());
    }

    // Every fluid step and every redstone delay passes through here, and a level's transformer never changes.
    // Deliberately not volatile: resolution is idempotent — transformerOf hands back the level's one attachment
    // instance — so a race can only cost a repeated lookup, never a second transformer.
    @Unique
    private WorldLoopTransformer toroidal$transformer() {
        if (this.toroidal$transformer == null) {
            this.toroidal$transformer = WorldLoopAttachments.transformerOf(this.toroidal$level);
        }

        return this.toroidal$transformer;
    }
}
