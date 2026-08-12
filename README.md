# Toroidal World

A NeoForge mod that makes the Minecraft world a seamless torus: the world has a finite size and no edge — cross the +X border and you arrive from the −X side, with terrain, structures, mobs and gameplay continuous across the seam.

The player-facing description (the Modrinth page source) lives in [DESCRIPTION.md](DESCRIPTION.md).

Running a server? [docs/dedicated-server.md](docs/dedicated-server.md) — one `level-type` line turns a dedicated server into a toroidal world.

## Versions

- Minecraft **26.2**, NeoForge **26.2.0.45-beta+**.

## Building from source

```
./gradlew build
```

The jar lands in `build/libs/`. The Gradle wrapper provisions the required JDK itself.

## License

MIT — see [LICENSE](LICENSE).
