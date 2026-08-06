package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

// The entity manager holds two maps keyed by chunk — what each chunk's entities are allowed to do, and whether they
// have been read off disk — and it answers four questions out of them: canPositionTick for a block and for a chunk,
// isTicking, and areEntitiesLoaded. Each one is asked with a coordinate its caller walked to, and every one of them
// ends in the same lookup, so the whole class shares a single defect: a key past the bounds names a chunk the manager
// has never held, both maps answer with their default (HIDDEN, FRESH), and the caller is told a flat no about ground
// that is plainly ticking a few steps away on the other side.
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

    // canPositionTick contributes two matches — both overloads answer to the bare name.
    @ModifyArg(
            method = {"isTicking", "canPositionTick", "areEntitiesLoaded"},
            at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"),
            index = 0,
            expect = 4)
    private long toroidal$askThePhysicalChunk(long chunkKey) {
        if (this.toroidal$level == null) {
            return chunkKey;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        return transformer == null ? chunkKey : transformer.chunks.wrapChunkKey(chunkKey);
    }
}
