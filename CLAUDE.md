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

## Package map (as built through G2)

- `org.luckyraven.plaque.Plaque` — the `JavaPlugin` entry point. `onEnable()` builds `PlaqueContext` and calls
  `bootstrap()`, then `dependencyHandler()` (ViaVersion/PlaceholderAPI soft-dep checks, WS1-D3), then B5's
  enable-time board sweep. `onDisable()` ends every active board before shutting down beans.
- `org.luckyraven.plaque.bootstrap`
  - `PlaqueContext` — owns the single `DependencyContainer`/`BeanFactory`; drives KERNEL → FILE → CONFIG →
    LISTENER → COMMAND (Keystone's DATABASE phase runs empty — this plugin keeps no database). Same shape as
    Bartizan's `BartizanContext`.
  - `DefaultListenerService` — Keystone ships only the abstract `ListenerService`; every consumer plugin writes
    this one-method concrete subclass itself.
- `org.luckyraven.plaque.config` — three `@Configuration` classes, one per phase that needs one:
  - `KernelConfig` (KERNEL) — `Diagnostics` hub, `FileManager` (registers `settings.yml` + `scoreboard.yml`).
  - `FileConfig` (FILE) — `BoardSettings` + `BoardAddon` beans, each self-registers as a `FileInitializer`.
  - `BoardConfig` (CONFIG, default phase) — the `PlaceholderProvider` bean (`PapiText`), `BoardManager`,
    `DefaultListenerService`, `CommandManager`.
  - `BoardSettings` — reads `Enable`/`Driver` off `settings.yml` via plain `FileConfiguration` getters (no
    Gangland-style `NodeReader` validation layer — that class is Gangland application code, not Keystone).
- `org.luckyraven.plaque.board` — the rendering engine, ported (package rename only) from Gangland's
  `gangland-ui/scoreboard-api`, with two fixes applied at G1 port time (see "Deviations from upstream" below):
  - `Board` (was `Scoreboard`) — owns the per-tick `RepeatingTimer` and start/stop lifecycle (UI-02 fix intact:
    `timer.start(false)`, never `start(true)`).
  - `BoardManager` (was `ScoreboardManager`) — G2 addition. Owns the `Map<UUID, Board>` registry (this plugin has
    no `User`/`UserManager` to hold a per-player board reference the way Gangland did), the `DriverV3` factory
    (`getDriverHandler`), and the enable-time/reload sweep (`sweepOnlinePlayers()`, `rebuildAll()`, `removeAll()`
    — B5). Takes `Plaque` (not a bare `JavaPlugin`) so it can read `plaque.getViaAPI()` lazily — see "ViaAPI
    wiring" below, this is not incidental.
  - `board.driver.DriverHandler` — abstract base; owns the live `FastBoard` instance and the ViaVersion-aware
    `FastBoardImpl.hasLinesMaxLength()` override (works for every driver, not just V3 — ViaVersion coupling is
    repo-wide, not driver-specific).
  - `board.driver.version.DriverV3` — the **only** driver ported (WS1 decision D1(b)). `DriverV1`/`DriverV2` and
    the reflection-based `getDrivers()` scan were deleted, not carried forward — see "Silent migration" below.
  - `board.part.Line` / `board.part.StaticLine` — per-board line state with rotation-cursor content cycling
    (UI-01 fix intact: `Line#copy()`/`copyAll()` give every board its own instances).
  - `board.configuration.BoardAddon` (was `ScoreboardAddon`) — a `FileInitializer` that parses `scoreboard.yml`
    into `Line`/`StaticLine` instances.
- `org.luckyraven.plaque.listener` — `PlayerBoardListener` (`PlayerJoinEvent` → `BoardManager#createBoard`),
  `RemoveBoardListener` (`PlayerQuitEvent` → `BoardManager#removeBoard`). Flat under `listener/`, not
  `listener/player/` — this is a single-purpose plugin, not a Gangland module (the `feedback_listener_package`
  house rule is a Gangland-monorepo convention, not binding here; the WS1 plan's own file tree places both flat).
- `org.luckyraven.plaque.command` — `ReloadCommand` (`/plaque reload`), a **keystone-command** `Command`, not a
  plain `CommandExecutor` — see "Command framework" below. No `InformationManager`/`commands.json` help-listing
  layer: this plugin has exactly one command, so that machinery would be scaffolding for a listing nobody needs
  yet (ponytail).
- `org.luckyraven.plaque.placeholder` — `PapiText`, the PAPI → `ServicesManager` `PlaceholderProvider` → raw-text
  chain (WS1-D5/A5). Implements `PlaceholderProvider` directly (a thin wrapper around
  `CompositePlaceholderProvider.of(papiOrPassthrough, serviceFallbackLambda)`) so it drops straight into
  `DriverHandler`'s constructor as the `placeholder` bean.

## Command framework: keystone-command, not a plain CommandExecutor

**This overrides an earlier draft of the WS1 plan document** (`WS1-scoreboard.md` §9 D2 recommended a plain
Bukkit `CommandExecutor`, arguing a full argument-tree framework is overkill for one leaf command). The user
decided otherwise in the wave's decisions register (WS1-D2): *"stick with keystone implementation since they
already have the whole library"* — orchestrator ruling W5 treats this as authoritative over the plan document's
own recommendation. `/plaque reload` is therefore a `org.luckyraven.keystone.command.Command` subclass
(`@CommandHandler`-annotated, discovered by the COMMAND-phase scan), `keystone-command` is a real `provided`
dependency, and `PlaqueContext` runs a real COMMAND phase (`CommandManager`, `CommandTabCompleter`,
`BrigadierTabRegistrar.registerIfSupported`) — mirroring Bartizan's `ReloadCommand`/`BartizanContext` almost
exactly. If a future session finds a plan artifact still describing "plain CommandExecutor" for this plugin,
that text is stale; this file and the shipped code are the source of truth.

## ViaAPI wiring: lives on `Plaque`, never on a bean (fix round 1, Important 1)

**A soft-dependency type must never appear in a public method descriptor of a registered Keystone bean.**
`BoardManager` originally held `@Setter private ViaAPI<?> viaApi` — that `@Setter` generates
`public void setViaApi(ViaAPI<?>)`, and `BeanFactory.runPostConstruct` reflectively calls `getDeclaredMethods()`
on every registered bean. `getDeclaredMethods()` resolves every parameter/return type in every method's
descriptor as part of loading the method list — on a server without ViaVersion installed, resolving `ViaAPI`
throws `NoClassDefFoundError` for the *whole class*, so `ReflectionGuard.orSkip` logs a `reflection.type.missing`
fault and silently skips `BoardManager`'s post-construct wiring entirely on every ViaVersion-less boot (Opus
review G2-review.md, Important 1 — found in code review, not caught by any test since the test suite doesn't
run this plugin through a real Keystone `BeanFactory`).

The fix: `viaAPI` lives on `Plaque` (`private ViaAPI<?> viaAPI;`, `@Getter`-generated `getViaAPI()`), set once by
`dependencyHandler()` right after `context.bootstrap()` completes. `Plaque` is safe to hold this because it is
`registerInstance`d directly into the container in `PlaqueContext`'s constructor — it is never scanned by
`runPostConstruct`'s reflection pass, so a soft-dependency type in one of its method descriptors never gets
resolved reflectively. `BoardManager.getDriverHandler(Player)` reads `plaque.getViaAPI()` inline, every call,
never caching it in a field of its own — same "resolve lazily" rule as before, just relocated to a safe holder.

**Do not** reintroduce a `ViaAPI`-typed field, setter, or constructor parameter on `BoardManager` (or any other
`@Bean`-produced class) without re-reading this section — the class-loading failure only manifests when
ViaVersion is actually absent, so a dev environment with ViaVersion installed will not catch the regression.

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

## Silent migration warning (WS1 decision D1) — implemented, keep it that way

Only `DriverV3` exists in this repo. Gangland's `ScoreboardManager.java:66` (pre-split) defaulted an
**unrecognised** `Driver:` value to `DriverV1`, not V3. A server admin copying an old `settings.yml` that still
says `Driver: Driver_V1` or `Driver_V2` must **not** be silently handed V1 (which had UI-16, now deleted code) —
`BoardManager` hardcodes `DriverV3` in `getDriverHandler()`. The warning on an unrecognised value lives in
`BoardSettings.initialize()` (fix round 1, Minor 3 — moved out of `getDriverHandler()`, which runs once per
player, so it now fires once per config load/reload instead of spamming once per board built). Do not reintroduce
a silent multi-driver fallback, and don't move the warning back to a per-board call site.

## Cross-plugin placeholder chain — implemented

Per WS1-D5/A5: `PapiText` tries PAPI first (guard-constructed once, at startup, only if the PlaceholderAPI
plugin is present), then an optional Keystone `PlaceholderProvider` another plugin (e.g. Gangland, via
`GanglandPlaceholder.asProvider()` — the same seam `ItemVocabulary` already uses) publishes on Bukkit's
`ServicesManager` — looked up **fresh on every call**, never cached, since the publishing plugin may enable
after this one — then raw text. `Plaque#dependencyHandler()` logs a boot warning when PlaceholderAPI is absent
(WS1-D3); `scoreboard.yml`'s header comment states the same requirement for admins.

`FlashPlaceholderWrapper.currentTick` is a **static field in the shared, unrelocated `keystone-common` jar** —
this plugin's `Board`/`DriverV3` write it every tick, Gangland's `GanglandPlaceholder` reads it for
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

### `BukkitStatics` only pre-wires four statics

`BukkitStatics.install()` (keystone-testkit) wires `Bukkit.getServer()` / `getPluginManager()` /
`getServicesManager()` / `getScheduler()` to its own mocks automatically. **Every other static Bukkit method —
`Bukkit.getOnlinePlayers()` included — is not pre-wired** and returns `null` until stubbed explicitly through the
`MockedStatic` handle: `bukkit.statics().when(Bukkit::getOnlinePlayers).thenReturn(List.of(...))`. Stubbing
`bukkit.server().getOnlinePlayers()` instead does nothing, because `Bukkit`'s own static methods are intercepted
independently of the `Server` mock they happen to share a name with — they are not proxied through
`Bukkit.getServer()` under a static mock. `BoardManagerTest` hit this directly; check `bukkit.statics()`'s
existing stubs before assuming a `Bukkit.*` static call is covered.

### Testing a bean that only orchestrates other beans

`BoardManagerTest` subclasses `BoardManager` twice for two different questions: one anonymous subclass overrides
`createBoard` to record calls (proving `sweepOnlinePlayers()` visits every online player, without needing a live
`FastBoard`/NMS server); a second overrides `removeAll`/`sweepOnlinePlayers` themselves to record call order
(proving `rebuildAll()` calls them in that order — fix round 1, Minor 4). `BoardAddon`/`BoardSettings`/`Plaque`
are plain Mockito mocks in both since none is touched once the relevant method is overridden away — don't
upgrade them to real `FileManager`-backed instances unless a test actually needs `getDriverHandler()`'s real
output.

`ReloadCommandTest` mocks `PlaqueContext` directly (it is `final` — Mockito 5.x's default inline mock maker
handles this with no extra setup) and asserts `FileManager#initializeAll()` → `PlaqueContext#reloadBeans()` →
`BoardManager#rebuildAll()` fire in that exact order via `org.mockito.InOrder`. `onExecute` is `protected`, so
the test lives in the same package (`org.luckyraven.plaque.command`) to call it directly rather than routing
through Bukkit's command dispatcher.
