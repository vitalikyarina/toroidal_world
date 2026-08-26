# Toroidal World

A NeoForge and Fabric mod that gives the Minecraft world a finite shape with no edge — a seamless torus, where both horizontal axes loop, or a cylinder, where one loops and the other stays endless: cross the +X border and you arrive from the −X side, with terrain, structures, mobs and gameplay continuous across the seam.

## Versions

- Minecraft **1.21.1**
- NeoForge **21.1.248+**
- Fabric Loader **0.19.3+** with Fabric API **0.116.15+1.21.1**

## Dedicated server

One `level-type` line in `server.properties` turns a dedicated server into a toroidal world — see [docs/dedicated-server.md](docs/dedicated-server.md) for the presets, custom sizes and the size rules.

## Building from source

```
./gradlew build
```

The jar lands in `build/libs/`. The Gradle wrapper provisions the required JDK itself.

## License

MIT — see [LICENSE](LICENSE).
