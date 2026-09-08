# Running a Toroidal World on a Dedicated Server

A world's shape lives entirely in its chunk generators, so a dedicated server needs no screen: the mod ships the same named world sizes the create-world screen offers as world presets, for the toroidal world and for the cylinder, and one `server.properties` line picks one. The result is a fully shaped world — overworld, nether and End — that behaves exactly like one created in singleplayer. Joining players need the mod installed; they receive the world's bounds automatically on login.

## Setup

1. Put the mod jar into the server's `mods/` folder.
2. In `server.properties`, pick a preset — do this **before the first start**, the world type is read only when the world is created:

```properties
level-type=toroidal_world\:medium
```

3. Start the server. No datapack needed.

| Size | Toroidal `level-type` | Cylinder `level-type` | Overworld | Nether (portal scale) | End |
| --- | --- | --- | --- | --- | --- |
| Tiny | `toroidal_world:tiny` | `toroidal_world:cylinder_tiny` | 32 chunks (512 blocks) | 16 chunks (256 blocks), 1:2 | 256 chunks (4096 blocks) |
| Small | `toroidal_world:small` | `toroidal_world:cylinder_small` | 64 chunks (1024 blocks) | 16 chunks (256 blocks), 1:4 | 320 chunks (5120 blocks) |
| Medium | `toroidal_world:medium` | `toroidal_world:cylinder_medium` | 128 chunks (2048 blocks) | 16 chunks (256 blocks), 1:8 | 384 chunks (6144 blocks) |
| Large | `toroidal_world:large` | `toroidal_world:cylinder_large` | 256 chunks (4096 blocks) | 32 chunks (512 blocks), 1:8 | 448 chunks (7168 blocks) |
| Huge | `toroidal_world:huge` | `toroidal_world:cylinder_huge` | 512 chunks (8192 blocks) | 64 chunks (1024 blocks), 1:8 | 512 chunks (8192 blocks) |

A toroidal preset loops both horizontal axes at the given width. A cylinder preset loops along X at that width and leaves Z endless like vanilla — a cylinder looping along Z is a custom preset with the two axes swapped (below).

If the id has a typo, vanilla logs a warning (`Failed to parse level-type …, defaulting to minecraft:normal`) and silently creates an ordinary infinite world — check the first lines of the log if the world comes out non-toroidal.

## Custom sizes

