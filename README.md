# Occultism Jars

[![build](https://github.com/Ribeiro-73/occultism-jars/actions/workflows/build.yml/badge.svg)](https://github.com/Ribeiro-73/occultism-jars/actions/workflows/build.yml)

An [Occultism](https://modrinth.com/mod/occultism) addon for NeoForge 1.21.1.

A glass jar traps a single Crusher spirit and runs its crushing automatically, so
the ore processing stays inside one block instead of a spirit wandering around and
scattering dropped items.

## What it does

- **Catch a crusher.** Right-click a ritual-summoned Crusher spirit
  (Foliot / Djinni / Afrit / Marid) with an empty jar to absorb it. The captured
  spirit shows through the glass.
- **Place it and feed it.** A hopper or Pipez on top drops ore into the jar; one
  below pulls the crushed result out. Only items that actually have an
  `occultism:crushing` recipe for that tier are accepted.
- **Same numbers as the real crusher.** Speed, output multiplier and operation
  count are read live from Occultism's own `occultism-server.toml`
  (`[spirit_job] crusher_tier1..4`), so tuning the config affects the jars too.
- **Screen.** Shift + right-click opens it: the input and output slots, a portrait
  of the trapped spirit, and a progress bar.
- **Get the spirit back.** Right-click the jar with the Demon Extractor Blade to
  release the crusher. It comes back holding whatever it was mid-crush and keeps
  going where it lands.
- Breaking the jar drops its contents; the trapped spirit stays with the item.
- **JEI:** the jar is a catalyst for the Crushing category, and the recipe-transfer
  (`+`) button fills the input slot.

## Building

Requires JDK 21. Occultism and JEI are pulled from their mavens (Modrinth and
BlameJared), so a plain

```bash
./gradlew build
```

produces the jar in `build/libs/`. Occultism is a required runtime dependency;
JEI is optional (only the JEI hook uses it).

## Running in dev

```bash
./gradlew runClient
```

Occultism and its dependencies need to be in `run/client/mods/` for `runClient` to
load the addon.

## License

MIT — see [LICENSE](LICENSE).
