# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build commands

```bash
./gradlew assembleDebug                              # Build debug APK
./gradlew testDebugUnitTest                          # Run all unit tests
./gradlew :feature:auth:presentation:testDebugUnitTest    # Tests for one module
./gradlew connectedAndroidTest                       # Instrumented tests on device/emulator
```

Gradle needs a JDK 21; there is no system Java on the usual dev machine, so export Android
Studio's bundled JBR first:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

No CI/CD is wired yet. ktlint is not configured.

## Architecture

**Multi-module**, clean-architecture. The `app` module is the shell — it wires DI and hosts
`RootNavHost`. Each feature is split into three Gradle modules: `domain` (contracts and
models), `data` (data sources, mappers, service impls), and `presentation` (Compose UI +
MVI). There is **no api/impl split** and **no UseCase classes** — services hold domain logic
directly.

### Module map

| Layer | Modules | Purpose |
|---|---|---|
| `app` | `:app` | `TestAiApplication`, `MainActivity`, `RootNavHost` |
| `core` | `:core:resource`, `:core:network`, `:core:database` | Design system + MVI base, HTTP, Room |
| `feature` | `:feature:<name>:domain` + `:data` + `:presentation` | One trio per feature (`auth`, `home`) |

`:core:resource` is the design system **and** carries the MVI base (`BaseViewModel`,
`UiState`/`UiEvent`/`UiEffect`, `NoEvent`/`NoEffect`) and every user-facing string.
Presentation modules depend on it via the `testai.android.feature` convention plugin.

**There is no `:core:common` and no `:core:testing`, and adding one is not the answer to
"where does this go?"** A `core` module earns its place by naming a real technology boundary
— the design system, HTTP, Room. A module named for its lack of a subject collects whatever
has nowhere else to be, and then every feature depends on all of it. The things that would
have landed there are handled instead as follows.

**Dispatchers are not injected.** Data-layer classes call `withContext(Dispatchers.IO)` (or
`Dispatchers.Default`) directly at the call site — there is no `@Dispatcher` qualifier, no
`AppDispatcher` enum and no Hilt module. Nothing is lost in tests: a ViewModel test fakes the
*service* interface and never reaches a dispatcher, and the data-layer tests mock the source
they wrap. `MainDispatcherRule` still controls `Dispatchers.Main`, which is the one that
matters for a ViewModel.

**The clock belongs to whichever feature reads it.** `TimeProvider` (cache expiry) is home's
alone. `DateProvider` and `CalendarDate` exist once per feature: the contract in
`<feature>/domain/`, the `System*` implementation in `<feature>/data/service/`, bound in that
feature's data module. The two `CalendarDate`s are not a duplicated type — auth's carries only
the comparison a date of birth needs, home's carries the epoch-to-local-date arithmetic the
forecast and prayer caches need and auth never calls. Neither feature can drift into the
other's rules, because they do not share any.

### Packaging convention (IMPORTANT)

- Package = `com.example.test_ai_project.<module>` — the `core`/`feature` directory is
  **build-path only, never in the package**.
  - `:core:network` → `…test_ai_project.network`; `:feature:auth:domain` →
    `…test_ai_project.auth.domain`.
- **A feature is a flow, not a screen.** `:feature:auth` covers both the credentials form
  and the face check, and `:feature:home` covers all four tabs — one trio each, with a
  folder per screen under `presentation/`. Splitting per screen would multiply Gradle
  modules without splitting anything that actually varies independently.
- **Every file lives in a concern folder — nothing loose at a package root.**
  - `domain/`: `model/`, `service/`, `exception/`
  - `data/`: `remote/`, `local/`, `mapper/`, `service/`, `notification/`, `di/`
  - `presentation/<screen>/`: `screen/`, `viewmodel/`, `contract/`, `components/`, `navigation/`

### Feature internals

```
feature/<name>/
  domain/      model/ (data classes, sealed results, errors) + service/ (interfaces)
  data/        local/ or remote/ (sources) + mapper/ (Dto/Entity→domain) +
               service/ (DefaultXxxService impl) + di/ (@Binds)
  presentation/<screen>/  screen/ + viewmodel/ + contract/ + components/ + navigation/
```

