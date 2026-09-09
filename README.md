# Occultism Jars

An [Occultism](https://modrinth.com/mod/occultism) addon for NeoForge 1.21.1.

Adds a jar that traps a single Crusher spirit (Foliot / Djinni / Afrit / Marid) and
runs its crushing automatically. Items go in and out through hoppers and Pipez, so the
crushing stays contained instead of scattering dropped items around.

## Building

Requires JDK 21.

The build compiles against Occultism, so put its jar in `libs/`:

```
libs/occultism-1.21.1-neoforge-1.224.4.jar
```

Then:

```bash
./gradlew build
```

The jar ends up in `build/libs/`.

## Running in dev

```bash
./gradlew runClient
```
