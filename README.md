# Toroidal World

A NeoForge and Fabric mod that gives the Minecraft world a finite shape with no edge — a seamless torus, where both horizontal axes loop, or a cylinder, where one loops and the other stays endless, with terrain, structures, mobs and gameplay continuous across the seam.

How the shape is made to work — terrain, distances and the seam crossing — is in [docs/how-it-works.md](docs/how-it-works.md).

## Versions

- Minecraft **26.1.2**
- NeoForge **26.1.2.94+**
- Fabric Loader **0.19.3+** with Fabric API **0.141.3+26.1**

## Dedicated server

One `level-type` line in `server.properties` turns a dedicated server into a toroidal world — see [docs/dedicated-server.md](docs/dedicated-server.md) for the presets, custom sizes and the size rules.

## For mod developers

`com.toroidalworld.api.v1` is the surface other mods read the world's shape through, make their own mechanisms work on it through, and declare a shape of their own through.

**Reading a shape.** `ToroidalWorldApi.shapeOf` answers for a server level, `ToroidalWorldClientApi.shapeOf` for the client level — the client is deliberately told the world is infinite, so it needs its own entry point. Both hand back a `ToroidalShape`: the looping axes and their spans, and the folds that turn a coordinate into the canonical one, the copy nearest a reference, or the shortest vector through the seam.

**Declaring one.** A `ShapeModule` is a whole world shape — an id, its settings, and what it writes into the world's dimensions through `ShapeDimensions.withSpans`; it appears on the create-world screen beside the torus and the cylinder, which register the same way. `WorldOption` declares a value the player picks there and the world stores; `GenerationHooks.atRandomState` runs a mod's own code at the moment a folding level's noise router is built.

**Making your own mechanism cross the seam.** Every vanilla packet carrying a world position is already rewritten between the server's frame and the client's; a payload or a particle type of yours is not, because nothing but you knows where a position sits inside it. `PacketRewriters` is where you say how — the rewriter is handed a `SeamContext` for the connection the packet is on, and moves what it knows to move. `ClientAnchors` seats a position against the anchors a client already has: its player, its camera, and the copy it is actually holding. And `ToroidalShape.shiftToNearestCopy` hands out one `SeamShift` for a whole rigid group — a contraption, a vehicle, a multiblock — which arrives in one piece where seating its members one by one would tear it in half.

```groovy
repositories {
    maven { url = 'https://raw.githubusercontent.com/vitalikyarina/toroidal_world/maven/' }
}

dependencies {
    compileOnly 'com.toroidalworld:toroidal-world-api:<mod version>'
}
```

The artifact carries that one package, with sources and javadoc beside it. It follows semantic versioning against the mod version: within a major version, members are not removed or changed incompatibly. Everything outside the package is internal — it moves without notice, and mixins into it are unsupported.

Every walkthrough — reading, declaring, and which fold to reach for — is in [docs/modding.md](docs/modding.md).

## Building from source

```
./gradlew build
```

The jar lands in `build/libs/`. The Gradle wrapper provisions the required JDK itself.

## License

MIT — see [LICENSE](LICENSE).
