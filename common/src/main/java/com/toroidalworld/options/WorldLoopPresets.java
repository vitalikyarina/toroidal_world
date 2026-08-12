package com.toroidalworld.options;

import com.toroidalworld.core.CoordinateConstants;

// The named world configurations — one definition serving the settings screen's preset row now and the jar world
// presets (level-type=toroidal_world:<id>) later, so the screen and the server can never drift apart. Ids are
// registry-path material: snake_case, stable, describing size and nothing else.
//
// A preset names the complete choice, not just the overworld width. Its nether scale is vanilla's 1:8 wherever the
// width admits it (a scale exists only where it divides the width with the nether itself at least 16 chunks
// (256 blocks) wide) and the deepest available below that otherwise — 1:2 at 32 chunks (512 blocks), 1:4 at 64
// (1024 blocks). The End starts at the designed default — 256 chunks (4096 blocks), the width the gateway/island
// math was tuned for — and grows a gentle 64 chunks (1024 blocks) per step: a bigger world earns a roomier
// outer-island ring without the End racing the overworld.
//
// The overworld spread doubles from the playable default up, and each step is the smallest width that buys something
// concrete: 1:4 at 64 chunks (1024 blocks), vanilla's 1:8 at 128 (2048 blocks). Past that the steps buy structure
// room: at 256 chunks (4096 blocks) the half-width reaches the first stronghold ring's band (88–168 chunks /
// 1408–2688 blocks radius), and at 512 chunks (8192 blocks) the widest overworld grid — woodland mansions at
// 80 chunks (1280 blocks) — gets six cells per axis.
public enum WorldLoopPresets {
    TINY("tiny", 32, 2, 256),
    SMALL("small", 64, 4, 320),
    MEDIUM("medium", 128, 8, 384),
    LARGE("large", 256, 8, 448),
    HUGE("huge", 512, 8, 512);

    private final String id;
    private final int chunkWidth;
    private final int netherScale;
    private final int endChunkWidth;

    WorldLoopPresets(String id, int chunkWidth, int netherScale, int endChunkWidth) {
        this.id = id;
        this.chunkWidth = chunkWidth;
        this.netherScale = netherScale;
        this.endChunkWidth = endChunkWidth;
    }

    public String id() {
        return id;
    }

    public int chunkWidth() {
        return chunkWidth;
    }

    public int netherScale() {
        return netherScale;
    }

    public int endChunkWidth() {
        return endChunkWidth;
    }

    public int blockWidth() {
        return chunkWidth * CoordinateConstants.CHUNK_WIDTH;
    }

    public int endBlockWidth() {
        return endChunkWidth() * CoordinateConstants.CHUNK_WIDTH;
    }
}
