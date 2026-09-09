# Toroidal World

A NeoForge and Fabric mod that gives the Minecraft world a finite shape with no edge — a seamless torus, where both horizontal axes loop, or a cylinder, where one loops and the other stays endless, with terrain, structures, mobs and gameplay continuous across the seam.

How the shape is made to work — terrain, distances and the seam crossing — is in [docs/how-it-works.md](docs/how-it-works.md).

## Versions

- Minecraft **1.21.1**
- NeoForge **21.1.248+**
- Fabric Loader **0.19.3+** with Fabric API **0.116.15+1.21.1**

## Dedicated server

One `level-type` line in `server.properties` turns a dedicated server into a toroidal world — see [docs/dedicated-server.md](docs/dedicated-server.md) for the presets, custom sizes and the size rules.

## For mod developers

`com.toroidalworld.api.v1` is the surface other mods read the world's shape through. `ToroidalWorldApi.shapeOf` answers for a server level, `ToroidalWorldClientApi.shapeOf` for the client level — the client is deliberately told the world is infinite, so it needs its own entry point. Both hand back a `ToroidalShape`: the looping axes and their spans, and the folds that turn a coordinate into the canonical one, the copy nearest a reference, or the shortest vector through the seam.

```groovy
repositories {
    maven { url = 'https://raw.githubusercontent.com/vitalikyarina/toroidal_world/maven/' }
}

dependencies {
    compileOnly 'com.toroidalworld:toroidal-world-api:<mod version>'
}
```

The artifact carries that one package, with sources and javadoc beside it. It follows semantic versioning against the mod version: within a major version, members are not removed or changed incompatibly. Everything outside the package is internal — it moves without notice, and mixins into it are unsupported.

Both walkthroughs, and which fold to reach for, are in [docs/modding.md](docs/modding.md).

## Building from source

```
./gradlew build
```

The jar lands in `build/libs/`. The Gradle wrapper provisions the required JDK itself.

## License

MIT — see [LICENSE](LICENSE).
