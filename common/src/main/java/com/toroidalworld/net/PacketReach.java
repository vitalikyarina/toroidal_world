package com.toroidalworld.net;

import com.toroidalworld.core.CoordinateConstants;

// How far a positional packet may legitimately land from the client it is sent to, carried with the name of the
// traffic it describes so a log line says what it judged.
//
// The translation guard has nothing else to test against. Folding a coordinate to the copy nearest the anchor cannot
// produce a difference larger than half the world, so half the world is not a bound — it is the shape of the answer.
// The only real bound is on the other side: the radius vanilla's own send site measured before it put the packet on
// the wire. A sound whose sender offered it to listeners within 16 blocks (256 blocks with a volume of 16) has no
// business arriving 300 blocks from the anchor, and that is a certainty rather than a suspicion.
//
// Every value here is read from the send site, not chosen: change one only against the vanilla source it came from.
public record PacketReach(String kind, double blocks) {
    // ServerLevel.sendParticles offers a particle to players within 32 blocks, or within 512 when the sender overrides
    // the limiter — /particle with force, and the payload paths that ride on it.
    public static final PacketReach PARTICLE = new PacketReach("particle", 32.0);
    public static final PacketReach FORCED_PARTICLE = new PacketReach("forced_particle", 512.0);

    // ServerLevel.explode sends the burst to everyone within 64 blocks of the centre.
    public static final PacketReach EXPLOSION = new PacketReach("explosion", 64.0);

    // SoundEvent.getRange: the range the event fixes for itself, or 16 blocks scaled by the volume once the volume
    // passes one. Both the level and the playsound command gate on exactly this number, and the packet carries the
    // very volume it was computed from, so the radius can be recovered from the packet alone.
    public static PacketReach sound(float range) {
        return new PacketReach("sound", range);
    }

    // Everything that reaches the client because an entity is tracked for them — a placement, a teleport, a position
    // sync, a minecart's lerp steps, the source of a damage event, the anchor a particle payload hangs on.
    // ChunkMap's tracker shows an entity only within min(its own tracking range, the player's view distance in
    // blocks), so the view is the outer bound whatever the entity's range happens to be.
    public static PacketReach tracked(int viewDistance) {
        return new PacketReach("tracked_entity", (double) viewDistance * CoordinateConstants.CHUNK_WIDTH);
    }
}
