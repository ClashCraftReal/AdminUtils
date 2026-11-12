# AdminUtils Phase 1 - Test Record

Everything below was executed on this machine. Anything not executed is listed under
"Not executed" and must not be reported as passing.

## Build matrix

Command: `./gradlew.bat buildAllVersions --console=plain --rerun-tasks`

| Target | compileJava | compileClientJava | jar |
|---|---|---|---|
| 1.21.11 | pass | pass | `versions/1.21.11/build/libs/adminutils-1.0.0+1.21.11.jar` |
| 26.1.2 | pass | pass | `versions/26.1.2/build/libs/adminutils-1.0.0+26.1.2.jar` |
| 26.2 | pass | pass | `versions/26.2/build/libs/adminutils-1.0.0+26.2.jar` |
| 26.3 | pass | pass | `versions/26.3/build/libs/adminutils-1.0.0+26.3.jar` |

Result: `BUILD SUCCESSFUL`.

## Jar content checks

Verified by unzipping each produced jar:

- `com/lorenz/adminutils/AdminUtils.class` and
  `com/lorenz/adminutils/client/AdminUtilsClient.class` present in all four jars (76 classes each).
- `fabric.mod.json` expanded with the correct version and `assets/adminutils/icon.png`.
- `adminutils.mixins.json` compatibility level: `JAVA_21` on 1.21.11, `JAVA_25` on 26.1.2/26.2/26.3.
- `adminutils.client.mixins.json` compatibility level matches the same per-version values.
- `PayloadRegistry` bytecode references `playC2S`/`playS2C` on 1.21.11 and
  `serverboundPlay`/`clientboundPlay` on 26.x.

## Dedicated server startup

Command: `./gradlew.bat :<target>:runServer`, waiting for the `AdminUtils ready` log line.

| Target | Result | Log evidence |
|---|---|---|
| 1.21.11 | pass | `AdminUtils ready in 37 ms: permissions enabled=true, opBypass=true, authorizedPlayers=0` |
| 26.1.2 | pass | `AdminUtils ready in 50 ms` |
| 26.2 | pass | `AdminUtils ready in 37 ms` |
| 26.3 | pass | `AdminUtils ready in 58 ms` |

Each run also logged: 6 payload types registered, 3 server receivers registered,
disguise service `noop` unavailable, Librarian not installed.

## Configuration loading

- Missing file: `config/adminutils/permissions.json` created with
  `schemaVersion`, `enabled`, `opBypass`, `authorizedPlayers`.
- Valid file with a bad entry: invalid UUID skipped, unknown permission dropped,
  `authorizedPlayers=2` loaded.
- Malformed JSON: `Failed to load ...; falling back to safe defaults (operators only)`,
  server still reached ready state with `authorizedPlayers=0`.

## Automated unit tests

Command: `./gradlew.bat :<target>:test` (JUnit 5, test-only dependency).

| Target | tests | failures | skipped |
|---|---|---|---|
| 1.21.11 | 43 | 0 | 0 |
| 26.1.2 | 43 | 0 | 0 |
| 26.2 | 43 | 0 | 0 |
| 26.3 | 43 | 0 | 0 |

Coverage: permission identifiers and `admin.full` inheritance, snapshot `has()` and CSV
round-trip, token-bucket burst/refill/isolation/forget, permission config creation,
validation, duplicate handling, malformed JSON and newer-schema rejection, item storage
persistence, removal, data-version rejection and JSON import/export round-trip, plus
`ServerPermissionServiceTest` (9 cases) for OP detection and the `opBypass` rule.

## OP detection and GUI refresh (bug fix)

Reported symptom: an operator saw `Operator: no` / `Permissions: none` in the menu.

Root cause: the server side was already correct. `ServerPermissionService` resolved
operator status from `player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`
and the snapshot carried `operator=true` with `admin.full`. The defect was client-side:
`AdminScreen` built its widgets once in `init()` and `ClientSession.addListener` was never
called, so a `PermissionSnapshotS2C` that arrived after the screen opened never rebuilt it.

Fix: `AdminScreen` registers a `ClientSession` listener in `init()`, removes it in
`removed()`, and calls `rebuildWidgets()` from the listener (guarded against re-entry and
against firing while the screen is not active).

Evidence (26.3 dedicated server on port 25565, `online-mode=false`, `ops.json` level 4 for
`Tester`, `opBypass=true`, `authorizedPlayers: []`; dev client
`--username Tester --quickPlayMultiplayer localhost:25565`, temporary auto-open hook since
removed):

