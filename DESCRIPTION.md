# Toroidal World

**Walk around the world.** The world has a finite size and no edge: cross the border on one side and you walk in from the opposite side — no barrier, no teleport, no visible seam. Go around the world on foot, or run an SMP where nobody can outrun anybody forever.

## What it does

- Adds a **World Shape** option to the world-creation screen — pick a shape and set the world size in chunks.
- Terrain is genuinely periodic — the noise itself wraps, so mountains, biomes and structures continue across the seam instead of hitting a mirrored wall or a cut-off cliff.
- The game keeps working across the seam, not just the blocks: mob AI and pathfinding, projectiles and explosions, sounds and particles, compasses and lodestones, villages and raids, distance-based advancements — they all treat the world as round.
- The Nether and the End follow the shape — the Nether scaled relative to the overworld, the End with its own size.

## World shapes

- **Toroidal** — both horizontal axes loop: walk far enough in any direction and you come back to where you started.
- **Cylinder** — one horizontal axis loops, the other goes on forever like vanilla; you pick which axis loops, and the Nether and the End loop along the same one.

## Dedicated servers

One line in `server.properties` creates a shaped world: `level-type=toroidal_world:medium` for a toroidal one, `level-type=toroidal_world:cylinder_medium` for a cylinder, each in five sizes — `tiny`, `small`, `medium`, `large`, `huge`. Custom sizes and the full walkthrough are in the [dedicated server guide](https://github.com/vitalikyarina/toroidal_world/blob/main/docs/dedicated-server.md).

## Compatibility

Runs on **NeoForge** and **Fabric**.

**Supported**

- [Sodium](https://modrinth.com/mod/sodium) and [Iris Shaders](https://modrinth.com/mod/iris).
- [Lithium](https://modrinth.com/mod/lithium) — two of its optimizations are switched off automatically.
- [C2ME](https://modrinth.com/mod/c2me-fabric).
- [JourneyMap](https://modrinth.com/mod/journeymap), [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) and [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map).
- [Create](https://modrinth.com/mod/create) (1.21.1 only).

**Partially supported**

- LOD mods (**Distant Horizons**, **Voxy**) — distant terrain is not wrapped across the seam yet.

## Before you download

- The world shape is chosen **at world creation** — an existing vanilla world cannot be converted into a toroidal or cylinder one (or back).
- The mod is pre-1.0: world compatibility between mod versions is not guaranteed until 1.0.

## Bug reports

Found something broken — especially anything that behaves differently near the seam? Report it on the [issue tracker](https://github.com/vitalikyarina/toroidal_world/issues).
