# Occultism Jars

An [Occultism](https://modrinth.com/mod/occultism) addon for NeoForge 1.21.1.

Adds a jar that traps a single Crusher spirit (Foliot / Djinni / Afrit / Marid) and
runs its crushing automatically. Items go in and out through hoppers and Pipez, so the
crushing stays contained instead of scattering dropped items around.

## Building

Requires JDK 21.

The build compiles against Occultism and JEI, so put their jars in `libs/`:

```
libs/occultism-1.21.1-neoforge-1.224.4.jar
libs/jei-1.21.1-neoforge-19.51.0.418.jar
```

Occultism is a required dependency; JEI is optional (used only for the recipe-catalyst hook).

Then:

```bash
./gradlew build
```

The jar ends up in `build/libs/`.

## Running in dev

```bash
./gradlew runClient
```
