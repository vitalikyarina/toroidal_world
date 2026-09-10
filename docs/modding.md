# Toroidal World from Another Mod

Toroidal World exposes one package to other mods: `com.toroidalworld.api.v1`. It does two things. It answers what the world's shape is and folds coordinates into it, so a mod that measures distance, keys storage by position or draws a marker keeps working when the world loops. And it lets a mod declare a world shape of its own, which the player then picks on the create-world screen beside the ones this mod ships.

Everything outside that package is internal — it moves between releases without notice, and mixins into it are unsupported.

Reading a shape is the first half of this page; declaring one starts at [Declaring a world shape](#declaring-a-world-shape).

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

## Declaring a world shape

A shape is one declaration: an id, the settings it starts from, what it writes into the world's dimensions at creation, and how it reads itself back out of an existing world.

```java
public static final ShapeModule<BandSettings> MODULE = ShapeModule.of(
        Identifier.fromNamespaceAndPath("your_mod", "band"),
        BandSettings.DEFAULT,
        BandDimensions::apply,
        BandDimensions::read);
```

`MODULE.register()` enrols it. Call that from your mod's initialiser: the registry closes at `MinecraftServer.runServer`, before the levels load, and a `register` past that throws instead of being silently half-effective.

The settings type is yours and Toroidal World never looks inside it. `MODULE.settings()` is what a Customize screen reads and `MODULE.settings(chosen)` what it writes back; the module holds the player's choice until the world is made.

The shape's name and tooltip come from `gui.<namespace>.world_shape.<path>` and that key plus `.hint` — for the module above, `gui.your_mod.world_shape.band` and `gui.your_mod.world_shape.band.hint`.

### The geometry it declares

`LoopSpans` is the whole of it: which horizontal axes loop and over what span of chunks. Spans are half-open — `minChunk` is the first chunk inside the world, `maxChunk` the first one past it.

```java
LoopSpans.ofWidth(24);                        // both axes, 24 chunks, centred on the origin
LoopSpans.ofWidth(Direction.Axis.Z, 24);      // Z alone; X runs to the vanilla world border
LoopSpans.of(Direction.Axis.X, -12, 12);      // an explicit span
```

Which flat surface the world ends up being is read off the spans, never declared: both axes give a torus, one gives a cylinder, neither gives an ordinary world. `scaledDown(scale)` divides every looping axis and re-centres — how the nether is normally derived from the overworld, so the vanilla portal ratio still lands inside the world.

`ShapeDimensions` writes those spans into the three vanilla stems, and reads them back:

```java
public static WorldDimensions apply(WorldDimensions dimensions, BandSettings settings) {
    LoopSpans spans = settings.spans();
    return ShapeDimensions.withSpans(dimensions, spans, spans.scaledDown(8), spans, settings.options());
}

public static @Nullable BandSettings read(WorldDimensions dimensions) {
    LoopSpans spans = ShapeDimensions.spansOf(dimensions, LevelStem.OVERWORLD);
    if (spans == null || spans.loops(Direction.Axis.X) || !spans.loops(Direction.Axis.Z)) {
        return null;
    }

    return new BandSettings(spans, ShapeDimensions.optionsOf(dimensions, LevelStem.OVERWORLD));
}
```

`read` is asked of every registered shape when an existing world is opened, and the first one to answer non-null owns it. So refuse anything you did not write: check which axes loop, not merely that a shape is there. `spansOf` answers `null` for a stem carrying no shape of ours, and for one whose axes do not fold independently — geometry this API cannot declare.

The dimensions handed to `apply` have had any earlier shape stripped, so state your whole geometry rather than amending someone else's. A stem whose generator cannot take a shape is left alone.

### The settings screen

`ShapeCustomizers` maps the shape's id to the screen behind the Customize button; a shape with no customizer registered leaves that button dark. Register it from the client side only.

```java
ShapeCustomizers.register(MODULE.id(), parent -> new BandSettingsScreen(parent, MODULE.settings(), MODULE::settings));
```

## Declaring a world option

A world option is a value the player picks at creation, stored in the world and read back at load. It is a declaration, not a screen: the codec key, where it sits among the other options, its codec and its default.

```java
public static final WorldOption<Boolean> OPTION =
        new WorldOption<>("band_floor", 100, Codec.BOOL, Boolean.FALSE);

WorldOptions.register(OPTION);
```

The key is written into the world's generators and must be unique across every mod. `position` orders the option against the others on a settings screen; equal positions fall back to the key.

`GenerationOptions` is the value set a world carries — `get(option)` to read, `with(option, value)` to write, `GenerationOptions.DEFAULT` to start from. Only a value that differs from its option's default is written to the world file, so an option nobody touched costs nothing on disk.

The client half is one control per option:

```java
WorldOptionControls.register(OPTION, BandFloorControl::new);
```

A `WorldOptionControl` adds its own widgets, commits its value into the options, and may veto **Done** while its value is unusable. It reaches the screen around it only through `WorldOptionContext` — the parent screen, the loop width the screen states, the current options, a change signal and a rebuild request. A shape's settings screen builds them all with `WorldOptionControls.createAll(context)`, in registry order.

## Hooking a generation moment

`GenerationHooks.atRandomState(key, hook)` runs a hook as a level's `RandomState` finishes building — the moment its noise router exists and nothing has sampled it yet, so rewriting a noise in that router still reaches every chunk.

```java
GenerationHooks.atRandomState("band_floor", (randomState, shape, options, seaLevel) -> {
    if (shape.loops(Direction.Axis.X) || !shape.loops(Direction.Axis.Z) || !options.get(OPTION)) {
        return;
    }

    liftTheCoastNoise(randomState.router(), shape.widthBlocks(Direction.Axis.Z));
});
```

**A hook gates itself.** Every registered hook runs for every folding level of every world, whatever shape made it and whichever mod declared that shape — a hook is not scoped to the shape it was registered beside. A world builds one `RandomState` per dimension, so it runs once for the overworld, once for the nether and once for the End. Read `shape` and `options` and return early unless both are what the hook is for; reaching for a span on an axis that does not loop throws.

The key orders the hooks against one another and, like an option key, must be unique across every mod. Registration closes at the same boundary as everything else.
