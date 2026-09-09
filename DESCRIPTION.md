# Toroidal World

**Walk around the world.** The world has a finite size and no edge: cross the border on one side and you walk in from the opposite side — no barrier, no teleport, no visible seam. Or run an SMP where nobody can outrun anybody forever.

## What it does

- Adds a **World Shape** option to the world-creation screen — pick a shape and set the world size in chunks.
- Terrain is genuinely periodic — the noise itself wraps, so mountains, biomes and structures continue across the seam instead of hitting a mirrored wall or a cut-off cliff.
- The game keeps working across the seam, not just the blocks: mob AI and pathfinding, projectiles and explosions, sounds and particles, compasses and lodestones, villages and raids, distance-based advancements — they all treat the world as round.
- The Nether and the End follow the shape — the Nether scaled relative to the overworld, the End with its own size.
- Modded terrain and modded dimensions come along. A world-generation mod that changes vanilla's terrain settings keeps its terrain, wrapped; a modded dimension takes the world's shape as long as it generates the vanilla way, and one with a generator of its own is left as its mod makes it.

How it does that, in short: [how it works](https://github.com/vitalikyarina/toroidal_world/blob/main/docs/how-it-works.md).

## World shapes

- **Toroidal** — both horizontal axes loop.
- **Cylinder** — one horizontal axis loops, the other goes on forever like vanilla; you pick which axis loops, and the Nether and the End loop along the same one.

## Dedicated servers

One line in `server.properties` creates a shaped world: `level-type=toroidal_world:medium` for a toroidal one, `level-type=toroidal_world:cylinder_medium` for a cylinder, each in five sizes — `tiny`, `small`, `medium`, `large`, `huge`. Custom sizes and the full walkthrough are in the [dedicated server guide](https://github.com/vitalikyarina/toroidal_world/blob/main/docs/dedicated-server.md).

## Compatibility

Runs on **NeoForge** and **Fabric**.

- [Sodium](https://modrinth.com/mod/sodium) and [Iris Shaders](https://modrinth.com/mod/iris).
- [JourneyMap](https://modrinth.com/mod/journeymap) 6.0.5 or newer, [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) and [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map).
- [Distant Horizons](https://modrinth.com/mod/distanthorizons) — the far side of the world shows across the seam, out to half the world width.

Every mod checked against the world's shape, with the game versions and loaders each one applies to, is in the [supported mods list](https://github.com/vitalikyarina/toroidal_world/blob/main/docs/supported-mods.md).

## Before you download

- The world shape is chosen **at world creation** — an existing vanilla world cannot be converted into a toroidal or cylinder one (or back).
- The mod is pre-1.0: world compatibility between mod versions is not guaranteed until 1.0.

## Bug reports

Found something broken — especially anything that behaves differently near the seam? Report it on the [issue tracker](https://github.com/vitalikyarina/toroidal_world/issues).
