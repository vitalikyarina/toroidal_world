package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.probe.ReseatProbe;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.Vec3;

// The entity manager holds two maps keyed by chunk — what each chunk's entities are allowed to do, and whether they
// have been read off disk — and it answers three questions out of them: canPositionTick for a block and for a chunk,
// and areEntitiesLoaded. Each one is asked with a coordinate its caller walked to, and every one of them ends in the
// same lookup, so the whole class shares a single defect: a key past the bounds names a chunk the manager has never
// held, both maps answer with their default (HIDDEN, FRESH), and the caller is told a flat no about ground that is
// plainly ticking a few steps away on the other side.
//
// The four callers it costs are worth naming, because they are what the no is felt as: a raid gates every candidate
// spawn position on canPositionTick and so can only ever raise waves on the in-world side of a village near the seam;
// natural spawning refuses a candidate whose position drifts into the chunk past the bounds; and the block-entity and
// scheduled-tick gates read areEntitiesLoaded.
//
// So the fold sits on the argument of that one lookup rather than on any of the four. Reads only, deliberately:
// updateChunkStatus writes with the holder's own position, and a phantom holder folded onto its physical chunk would
// overwrite the real chunk's status with its own — two owners for one entry, which is exactly what the read-side fold
// exists to avoid needing. It is the same correction DistanceManagerMixin makes to inEntityTickingRange, the other
// half of the very question isPositionEntityTicking asks.
//
// A key already inside the bounds is handed back untouched, so the in-world side of every one of those questions is
// answered by vanilla's own path, unchanged.
//
// The level is bound rather than looked up: vanilla's entity manager never learns which level owns it.
@Mixin(PersistentEntitySectionManager.class)
public class EntitySectionManagerMixin implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    // The moment an entity enters the world, and the last one at which its position is still only its own. The very
    // next line files it by that position — SectionPos.asLong(entity.blockPosition()) — and a coordinate past the
    // bounds names a section of a chunk the world never loads, which is born HIDDEN: never ticked, never tracked,
    // never found again, and saved to a region file out there. That is the whole of the bug, whatever spawned it: a
    // piston dropping a block it destroyed across the seam, a spawner, worldgen placing a mob.
    //
    // Corrected here rather than in each spawner because this is the one gate they all pass — every server path into
    // the world funnels through it: fresh spawns, worldgen, players, and the chunk load, so re-reading an
    // already-stranded entity repairs it.
    //
    // The gate has a different name on each loader. Vanilla's funnel is addEntity; NeoForge splits an event-firing
    // addEntity from addEntityWithoutEvent and routes players through the latter only, so both names are listed and
    // require = 1 accepts whichever the running loader has. On NeoForge both match and addEntity delegates into
    // addEntityWithoutEvent, so a wrapped entity is read twice — the second pass finds it already in bounds and does
    // nothing.
    //
    // absMoveTo, not setPos: the old position has to move with it, or the entity spends a tick believing it travelled
    // a whole world. It deliberately does not route through snapTo, so a joining player's client mirror is left alone.
    //
    // A level that does not wrap answers null here; this manager only ever serves server levels.
    @Inject(
            method = {
                    "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z",
                    "addEntityWithoutEvent(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z" },
            at = @At("HEAD"),
            require = 1)
    private void toroidal$wrapJoiningEntity(EntityAccess entity, boolean loaded, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Entity actualEntity)) {
            return;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(actualEntity.level());
        if (transformer == null) {
            return;
        }

        Vec3 position = actualEntity.position();
        if (!transformer.vectors.isOver(position)) {
            return;
        }

        Vec3 wrapped = transformer.vectors.wrap(position);
        actualEntity.absMoveTo(wrapped.x, wrapped.y, wrapped.z);
    }

    // canPositionTick contributes two matches — both overloads answer to the bare name.
    @ModifyArg(
            method = {"canPositionTick", "areEntitiesLoaded"},
            at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"),
            index = 0,
            expect = 3)
    private long toroidal$askThePhysicalChunk(long chunkKey) {
        if (this.toroidal$level == null) {
            return chunkKey;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        return ReseatProbe.decidedChunkKey(this.toroidal$level, ReseatProbe.ENTITY_CHUNK_KEY, chunkKey,
                transformer == null ? chunkKey : transformer.chunks.wrapChunkKey(chunkKey));
    }
}
