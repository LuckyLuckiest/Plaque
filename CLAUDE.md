# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What Plaque is

A standalone Spigot plugin that renders a `scoreboard.yml`-driven FastBoard sidebar, split out of Gangland
Warfare's `gangland-ui/scoreboard-api` module (decoupling wave WS1, 2026-09-16 — see
`brainstorming/decoupling-wave-2026-09-14/plans/WS1-scoreboard.md` in the Gangland repo for the full plan,
including the workstream's decision register and docket dispositions). Single Maven module (`packaging: jar`, no
`-api` split — nobody outside this plugin calls it programmatically; `%gangland_*%`-style tokens through
PlaceholderAPI are the only cross-plugin surface). Depends on Keystone (`org.luckyraven:keystone-*`) at
`provided` scope, pinned to **1.9.2** (independent of Keystone's own current branch tip).

## Server platform

**Spigot only — not Paper.** Do NOT use Paper-specific APIs (e.g. `io.papermc.paper.event.player.PlayerJumpEvent`
or any `io.papermc.*` import). Use Spigot/Bukkit equivalents instead. API floor is Spigot 1.16.5, matching
Keystone's own floor.

## Package map (as built, G0/G1)

- `org.luckyraven.plaque.Plaque` — the `JavaPlugin` entry point. A G0/G1 stub: `onEnable`/`onDisable` only.
  Bootstrap wiring (a `BeanFactory`-driven context, `settings.yml`/`scoreboard.yml` loading, the placeholder
  chain, the enable-time board sweep, `/plaque reload`) lands in G2.
- `org.luckyraven.plaque.board` — the rendering engine, ported verbatim (package rename only) from Gangland's
  `gangland-ui/scoreboard-api`, with two fixes applied at port time (see "Deviations from upstream" below):
  - `Board` (was `Scoreboard`) — owns the per-tick `RepeatingTimer` and start/stop lifecycle (UI-02 fix intact:
    `timer.start(false)`, never `start(true)`).
  - `board.driver.DriverHandler` — abstract base; owns the live `FastBoard` instance and the ViaVersion-aware
    `FastBoardImpl.hasLinesMaxLength()` override (works for every driver, not just V3 — ViaVersion coupling is
    repo-wide, not driver-specific).
  - `board.driver.version.DriverV3` — the **only** driver ported (WS1 decision D1(b)). `DriverV1`/`DriverV2` and
    the reflection-based `getDrivers()` scan were deleted, not carried forward — see "Silent migration" below.
  - `board.part.Line` / `board.part.StaticLine` — per-board line state with rotation-cursor content cycling
    (UI-01 fix intact: `Line#copy()`/`copyAll()` give every board its own instances).
  - `board.configuration.BoardAddon` (was `ScoreboardAddon`) — a `FileInitializer` that parses `scoreboard.yml`
    into `Line`/`StaticLine` instances. Not yet wired to a `FileManager` bean — that's G2.

## Deviations from upstream Gangland (applied during the G1 port, not carried in unfixed)

- **UI-17 (fixed):** `Line.getCurrentContent()`/`Line.update()` used to throw `IndexOutOfBoundsException` /
  `ArithmeticException` on an empty `Lines:` list (a misconfigured board). Both now guard with an early
  `if (contents.isEmpty()) return "";`. Pinned by `LineTest.emptyContents_doesNotThrowAndReturnsEmptyString`.
- **Off the deprecated `Placeholder` interface:** `Line.update(...)`, `DriverHandler.updateLine(...)` and
  `DriverV3`'s constructor took `org.luckyraven.keystone.util.Placeholder` (`@Deprecated(since = "1.3.0")`,
  single method `convert(Player, String)`). All three now take
  `org.luckyraven.keystone.placeholder.PlaceholderProvider` (`resolve(OfflinePlayer, String)`) instead — the
  unified SPI the rest of the Keystone ecosystem is migrating onto. `Player extends OfflinePlayer`, so every call
  site that used to pass a `Player` is unchanged.
