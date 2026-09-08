# How Toroidal World Works

The mod does four things: it gives the world a fixed width, makes the terrain repeat exactly at that width, measures every distance to the nearest copy of the target, and puts anything that walks off one side back in on the other.

## The shape lives inside the world

The width, and which axes loop, are stored in the world's chunk generators — the same place vanilla keeps its terrain settings. That is why the shape is picked on the world-creation screen (or with one `level-type` line on a server) and why an existing world cannot be converted into a shaped one, or back. When you join, the server sends your client the shape of every dimension, so the client measures the world the same way the server does. Every player on the server needs the mod installed.

## The terrain repeats instead of ending

Vanilla terrain comes from noise sampled on a grid of cells that runs on forever. The mod resizes that grid so a whole number of cells fits into one lap of the world, and wraps the sampling around the border. The block one step past the +X border is built from exactly the same numbers as the block at the −X border, so a mountain, a river or a biome continues through the seam. Structures, caves and the End islands come off the same wrapped noise.

One lap of a small world is short, so a full spread of climates may not fit into it and everything comes out one biome. That is what the **Compact biomes** option is for: it shrinks the biome noise until a full climate range fits into one lap, while coastlines and terrain keep their usual size.

## Distances are measured to the nearest copy

Picture the finished map tiled over an endless plane, every tile identical. The game asks distance questions all the time — how far to that player, which way to that block, is that mob in range. The mod answers each one with the nearest of those copies, in a single place that the rest of the game reads through.

That one answer is what makes the world behave as round rather than merely look it: mob AI and pathfinding, projectiles and explosions, sounds and particles, compasses and lodestones, villages and raids, distance-based advancements. A mod that measures distance in its own code does not get this for free — which mods have been folded across the seam is in the [supported mods list](supported-mods.md).

## Crossing the seam

Coordinates stay inside the world's bounds. An entity that steps over the +X border is moved to the −X side in the same tick, together with whatever it is riding, its speed and the path it was walking, so a mob crossing the border does not lose the target it was chasing. Your own laps are counted separately, which is how the circumnavigation advancement knows you went all the way around.

You see none of that happen, because everything the server sends your client — chunks, entity positions, block updates, waypoints — is rewritten to the copy nearest to you. A chunk on the far side of the border arrives at the coordinates right next to you instead of at the opposite end of the map.