Domain returns its own sealed result type (`LoginResult`, `FaceVerificationResult`) with a
semantic error; the **data layer maps whatever it deals in — an `AppError`, a keystore
exception — onto the domain error**, so domain never sees the network or platform layer.
Where a server supplies a message it is carried up as an optional `message: String?` and
preferred over the localized fallback.

### The dependency rule is enforced by the classpath

`:app` takes `implementation(project(":feature:X:presentation"))` but
`runtimeOnly(project(":feature:X:data"))`. Data modules are invisible at compile time and
join only via Hilt at runtime — that, not review discipline, is what stops a screen reaching
past its own service contracts.

### Navigation

Each screen owns a `navigation/` folder with route constants (`object XxxRoutes`) and a
`NavGraphBuilder.xxxScreen(...)` extension. `RootNavHost` registers them and wires
cross-screen transitions via lambdas (`onAuthenticated`, `onVerified`). Launch order —
login → face verification → home — lives in `RootNavHost` and nowhere else. Both auth
screens register from `:feature:auth:presentation`, so the flow between them is still the
root graph's decision rather than one screen naming another.

## UI / Design system (`:core:resource`)

Packages: `component/` (the `App*` widgets), `theme/` (tokens), `util/` (`CollectAsEffect`),
`base/` (MVI), `preview/` (`DevicePreview`).

### Responsive scaling (no breakpoints)

`AppTheme` computes `ScreenMetrics.scaleFactor = screenWidthDp / 360f`, clamped to
[1.0, 1.2]. Use `.scaled` / `.scaledSp` — never `.dp` / `.sp` directly in screens.
`LocalSpacing` / `LocalSizes` are provided via `CompositionLocal`, pre-multiplied by scale.

- `Spacing(small=8, medium=16, large=24)` via `spacing`
- `Sizes(buttonHeight, icon, iconLarge, radius, avatar, contentMaxWidth)` via `sizes`
- One light `VaultColorScheme`; the dark surfaces (launch window, viewfinder) paint from
  brand tokens directly.

### Rules (enforced)

1. **Design-system only**: screens do not use raw Material3 `Text`/`Button`/`Icon`/
   `TextField` or Coil directly. Use `AppText`, `AppButton`, `AppTextField`, `AppIcon`,
   `AppProgressIndicator`, `AppNetworkImage`, `AppLoadingState`, `AppErrorState`.
   Typography goes through the **`AppTextStyle`** enum, not `MaterialTheme.typography`.
   (`MaterialTheme.colorScheme` is still used directly for colours.)
2. **Screen/Content split**: every screen has a `Screen()` (with `hiltViewModel()`, effect
   collection) and a stateless `Content()`. Extract each UI element into its own file under
   `components/`.
3. **One `@DevicePreview` per file** — use the shared multipreview annotation
   (`…resource.preview.DevicePreview`), never a raw `@Preview`. It is the single source of
   truth for preview size.
4. **No hardcoded UI strings** — all user-facing text lives in `:core:resource`
   `strings.xml`, named `<area>_<purpose>` (`login_continue`, `weather_error_generic`).
   Referenced as `com.example.test_ai_project.resource.R`, conventionally imported
   `as ResR`. Feature-local drawables stay in their own module and use the local `R`.
5. **Errors via the top toast** (`LocalAppToast` + `AppToastHost`, hosted once in
   `RootNavHost`), emitted as one-shot MVI effects — not inline UI state.
   Two deliberate exceptions, both where the message *is* the screen's primary content
   rather than an incidental failure: the face-verification instruction panel, and the home
   tabs' advisory banner that sits above still-valid cached data.
6. **Standard screen padding**: 16 horizontal / 8 vertical — `spacing.medium` horizontally,
   `spacing.small` vertically. Apply via the content/list `contentPadding`, not raw `.dp`.

### MVI

