package com.toroidalworld.entity;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.toroidalworld.ToroidalWorld;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

// The moment an entity enters the world, and the last one at which its position is still only its own. Immediately
// after, the entity manager files it by that position — reading it off the object, not from the caller — and a
// coordinate past the bounds names a section of a chunk the world never loads, which is born HIDDEN: never ticked,
// never tracked, never found again, and saved to a region file out there. That is the whole of the bug, whatever
// spawned it: a piston dropping a block it destroyed across the seam, a spawner, worldgen placing a mob.
//
// Corrected here rather than in each spawner because this is the one gate they all pass — every server path into the
// world posts this event first, including the chunk load, so re-reading an already-stranded entity repairs it.
//
// absSnapTo, not setPos: the old position has to move with it, or the entity spends a tick believing it travelled a
// whole world. It deliberately does not route through snapTo, so a joining player's client mirror is left alone.
//
// A level that does not wrap answers null here, and so does every client level — the client is told the world is
// infinite, and its coordinates arrive already translated into its own frame.
@EventBusSubscriber(modid = ToroidalWorld.MODID)
public final class SeamEntityEntry {
    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(event.getLevel());
        if (transformer == null) {
            return;
        }

        Entity entity = event.getEntity();
        Vec3 position = entity.position();
        if (!transformer.vectors.isOver(position)) {
            return;
        }

        Vec3 wrapped = transformer.vectors.wrap(position);
        entity.absSnapTo(wrapped.x, wrapped.y, wrapped.z);
    }

    private SeamEntityEntry() {
    }
}
