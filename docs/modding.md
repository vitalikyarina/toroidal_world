# Reading a Toroidal World from Another Mod

Toroidal World exposes one package to other mods: `com.toroidalworld.api.v1`. It answers what the world's shape is and folds coordinates into it, so a mod that measures distance, keys storage by position or draws a marker keeps working when the world loops.

Everything outside that package is internal — it moves between releases without notice, and mixins into it are unsupported.

## Depending on it

```groovy
repositories {
    maven {
        name = 'Toroidal World'
        url = 'https://raw.githubusercontent.com/vitalikyarina/toroidal_world/maven/'
    }
}

dependencies {
    compileOnly 'com.toroidalworld:toroidal-world-api:<mod version>'
}
```

The API version is the mod version; `maven-metadata.xml` under the coordinate lists what is published. Sources and javadoc are published beside the jar.

`compileOnly` is the point: the artifact carries the API package alone, and at runtime the classes come from the Toroidal World the player installed. Declare the mod as an optional dependency of yours and check that it is loaded before you touch any `com.toroidalworld` class — the mod id is `toroidal_world`. A class that names one of them fails to load when the mod is absent, so keep those calls in a class you reach only after the check, not in a field initialiser of a class that always loads.

## Server side

`ToroidalWorldApi.shapeOf(level)` answers the geometry of a level whose own engine knows its bounds — a `ServerLevel`, or anything delegating to one. It is empty when no axis of that level loops, which is the normal answer on a world that is not toroidal.

```java
Optional<ToroidalShape> shape = ToroidalWorldApi.shapeOf(level);
if (shape.isEmpty()) {
    return distance(from, to);
}
```

The view is immutable and cheap; hold it for as long as the level lives.

`ToroidalWorldApi.travelOf(player, axis)` answers how far a player has travelled toward the next lap of the dimension they are in, in blocks, signed with the direction of travel. Pacing back and forth cancels out, crossing the seam does not count as a world width, and one whole width comes off each time a lap closes.

## Client side

A client level's own engine believes the world is infinite. That is deliberate — it is what keeps vanilla rendering and chunk loading working across the seam — and it is why `ToroidalWorldApi.shapeOf` answers empty for a `ClientLevel` even in a world that loops. The bounds the server declared ride apart on the client level, and `ToroidalWorldClientApi` is what reads them:

```java
Optional<ToroidalShape> shape = ToroidalWorldClientApi.shapeOf(clientLevel);
```

Every looping level arrives on login and on dimension change, before the first chunk, so a shape is there by the time anything is drawn. `ToroidalWorldClientApi.shapeOf(dimension)` takes a `ResourceKey<Level>` instead and answers for a dimension the player is not standing in — what a fullscreen map browsing another dimension folds by.

Client coordinates near the seam run whole world widths away from the server's, because the client holds whichever copy it was sent. Fold before you key anything by position, and measure against the player rather than against raw coordinates.

## Which operation to reach for

`ToroidalShape` offers three ways to move a coordinate, and they are not interchangeable:

- **`fold`** — the canonical position inside the world's bounds. Use it wherever a position becomes a key: a map, a set, a saved record, a hash. Two coordinates a lap apart are the same place, and only `fold` makes them the same key.
- **`nearestCopy(ref, target)`** — the copy of `target` closest to `ref`, one axis at a time. Use it wherever something is drawn or compared: a renderer, a range check, a squared distance. Something just across the seam reads as beside the reference instead of a world away.
- **`shortestDelta(from, to)`** — the vector from one to the other, measured through the seam where that is shorter. Use it wherever a direction or a signed distance is the answer: a waypoint arrow, a compass, a distance readout. It equals `nearestCopy(from, to).subtract(from)`.

Ask `loops(axis)` before any per-axis member — `minChunk`, `maxBlock`, `widthChunks` and the rest throw `IllegalArgumentException` on an axis that does not loop, and `Direction.Axis.Y` never loops.

## The `==` guarantee

Every fold hands back the argument instance itself when it moved nothing: `fold(pos) == pos`, `nearestCopy(ref, target) == target`, and likewise for the `value()` inside an `Oriented` result. A caller may read that `==` as "nothing moved" and skip the copy, the allocation or the write it would otherwise make:

```java
BlockPos folded = shape.fold(pos);
if (folded != pos) {
    rebuildWhateverWasKeyedBy(pos, folded);
}
```

`shortestDelta` is the one exception — its result is neither of its arguments, so it can only answer by value.

## The two capability flags

`ToroidalShape` carries two flags that describe folds this mod does not yet hand out. Every shape a player can create today — the torus and the cylinder — answers `true` to both, because the engine refuses to build a fold that answers `false`. They are in the API so that code written now survives the day one lands.

`decomposesPerAxis()` is false where crossing a seam on one axis moves or flips the other. There the per-axis members — `foldCoord`, `foldBlock`, `foldChunk`, `nearestCoord` — throw `IllegalStateException`, because one axis cannot be folded without the other; only the whole-position folds answer correctly. Reach for `fold(pos)` over `foldBlock(axis, coord)` where either will do, and the question never arises.

`preservesLocalIndices()` is false on a shape whose seam mirrors, where crossing it reverses a position's index inside its own chunk. Anything keyed by the low bits of a coordinate must be rebuilt across such a fold rather than carried over.

Across a mirrored seam a direction, a velocity or an offset that travelled with a position points the wrong way. The `Oriented` forms — `foldOriented`, `nearestCopyOriented` — report what the fold did, and `applyToDelta` carries a vector through the same turn:

```java
ToroidalShape.Oriented<Vec3> folded = shape.foldOriented(position);
Vec3 heading = folded.orientation().applyToDelta(velocity);
```

`Orientation.IDENTITY` is the only orientation an unmirrored shape ever reports — which today means the only one you will see — and `preservesHandedness()` tells a half turn from a genuine mirror.