Two ready packs sit next to this page: [`datapacks/torus/`](datapacks/torus) loops both axes, [`datapacks/cylinder/`](datapacks/cylinder) loops X alone. Both carry a 192-chunk (3072-block) overworld, a 24-chunk (384-block) nether at 1:8 and a 256-chunk (4096-block) End; the toroidal one also states `climate_compression` in its `custom` form. Copy the folder into `world/datapacks/` **before the first server start**, point `server.properties` at `my_pack\:torus` or `my_pack\:cylinder`, and edit the numbers to the size you want. Change the overworld and the nether has to follow it — [Nether width](#nether-width) carries the arithmetic.

For a size outside the preset spread, define your own world preset in a datapack:

```
world/datapacks/my-toroidal/
├── pack.mcmeta
└── data/
    └── my_pack/
        └── worldgen/
            └── world_preset/
                └── my_toroidal.json
```

The `world/` folder does not exist yet on a fresh server — create it with just the datapack inside; the server builds the rest around it and enables the pack automatically on first start. The preset resolves as `data/<namespace>/worldgen/world_preset/<file>.json` → `level-type=<namespace>\:<file>`, where the backslash escapes the colon `.properties` would otherwise read as the end of the key.

`pack.mcmeta`:

```json
{
  "pack": {
    "description": "Custom toroidal world preset",
    "min_format": 107,
    "max_format": 107
  }
}
```

107 is the data pack format of Minecraft 26.2. For another game version, read `pack_version.data_major` out of the `version.json` inside the game jar.

`my_toroidal.json` — the example carries the `tiny` configuration; change the `wrapping` bounds per dimension:

```json
{
  "dimensions": {
    "minecraft:overworld": {
      "type": "minecraft:overworld",
      "generator": {
        "type": "toroidal_world:toroidal",
        "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:overworld" },
        "settings": "minecraft:overworld",
        "wrapping": {
          "x": { "min_chunk": -16, "max_chunk": 16 },
          "z": { "min_chunk": -16, "max_chunk": 16 }
        },
        "climate_compression": false
      }
    },
    "minecraft:the_nether": {
      "type": "minecraft:the_nether",
      "generator": {
        "type": "toroidal_world:toroidal",
        "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:nether" },
        "settings": "minecraft:nether",
        "wrapping": {
          "x": { "min_chunk": -8, "max_chunk": 8 },
          "z": { "min_chunk": -8, "max_chunk": 8 }
        },
        "climate_compression": false
      }
    },
    "minecraft:the_end": {
      "type": "minecraft:the_end",
      "generator": {
        "type": "toroidal_world:toroidal",
        "biome_source": { "type": "minecraft:the_end" },
        "settings": "minecraft:end",
        "wrapping": {
          "x": { "min_chunk": -128, "max_chunk": 128 },
          "z": { "min_chunk": -128, "max_chunk": 128 }
        },
        "climate_compression": false
      }
    }
  }
}
```

Then point `server.properties` at it: `level-type=my_pack\:my_toroidal`. Left at `toroidal_world\:medium`, the world is created from that preset and the pack changes nothing, with no warning in the log to say so.

For a cylinder, give the looping axis its bounds and write the other axis as an empty object — the same axis in all three dimensions. This `wrapping` makes the overworld of a 64-chunk (1024-block) cylinder looping along Z:

```json
"wrapping": {
  "x": {},
  "z": { "min_chunk": -32, "max_chunk": 32 }
}
```

The empty object is required: an axis left out of `wrapping` is an error, not an endless axis.

### Size rules

`min_chunk`/`max_chunk` are a half-open chunk range: `-16 … 16` means 32 chunks (512 blocks), block bounds −256 … 255. The create-world screen enforces these rules for you. A hand-written preset is checked at startup instead: a width under the floor stops the server before anything is generated, and the other two are named as `BROKEN` on the `World shape:` lines, logged at `WARN`:

- **Overworld** — every looping axis at least 16 chunks (256 blocks), centered on zero as the example does (`min_chunk = -width/2`).
- **Nether** — the overworld width divided by the portal scale, and the scale must divide it exactly; the nether itself must stay at least 16 chunks (256 blocks) wide. An uneven ratio breaks portal linking near the seam. The arithmetic, the ceiling it puts on the scale and the widths that trip over it are in [Nether width](#nether-width).
- **End** — independent of the other two, at least 192 chunks (3072 blocks) on every looping axis, and in a cylinder looping along the same axis as the overworld. Smaller Ends lose the outer island ring — no end cities, no elytra — and let gateway teleports reach across the seam.

For a Superflat world use `"type": "toroidal_world:toroidal_flat"` with a `"settings"` object of the flat generator instead of the noise settings id.

## Nether width

The nether's bounds are the overworld's, divided by the portal scale: take the overworld's `min_chunk` and `max_chunk`, divide both by the scale, and write the result into the nether generator. Two limits sit on that division. It has to come out whole — otherwise the startup line reports `BROKEN portal scale on the x axis`. And the nether may never be narrower than 16 chunks (256 blocks). Together they put a ceiling on the scale — no world can carry one deeper than its own width divided by 16.

Widths below are in chunks.

| Overworld | Scale | Nether | What it shows |
| --- | --- | --- | --- |
| 256 (`-128 … 128`) | 1:8 | 32 (`-16 … 16`) | What right looks like: both widths even, both loops centered |
| 128 (`-64 … 64`) | 1:8 | 16 (`-8 … 8`) | The narrowest overworld that still carries 1:8 |
| 96 (`-48 … 48`) | 1:8 | 12 | Divides cleanly and is refused anyway: 12 is under the 16-chunk floor. The same overworld carries 1:6 → 16 (`-8 … 8`) |
| 100 (`-50 … 50`) | 1:8 | 12.5 | Does not divide, so the line comes out `BROKEN`. The deepest scale on offer is 1:5 → 20 (`-10 … 10`) |
| 200 (`-100 … 100`) | 1:8 | 25 (`-12 … 13`) | Divides, but into an odd width no loop can center. 1:10 gives 20; an overworld of 208 gives 26 (`-13 … 13`) |
| 3920 (`-1960 … 1960`) | 1:8 | 490 (`-245 … 245`) | A hand-typed width with nothing to watch for |
| 12345 | 1:15 | 823 (`-411 … 412`) | 1:8 does not exist here: 12345 is 3 × 5 × 823, and the ceiling is 771 |
| 29 | 1:1 | 29 (`-14 … 15`) | A prime width: 1:1 is the only ratio it has |
| 27 | 1:1 | 27 (`-13 … 14`) | Composite, but every divisor except 1 sits above the ceiling |
| `x: -96 … 96`, `z: {}` | 1:8 | `x: -12 … 12`, `z: {}` | A cylinder: numbers written into the nether's `z` pass without a word, and loop an axis the overworld leaves open |

An odd width can never produce an even nether — every divisor of an odd number divides it into odd parts — so that nether's loop never centers on zero.

Choose the overworld as a multiple of 16 chunks, 128 or wider, and none of this comes up: 1:8 always divides, and the nether lands even and centered.

## World options

Next to `wrapping`, the generator takes the two options the create-world screen offers. Each dimension carries its own, so an option goes on the generator of every dimension it should apply to — the presets the mod ships write both in all three. They work only where the dimension loops on both axes: in a cylinder they are read and then change nothing.

**`climate_compression`** shrinks biomes so a full climate fits into one lap of the world; coastlines and terrain keep their usual size. It takes either a boolean — `true` for Auto, `false` for Off — or an object naming the mode:

```json
"climate_compression": { "mode": "strong" }
```

```json
"climate_compression": { "mode": "custom", "factor": 6 }
```

- `off` — biomes keep their vanilla size, so a small world may come out all forest or all desert.
- `auto` — shrinks them just enough for one climate per lap; on a world already wide enough it changes nothing.
- `strong` — shrinks them four times over, and more where fitting the climate needs more, never less than `auto`.
- `custom` — shrinks them exactly `factor` times, a whole number from 1 to 16. `factor` is read in this mode alone.

**Leaving the field out means `auto`, not off.** Every shipped preset writes `"climate_compression": false`, so a hand-written preset started from one keeps that line, or gets compressed biomes it never asked for.

**`guaranteed_land`** keeps a world from coming out all ocean: where a lap would leave nowhere to stand, `true` raises the land level just enough for one island. A world that already has land is left as it is. It defaults to `false`.

## Notes

- To get a toroidal or cylinder world, start from a fresh `world` folder.
- Fresh means the whole `world` folder. A half-cleared one is worse than an untouched one: with `level.dat` still in place and `data/minecraft/world_gen_settings.dat` deleted, the server neither loads the old world nor creates a new one — it stops on `Overworld settings missing` before the world type is ever read. Delete the folder, then put the datapack back into `world/datapacks/`.
- A datapack that declares `minecraft:overworld`, `minecraft:the_nether` or `minecraft:the_end` cannot take the shape away. Such a stem normally replaces the world's own generator for that dimension; here it is rebuilt on the shape the world was created with, so the pack's terrain choice applies and the world keeps looping. The `World shape:` line logged at startup reports `shape=restored` and names the pack's generator — `shape=stamp`, where that generator kept its own class and the shape was stamped onto it, or `shape=stored`, where it could take no shape at all and the world's own was kept.
