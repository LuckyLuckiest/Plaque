# Plaque

Standalone scoreboard plugin for Spigot (Keystone-powered), extracted from Gangland Warfare (decoupling wave WS1,
2026-09-16). Renders a `scoreboard.yml`-driven FastBoard sidebar with no Gangland dependency — gang/user data
reaches it only as text, through real PlaceholderAPI `%gangland_*%` tokens (plus an optional Keystone
`PlaceholderProvider` fallback, landing in G2).

## Status

- **G0** (repo skeleton, pom, `plugin.yml`, `mvn clean install` green) — done.
- **G1** (rendering engine ported: `Board`, `DriverHandler`, `DriverV3`, `Line`/`StaticLine`, `BoardAddon`; UI-17
  guard added; UI-16 gone with `DriverV1`/`DriverV2`; off the deprecated `Placeholder` interface onto
  `PlaceholderProvider`; tests ported) — done.
- **G2** (bootstrap on Keystone beans, config loading, the PAPI → `PlaceholderProvider` → raw-text placeholder
  chain, the enable-time board sweep, `/plaque reload`, docs) — not started.

See `CLAUDE.md` for the package map, the fixes applied during the port, and the silent-migration warning around
`Driver:` values. The binding plan lives in the Gangland repo:
`brainstorming/decoupling-wave-2026-09-14/plans/WS1-scoreboard.md`.

## Build

```
mvn clean install                # target/Plaque-<version>.jar
mvn clean install -DskipTests    # skip tests
```

## Config compatibility

`scoreboard.yml`'s schema is kept byte-identical to Gangland's today (landing in G2) — an admin copies
`plugins/Gangland_Warfare/scoreboard.yml` to `plugins/Plaque/scoreboard.yml` verbatim. The one behavioral change:
literal `%gangland_*%` tokens now need PlaceholderAPI (or Gangland's optional `ServicesManager` fallback) to
resolve — Gangland's own in-process placeholder chain no longer applies once the board is a separate process.
