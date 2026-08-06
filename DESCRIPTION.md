# Toroidal World

**The world is a torus.** It has a finite size — and no edge. Walk past the +X border and you arrive from the −X side, with the terrain, mobs and gameplay continuing seamlessly: no barrier, no teleport, no visible seam. Circumnavigate a world on foot, or run an SMP where nobody can outrun anybody forever.

## What it does

- Adds a **World Shape** option to the world-creation screen: **Toroidal** (a normal generated world) and **Toroidal Flat** (superflat), with the world size configurable per axis in chunks.
- The terrain is genuinely periodic — the noise itself wraps, so terrain, biomes and structures continue across the seam instead of hitting a mirrored wall or a cut-off mountain.
- The game keeps working across the seam, not just the blocks: mob AI and pathfinding, projectiles and explosions, sounds and particles, compasses and lodestones, villages and raids, distance-based advancements — they all treat the world as round.

## Compatibility

- [Sodium](https://modrinth.com/mod/sodium) and [Iris Shaders](https://modrinth.com/mod/iris) are supported.
- LOD mods (**Distant Horizons**) are **not supported** yet.
- Requires **NeoForge 26.2.0.45-beta+**.

## Before you download

- The world shape is chosen **at world creation** — an existing vanilla world cannot be converted into a toroidal one (or back).
- The mod is pre-1.0: world compatibility between mod versions is not guaranteed until 1.0.

## Bug reports

Found something broken — especially anything that behaves differently near the seam? Report it on the [issue tracker](https://github.com/vitalikyarina/toroidal_world/issues).