`BaseViewModel<State, Event, Effect>` exposes `uiState: StateFlow` and `effects: Flow`;
reduce with `setState { copy(...) }`, emit one-shot effects with `sendEffect(...)`. Use
`NoEvent`/`NoEffect` for screens without events/effects. Effects go through a `Channel`, not
a `StateFlow`, so a rotation does not re-navigate.

`uiState` is `open`. Screens whose state is **derived** rather than reduced — the home tabs,
which fold a Room cache together with a ticking clock — override it with
`combine(...).stateIn(viewModelScope, WhileSubscribed(...), ...)`. That is load-bearing:
pushing those into `setState` from an eager collector leaves the clock ticking while nothing
is watching, and makes `advanceUntilIdle()` hang in tests.

## Data layer

- **Network** (`:core:network`): Retrofit + OkHttp + kotlinx.serialization. `AppResult`,
  `AppError` and `safeApiCall { }` in `result/` normalize Retrofit/IO exceptions into
  values; `AppError` is `Network`/`Server(code)`/`Unauthorized`/`Unknown`. Per-provider
  clients (TMDB, Aladhan, OpenWeather) are separate so an auth interceptor never runs for
  the wrong host. Credentials come from `local.properties` or env vars, never checked in.
- **Database** (`:core:database`): Room `AppDatabase`, DAOs in `dao/`, entities in `entity/`.
  Schemas are exported to `core/database/schemas/` and every step is an `AutoMigration`;
  v6 drops the generated `items` scaffolding via a `@DeleteTable` `AutoMigrationSpec`.
- **Offline-first, strictly**: Room is the only thing the UI reads from, and the network is
  a process that writes into Room. A failed request never produces an empty screen.

## Convention plugins (build-logic)

| Plugin ID | Applies |
|---|---|
| `testai.android.app` | Android application, adds `targetSdk` |
| `testai.android.library` | Android library — every core and feature-layer module |
| `testai.android.feature` | Bundles library + compose + hilt + serialization; adds `:core:resource`, lifecycle/nav deps |
| `testai.android.compose` | Compose compiler plugin, BoM, Material3, tooling |
| `testai.android.hilt` | KSP + Hilt |
| `testai.android.room` | Room + KSP + schema export |

SDK levels and the JVM target are read from `libs.versions.toml`, so there is one source of
version truth. AGP 9 has built-in Kotlin support: `CommonExtension` is **not** generic here,
and there is no `org.jetbrains.kotlin.android` plugin to apply.

## Testing

- Test deps come from the library convention — JUnit 4, Turbine, MockK, Truth,
  kotlinx-coroutines-test — so a new module can write a test without editing its build
  script. **There is no `:core:testing`**, for the same reason there is no `:core:common`:
  the convention already supplies the toolchain, which left such a module hosting one JUnit
  rule that half its dependents never used.
- `MainDispatcherRule` sets `Dispatchers.Main` to a test dispatcher. It lives in each
  presentation module's own test source set (`<feature>/presentation/…/testing/`) — fifteen
  lines of boilerplate, not a Gradle module. Defaults to `UnconfinedTestDispatcher`; pass a
  `StandardTestDispatcher` when a test needs to observe in-flight state.
- ViewModel tests drive `onEvent(...)` and assert on `uiState` and `effects`. Fake the
  *service* interface — it is the only thing a ViewModel can reach, so one fake replaces
  Room, Retrofit and the platform at once.
- Screens whose ViewModel has a ticking clock must not use `advanceUntilIdle()`; those
  tests carry a `settle()` helper that advances the scheduler without draining an infinite
  ticker.

## Key constraints

- Retrofit (not Ktor) for HTTP; Room (not SQLDelight) for local DB.
- No UseCase classes — services hold domain logic; no api/impl split.
- Hilt DI everywhere; `@Singleton` for services that hold state.
- Images: `AppNetworkImage` (Coil 3); the `ImageLoader` and its disk cache are configured
  once in `TestAiApplication`.
- Permissions travel with the module that needs them — camera in
  `:feature:auth:presentation`, location in `:feature:home:presentation`,
  notifications/alarms in `:feature:home:data` — so removing a feature removes its
  permission.
