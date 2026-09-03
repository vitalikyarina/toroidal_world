# Running a Toroidal World on a Dedicated Server

A world's shape lives entirely in its chunk generators, so a dedicated server needs no screen: the mod ships the same named world sizes the create-world screen offers as world presets, for the toroidal world and for the cylinder, and one `server.properties` line picks one. The result is a fully shaped world — overworld, nether and End — that behaves exactly like one created in singleplayer. Joining players need the mod installed; they receive the world's bounds automatically on login.

## Setup

1. Put the mod jar into the server's `mods/` folder.
2. In `server.properties`, pick a preset — do this **before the first start**, the world type is read only when the world is created:

```properties
level-type=toroidal_world\:medium
```

3. Start the server. That's it — no datapack needed.

The five sizes match the create-world screen exactly; each comes as a toroidal preset and as a cylinder preset:

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

For a size outside the preset spread, define your own world preset in a datapack. Create it **before the first server start**:

```
world/datapacks/my-toroidal/
├── pack.mcmeta
└── data/
    └── my_pack/
        └── worldgen/
            └── world_preset/
                └── my_toroidal.json
```

The `world/` folder does not exist yet on a fresh server — create it with just the datapack inside; the server builds the rest around it and enables the pack automatically on first start. The preset resolves as `data/<namespace>/worldgen/world_preset/<file>.json` → `level-type=<namespace>:<file>`.

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
        }
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
        }
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
        }
      }
    }
  }
}
```

Then point `server.properties` at it: `level-type=my_pack\:my_toroidal`.

For a cylinder, give the looping axis its bounds and write the other axis as an empty object — the same axis in all three dimensions. This `wrapping` makes the overworld of a 64-chunk (1024-block) cylinder looping along Z:

```json
"wrapping": {
  "x": {},
  "z": { "min_chunk": -32, "max_chunk": 32 }
}
```

The empty object is required: an axis left out of `wrapping` is an error, not an endless axis.

### Size rules

`min_chunk`/`max_chunk` are a half-open chunk range: `-16 … 16` means 32 chunks (512 blocks), block bounds −256 … 255. The create-world screen enforces these rules for you; a hand-written preset must respect them on its own:

- **Overworld** — every looping axis at least 16 chunks (256 blocks), centered on zero as the example does (`min_chunk = -width/2`). A toroidal world loops both axes at the same width; a cylinder loops one axis and writes the other as `{}`.
- **Nether** — the overworld width divided by the portal scale, and the scale must divide it exactly; the nether itself must stay at least 16 chunks (256 blocks) wide. Vanilla's 1:8 portal ratio therefore needs an overworld of at least 128 chunks (2048 blocks). An uneven ratio breaks portal linking near the seam — this rule is not optional. In a cylinder the rule applies to the looping axis; the endless axis stays `{}` in the nether too.
- **End** — independent of the other two, at least 192 chunks (3072 blocks) on every looping axis, and in a cylinder looping along the same axis as the overworld. Smaller Ends lose the outer island ring — no end cities, no elytra — and let gateway teleports reach across the seam.

For a Superflat world use `"type": "toroidal_world:toroidal_flat"` with a `"settings"` object of the flat generator instead of the noise settings id.

## Notes

- `level-type` matters only at world creation. On an existing world it changes nothing; to get a toroidal or cylinder world, start from a fresh `world` folder.
- The world's bounds are stored inside the world itself (in its chunk generators), so after the first start the `level-type` line no longer decides anything.
- A datapack that declares `minecraft:overworld`, `minecraft:the_nether` or `minecraft:the_end` cannot take the shape away. Such a stem normally replaces the world's own generator for that dimension; here it is rebuilt on the shape the world was created with, so the pack's terrain choice applies and the world keeps looping. The `World shape:` line logged at startup reports `shape=restored` and names the pack's generator — or `shape=stored`, where that generator could not take a shape and the world's own was kept.
