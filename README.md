# Plaque

Standalone scoreboard plugin for Spigot (Keystone-powered), extracted from Gangland Warfare (decoupling wave WS1,
2026-09-16). Renders a `scoreboard.yml`-driven FastBoard sidebar with no Gangland dependency — gang/user data
reaches it only as text, through real PlaceholderAPI `%gangland_*%` tokens, with an optional Keystone
`PlaceholderProvider` fallback another plugin may publish on the `ServicesManager`.

## Status

- **G0** (repo skeleton, pom, `plugin.yml`, `mvn clean install` green) — done.
- **G1** (rendering engine ported: `Board`, `DriverHandler`, `DriverV3`, `Line`/`StaticLine`, `BoardAddon`; UI-17
  guard added; UI-16 gone with `DriverV1`/`DriverV2`; off the deprecated `Placeholder` interface onto
  `PlaceholderProvider`; tests ported) — done.
- **G2** (bootstrap on Keystone beans via `PlaqueContext`, `settings.yml`/`scoreboard.yml` config loading, the
  PAPI → `ServicesManager PlaceholderProvider` → raw-text placeholder chain, the enable-time/reload board sweep,
  `/plaque reload` as a keystone-command `Command`) — done.

See `CLAUDE.md` for the package map, the fixes applied during the port, the silent-migration warning around
`Driver:` values, and why `/plaque reload` uses keystone-command rather than a plain `CommandExecutor` (WS1-D2,
a user decision that overrides an earlier draft of the plan document). The binding plan lives in the Gangland
repo: `brainstorming/decoupling-wave-2026-09-14/plans/WS1-scoreboard.md`.

## Build

```
mvn clean install                # target/Plaque-<version>.jar
mvn clean install -DskipTests    # skip tests
```

## Running it

Drop `Plaque-<version>.jar` into `plugins/` beside `Keystone-1.9.2.jar` (`depend: [Keystone]`, no other hard
dependency). `PlaceholderAPI` and `ViaVersion` are both soft dependencies — the plugin boots and shows static
lines without either, but logs a startup warning if PlaceholderAPI is missing (dynamic `%gangland_*%`-style
tokens render literally until it's installed, or until another plugin publishes a Keystone `PlaceholderProvider`
on the `ServicesManager`). `/plaque reload` (alias `/plq reload`, permission `plaque.command.main`) reloads
`settings.yml`/`scoreboard.yml` and rebuilds every online player's board.

PlaceholderAPI presence is detected once, at enable — `/plaque reload` does **not** re-check for it. Installing
PlaceholderAPI after Plaque has already started needs a server restart (or a full plugin reload via a plugin
manager) before `%gangland_*%`-style tokens start resolving through it; the `ServicesManager PlaceholderProvider`
fallback has no such limitation, since it is looked up fresh on every render.

## Config compatibility

`scoreboard.yml`'s schema is kept byte-identical to Gangland's — an admin copies
`plugins/Gangland_Warfare/scoreboard.yml` to `plugins/Plaque/scoreboard.yml` verbatim. `settings.yml`'s `Enable`/
`Driver` keys are the same two values that lived under Gangland's `settings.yml` `Scoreboard:` block, just at the
top level of their own file now. The one behavioral change: literal `%gangland_*%` tokens now need PlaceholderAPI
(or the `ServicesManager` `PlaceholderProvider` fallback) to resolve — Gangland's own in-process placeholder
chain no longer applies once the board is a separate process.