```
[20:01:58] Permission snapshot received: enabled=true operator=true opBypass=true granted=[admin.full]
[20:01:58] VERIFY screen built: operator=true granted=[admin.full]
[20:01:58] VERIFY rebuild: operator=true granted=[admin.full]
[20:01:58] Permission snapshot received: enabled=true operator=true opBypass=true granted=[admin.full]
```

The same sequence was reproduced on 26.2 (port 25566) with identical values. The
`VERIFY rebuild` line is the listener firing on a fresh snapshot while the menu was open.
Server-side OP detection and `opBypass` loading were confirmed on all four targets from the
`Loaded permissions ... opBypass=true` and `Permission snapshot received ... operator=true`
log lines.

## Client GUI initialization

Command: `./gradlew.bat :<target>:runClient` with a temporary auto-open hook (since removed
and verified absent from the shipped classes).

| Target | Result | Log evidence |
|---|---|---|
| 1.21.11 | pass | `AdminUtils screen initialized: section=HOME widgets=6` |
| 26.1.2 | pass | `AdminUtils screen initialized: section=HOME widgets=6` |
| 26.2 | pass | `AdminUtils screen initialized: section=HOME widgets=6` |
| 26.3 | pass | `section=HOME widgets=6` and `section=SETTINGS widgets=8` |

No render-thread exception occurred while the screen stayed open on any target.

Client startup also logged on every target: preferences loaded, 3 client receivers
registered, key binding registered, local item storage ready with `dataVersion 1`.

## Multiplayer matrix (executed)

Environment: dedicated server `:26.3:runServer` with `online-mode=false`, `white-list=false`;
dev client `:26.3:runClient --args="--username Tester --quickPlayMultiplayer localhost:25565"`.
Offline UUID for `Tester` is `f3d28cb0-7225-3cb1-baeb-2dadd2be89ae`. Evidence is the client
snapshot log line plus the server audit log.

| Case | Setup | Observed |
|---|---|---|
| Unauthorized non-OP | `authorizedPlayers: []`, `ops.json: []` | `Permission snapshot received: enabled=true operator=false opBypass=true granted=[]` |
| Authorized non-OP | entry with `gui.open`, `players.view` | `granted=[gui.open, players.view]`, `operator=false` |
| OP player | same entry plus `ops.json` level 4 | `operator=true`, `granted=[gui.open, players.view, admin.full]` |
| Permission revocation | `players.view` removed, OP removed, server restarted | `granted=[gui.open]`, `operator=false` |
| Disconnected client | client stopped while connected | server: `Tester lost connection: Disconnected`, `Tester left the game` |
| Invalid network requests | covered by `RequestValidatorTest` and `RateLimiterTest` (8 + 5 cases) | `invalid_request`, `rate_limited`, `unsupported_action` paths asserted |

Handshake and join were audited on the server for every connection, for example:
`audit OK handshake by Tester (f3d28cb0-...) : client=1.0.0+26.3 protocol=1` and
`audit OK session.join by Tester (...) : permission snapshot pushed`.

Note: the dev client generates a random offline username per launch unless `--username` is
passed, so a fixed username is required for UUID-based tests.

## Not executed

- Visual confirmation of layout, colours and text placement: no screenshot tooling here.
- A hand-crafted malicious client sending raw packets: the validation and rate-limit paths are
  covered by unit tests rather than by a modified client.

## Multiplayer test matrix (manual reference)

The matrix above was executed for 26.3. To repeat it on another target, run the same steps
with that target's `runServer` / `runClient` tasks and its own `config/adminutils/permissions.json`.

## Version-specific workarounds

- `PayloadRegistry` - `playC2S()/playS2C()` (1.21.11) vs `serverboundPlay()/clientboundPlay()` (26.x).
- `RenderContext` / `AdminScreen` - `render(GuiGraphics, ...)` + `drawString` (1.21.11) vs
  `extractRenderState(GuiGraphicsExtractor, ...)` + `text` (26.x).
- `AdminKeybinds` - `KeyBindingHelper.registerKeyBinding` (1.21.11) vs
  `KeyMappingHelper.registerKeyMapping` (26.x).
- `GuiCompat` - `setScreenAndShow` everywhere, because `setScreen` was removed in 26.2.
- Mixin `compatibilityLevel` - expanded per version, because 1.21.11 runs on Java 21.