- **UI-01/UI-02 (already fixed upstream, ported as-is):** per-board `Line` copies and the synchronous
  `RepeatingTimer` — do not "re-fix" these, they are already correct.

## Silent migration warning (WS1 decision D1)

Only `DriverV3` exists in this repo. Gangland's `ScoreboardManager.java:66` (pre-split) defaulted an
**unrecognised** `Driver:` value to `DriverV1`, not V3. A server admin copying an old `settings.yml` that still
says `Driver: Driver_V1` or `Driver_V2` must **not** be silently handed V1 (which had UI-16, now deleted code) —
G2's `BoardManager`/`BoardSettings` must explicitly default to `DriverV3` and log a warning on any unrecognised
value. Do not reintroduce a silent multi-driver fallback.

## Cross-plugin placeholder chain (G2, not yet built)

Per WS1-D5/A5: PAPI first, then an optional Keystone `PlaceholderProvider` Gangland publishes on Bukkit's
`ServicesManager` (`GanglandPlaceholder.asProvider()`, the same seam `ItemVocabulary` already uses), then raw
text last. `FlashPlaceholderWrapper.currentTick` is a **static field in the shared, unrelocated `keystone-common`
jar** — this plugin's `Board`/`DriverV3` write it every tick, Gangland's `GanglandPlaceholder` reads it for
`%gangland_flashif:...%`. This only works because Keystone is `provided` (not shaded) on both sides of the
process boundary; do not relocate `org.luckyraven.keystone.*` in this plugin's shade config.

## Build Commands

```
mvn clean install                # target/Plaque-<version>.jar
mvn clean install -DskipTests    # skip tests
```

Keystone (`org.luckyraven:keystone-*`) must already be installed to `~/.m2` before this reactor resolves — build
it with `mvn clean install` in the sibling `Keystone` repo first (or use the version already cached at 1.9.2).

## Code Style

### Method braces

Every method body must have its opening and closing curly brackets on their own lines — never collapsed onto one
line.

**Wrong:**

```java
@Override
public String getSecond() { return "s"; }
```

**Correct:**

```java
@Override
public String getSecond() {
	return "s";
}
```

This applies to all methods regardless of how short the body is.

### Logging

Use Lombok `@CustomLog` (backed by Keystone's `org.luckyraven.keystone.logging.Logger`, configured in
`lombok.config`) and `log.warn/info/error`; never `Bukkit.getLogger()`. `module.properties` sets the logger
prefix.

### Version-drifting enums

`Material` / `Particle` / `Sound` / `PotionEffectType` resolve through XSeries or Keystone's
`org.luckyraven.keystone.sound.SoundEffect` — never raw `valueOf` and never `player.playSound(..., Sound.X, ...)`.
(Not yet exercised in this repo — no sound/particle code ported so far.)

### Chat and color codes

Colored chat goes through `org.luckyraven.keystone.util.ChatUtil.color()` with `&` codes; never emit a literal
`§` in source. `Line.addContent()` already routes through it — do not bypass with a raw string concatenation.

### YAML

Block-style maps only — never inline `{ key: value }` flow syntax, every key on its own line.
`Capitalized_Underscore_Separated` keys (lookup ids inside values stay lowercase). Never
`FileConfiguration.setDefaults()` / `copyDefaults()` for YAML fallbacks. `scoreboard.yml`'s schema is kept
byte-identical to Gangland's today, so an admin can copy the file across verbatim (WS1 plan §5).

## Testing

JUnit 5 + Mockito + Keystone's `keystone-testkit` (`BukkitStatics`, `PluginMocks` — see the Gangland repo's
`documentation/TESTING.md` for the house patterns this repo follows). `com.viaversion:viaversion-api` stays on
the `provided`/test classpath even where no test currently mocks a ViaVersion-typed field — Mockito's inline mock
maker retransforms the full type hierarchy of any mocked type, so removing this dependency as "unused" risks a
`TypeNotPresentException` the moment a future test mocks something in `DriverHandler`'s hierarchy (same gotcha
documented in Bartizan's `CLAUDE.md`/`pom.xml`).
