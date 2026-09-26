# Occultism Jars

[![build](https://github.com/Ribeiro-73/occultism-jars/actions/workflows/build.yml/badge.svg)](https://github.com/Ribeiro-73/occultism-jars/actions/workflows/build.yml)

An [Occultism](https://modrinth.com/mod/occultism) addon for NeoForge 1.21.1.

Working spirits (crushers, smelters, crystallizers and traders) do their job inside a
block instead of wandering around and scattering dropped items.

Full documentation — setup, automation, config, tuning and an FAQ — is on the
**[wiki](https://github.com/Ribeiro-73/occultism-jars/wiki)**.

## What it does

- **Spirit Jar.** Right-click a ritual-summoned working spirit with an empty jar
  to trap it. A jar only holds the lower half of a job's tiers, so Foliot and
  Djinni. Shift + right-click the placed jar with an empty hand to release it.
- **Holographic Base.** A two-block stand with a gem holder. Right-click it with a
  Soul Gem or Trinity Gem holding a working spirit of any tier; the spirit shows
  as a purple hologram and gets to work. Shift + right-click with an empty hand
  takes the gem back.
- **Jobs.** Crushers use `occultism:crushing`, crystallizers use
  `occultism:crystallize`, smelters use furnace, blast furnace, smoker and campfire
  recipes, traders (Otherstone, Otherrock, sapling and the Gambler) use
  `occultism:spirit_trade` and only work on the base. A Demonic Wife or Husband on
  the base gives its owner (anywhere on the server) potion and suspicious stew
  effects that last much longer, waiting until they are about to run out before
  using the next one; it also cooks raw food and turns Cursed Honey into a Sweet
  Honey Heart. Harmful and instant effects are ignored. Only items the spirit can
  actually process go in.
- **Spirit Fire Chamber.** An otherstone, otherrock and gold frame that keeps Occultism's spirit fire burning.
  Right-click it with Datura, then light it with a flint and steel; chalk changes
  the colour, like the real fire. Hoppers and pipes feed it and the
  `occultism:spirit_fire` result is ready to pull out at once. Dropping items on
  it does nothing. Broken, it keeps its fire and colour.
- **Same numbers as the real spirits.** Speed, output multiplier and operation
  count are read live from Occultism's own `occultism-server.toml`
  (`[spirit_job]`), so tuning the config affects both blocks too.
- **Automation.** Hoppers, Pipez and other item pipes insert into the input slot
  and pull from the two output slots. Both halves of the base connect.
- **Screen.** Right-click opens it: the input and output slots, a portrait of the
  spirit, and a progress bar.
- **Crafting.** All three are made by ritual: the jar and the chamber on the Foliot
  summoning pentacle with a bound Foliot book, the base on the Marid one with a
  bound Marid book.
- **JEI:** both blocks are catalysts for crushing and crystallizing, and the
  recipe-transfer (`+`) button fills the input slot, cooking recipes included.

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
load the addon. More detail, including the stored-spirit data component, is on the
[For Developers](https://github.com/Ribeiro-73/occultism-jars/wiki/For-Developers)
wiki page.

## License

MIT — see [LICENSE](LICENSE).
