# Eonslate - Developer Documentation

> **Project:** Eonslate
> **Version:** 2.0.4
> **Last Updated:** 2026-11-14

## Architecture

Eonslate is a multi-module Android project in `eonslate-app/`. Modules depend inward: features use core modules, and core modules never depend on features.

| Module | Kind | Role |
| :--- | :--- | :--- |
| `:app` | Android application | Entry point, splash, navigation host, dependency injection root |
| `:core:navigation:api` | Kotlin JVM | Type-safe `@Serializable` destinations shared by features |
| `:core:tracker:domain` | Kotlin JVM | Tracker, field and entry models, exact value codecs, validators, repository interfaces |
| `:core:tracker:data` | Android library | Room database, DAOs, integrity triggers, repositories, Hilt bindings |
| `:core:tracker:ui` | Android library | Tracker UI shared by features: the tracker slate, field type labels and icons, validation messages |
| `:core:ui` | Android library | Material 3 Expressive theme, tokens and shared components |
| `:feature:home` | Android library | Home: the grid of tracker slates |
| `:feature:entry` | Android library | Logging and editing an entry: typed inputs, date and time, note |
| `:feature:trackereditor` | Android library | Tracker builder: details, icon, pigment, fields and the field sheet |

Planned modules follow the same pattern: pure Kotlin `domain` modules for models and rules, Android `data` modules for Room and storage, and one `feature` module per screen area.

## Tracker data

A tracker owns an ordered list of fields; each entry fills some of them. Values are never floating point:

| Field type | Stored as | Column |
| :--- | :--- | :--- |
| Number | Whole number scaled by 10^decimals (12.5 kg with one decimal is 125) | `long_value` |
| Money | Minor units of the field's ISO 4217 currency | `long_value` |
| Duration | Seconds | `long_value` |
| Rating | 1 to the field's scale | `long_value` |
| Yes/no | 0 or 1 | `long_value` |
| Date | Days since 1970-01-01 | `long_value` |
| Single choice | Option id | `text_value` |
| Multi choice | JSON array of option ids, sorted | `text_value` |
| Text | The text | `text_value` |

- Field rules live in `FieldConfig`, stored as JSON keyed by `kind`. Unknown properties are ignored, so new optional settings need no migration.
- `saveTracker` validates, then writes the tracker and exactly its fields in one transaction. `FieldValueAdapter` rescales values when decimals or currency digits change, and drops values whose type or option is gone. Limits (min, max, scale, length) apply to new input only.
- SQLite triggers in `TrackerIntegrity` refuse rows that break the rules. Room does not track triggers, so they are reinstalled on every open and no migration has to remember them.
- The log screen keeps what a person types as `FieldInput` text and selections. `EntryInputs.read` turns each into an exact `FieldValue` or reports it unreadable; it never rounds. `ValueFormat` (in `:core:tracker:ui`) turns values back into locale-aware text.
- Entries store `occurredAt` (epoch milliseconds) and the IANA `zoneId` they were logged in; the day an entry belongs to is always computed in that zone.
- Schemas are exported to `eonslate-app/core/tracker/data/schemas/` and committed. A schema change bumps `TrackerDatabase.VERSION`, adds a `Migration` to `TrackerMigrations.ALL`, and adds a test to `TrackerMigrationTest` that writes data with `ExportedSchema.create` under the old version and reads it back with the current app.

## Technology stack

| Area | Technology |
| :--- | :--- |
| Toolchain | Gradle 9.8.2, Android Gradle Plugin 9.5.1, Kotlin 2.4.21, KSP 2.3.12, Java 21 daemon, JVM 11 bytecode |
| Platform | compileSdk 37, targetSdk 37, minSdk 26 (Android 8.0+) |
| UI | Jetpack Compose BOM 2026.09.00, Material 3 1.5.0-rc01 (Expressive), MaterialKolor 5.0.1, Roboto Flex |
| State and DI | Kotlin Coroutines 1.11.0, Dagger Hilt 2.60.1, Navigation Compose 2.10.2 |
| Persistence | Room 2.8.5, DataStore 1.3.0, Kotlinx Serialization 1.12.0 |
| Testing | JUnit 4.13.2, Robolectric 4.17, MockK 1.14.12, Turbine 1.2.1, ArchUnit 1.5.1 |

Versions live in `eonslate-app/gradle/libs.versions.toml`.

## Identity

Eonslate has its own look, "Slate and Ember". It uses Material 3 Expressive for behavior and components, but never looks like a copy of another app.

| Element | Rule |
| :--- | :--- |
| Color | `BrandSeed` (slate blue) drives primary, secondary and neutrals. `EmberSeed` (amber) always fills tertiary, through `eonslateColorScheme`. The Slate preset is the default; wallpaper colors are opt-in. |
| Pigments | `BadgePalette` holds seventeen mineral pigments (Slate, Ember, Moss, Tide...). Tiles and glyphs come from fixed HCT tones, so contrast is the same for every hue. A tracker stores its pigment by index, so new pigments go at the end. |
| Type | `EonslateDisplay` is Roboto Flex at the narrow end of its width axis, set heavy, for titles, headlines and big values. Body text stays at normal width. |
| Shape | Tracker icons sit on `SlateTileShape` (a square with 30% corners), never a circle. Tracker cards use `SlateCardShape`. Empty states use the slanted slate. |
| Accent | Ember marks the main action (Save, New tracker) and what is alive, such as progress and streaks. |
| Signature | `TrackerSlate`: a pigment-tinted card with the icon tile, the name in the display face and one line about what was logged. |

## Design system

All screens use `:core:ui`. It provides `EonslateTheme` (system, light, dark and pure black; wallpaper, preset or custom colors), shape, spacing and type tokens, `EonslateScaffold` with the large flexible top bar, connected rows and choices, `ExpressiveSwitch`, `EonslateMenu`, `EonslateBottomSheet`, `InlineSearchField`, `FormField` (always the 16 dp `FieldShape`), `ConfirmDialog`, `EmberActionButton` (the one main action per page, labelled for screen readers), `LoadingState`, `EmptyState` and `rememberAppHaptics()`. `EonslateScaffold` takes `onBack` for pages below the top level and a `floatingActionButton`. A screen that needs something new adds it to `:core:ui` first.

Every module with Compose UI opts in to `ExperimentalMaterial3Api` and `ExperimentalMaterial3ExpressiveApi`.

## Build commands

Run from `eonslate-app/`:

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew :core:tracker:domain:test
```

UI tests run on the JVM with Robolectric and Compose's test rule (`FieldInputCardUiTest`, `HomeContentUiTest`, `TrackerEditorUiTest`). Each module's `src/test/resources/robolectric.properties` pins SDK 35 and a phone-sized screen.

```bash
./gradlew assembleRelease
```

Release signing reads `signing.properties` (or `local.properties`) with `signing.storeFile`, `signing.storePassword`, `signing.keyAlias` and `signing.keyPassword`. Both files are ignored by git.

## Conventions

- Package root: `dev.qtremors.eonslate`, with modules under `core.*` and `feature.*`.
- Release commits are titled `vX.Y.Z: Short Title Case Summary`.
- User-facing text lives in each module's `strings.xml`.
- Compose stability for classes in pure Kotlin modules is declared in `eonslate-app/compose-stability.conf`.
