# Eonslate - Developer Documentation

> **Project:** Eonslate
> **Version:** 2.0.8
> **Last Updated:** 2026-11-16

## Architecture

Eonslate is a multi-module Android project in `eonslate-app/`. Modules depend inward: features use core modules, and core modules never depend on features.

| Module | Kind | Role |
| :--- | :--- | :--- |
| `:app` | Android application | Entry point, splash, navigation host, dependency injection root |
| `:core:navigation:api` | Kotlin JVM | Type-safe `@Serializable` destinations shared by features |
| `:core:settings` | Android library | App settings in DataStore (appearance, welcome seen), apart from tracker data |
| `:core:tracker:domain` | Kotlin JVM | Tracker, field and entry models, exact value codecs, validators, insights (streaks, field stats, daily series), repository interfaces |
| `:core:tracker:data` | Android library | Room database, DAOs, integrity triggers, repositories, Hilt bindings |
| `:core:tracker:ui` | Android library | Tracker UI shared by features: the tracker slate, field type labels and icons, validation messages, value formatting, templates |
| `:core:ui` | Android library | Material 3 Expressive theme, tokens and shared components |
| `:feature:home` | Android library | Home: the grid of tracker slates, and the archived trackers page |
| `:feature:entry` | Android library | Logging and editing an entry: typed inputs, date and time, note |
| `:feature:trackerdetail` | Android library | A tracker's page: totals, entries by day, month calendar, delete with undo |
| `:feature:trackereditor` | Android library | Tracker builder: the template gallery, details, icon, pigment, fields and the field sheet |
| `:feature:settings` | Android library | Settings, About, open source licenses and the in-app privacy page |
| `:feature:backup` | Android library | Creating, restoring and undoing backups |
| `:feature:welcome` | Android library | The first run: what Eonslate is, then starter trackers |

Planned modules follow the same pattern: pure Kotlin `domain` modules for models and rules, Android `data` modules for Room and storage, and one `feature` module per screen area.

## Navigation

Destinations are `@Serializable` types in `:core:navigation:api`; `EonslateNavHost` in `:app` wires each to its screen.

| Destination | Screen | Opened from |
| :--- | :--- | :--- |
| `Welcome` | First-run pages | Start, until finished or skipped |
| `Home` | Grid of tracker slates | Start after the welcome |
| `Templates` | Blank tracker and every template | New tracker, the empty Home |
| `Archived` | Archived trackers | Home top bar |
| `Settings` | Appearance and behavior | Home's gear |
| `About`, `Licenses`, `Privacy` | About Eonslate and its legal pages | Settings, then About |
| `TrackerDetail(trackerId)` | A tracker's page | A slate on Home or Archived |
| `TrackerEditor(trackerId?, templateId?)` | Tracker builder; null creates, a template fills it in | The gallery, the detail pencil, a slate's menu |
| `LogEntry(trackerId, entryId?)` | Log or edit an entry | A slate's button, Log it, an entry's Edit |

Deleting a tracker from its editor returns to Home, since its detail page no longer exists.

## Settings

`Appearance` in `:core:settings` stores the theme, pure black, the color source, the preset, haptics and reduce motion, each by name so a value from a newer version falls back to its default. `Appearance.toThemeOptions()` in `:app` maps it onto `:core:ui`'s `ThemeOptions`, and `MainActivity` feeds it to `EonslateTheme`, so a change recolors the whole app at once. A near-grey preset seed uses Material's neutral scheme so it stays grey.

`Tracking` holds the week's first day and the currency new money fields start with; `firstDayOfWeek()` and `currencyCode()` fall back to the device. Screens that build trackers from templates pass a builder to their view model, which calls it with the currency read from settings, so a template never starts with a stale default. `InMemorySettingsRepository` stands in for DataStore in tests and previews.

The Licenses page lists `OpenSource.parts`. Full license texts ship as raw resources: Apache 2.0 taken verbatim from the Android SDK's NOTICE file and the MIT body from a published MIT license. Roboto Flex shows the copyright and license notice embedded in the font itself.

## First run

`MainActivity` keeps the splash up until it knows where to start. `WelcomeGate.shouldShow` shows the welcome only to someone new: never once it was finished or skipped, and never when trackers already exist (after an update or a restore), in which case it is marked seen at once. The welcome's picks and page survive process death in `SavedStateHandle`. An empty Home also offers one-tap starters, for anyone who skipped it.

## Templates

`Templates.all` in `:core:tracker:ui` holds every ready-made tracker as a `TemplateSpec`: an id that never changes (it is stored as the tracker's `templateId`), string resources for every name and choice, an icon and a pigment. `Templates.instantiate` turns one into a `Tracker` in the device's words and currency, with fresh tracker and field ids; choice options keep the template's ids. A test checks that every template builds a valid tracker with a known icon and pigment.

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
- Long histories are paged: lists read `observeRecentEntries(trackerId, limit)`, while totals and calendars read `observeEntryTimes`, which selects only `occurred_at` and `zone_id`. A picked day is read with `dayWindow(day)`, its UTC day widened by 14 hours each way to cover every zone, then filtered by each entry's own day.
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

## Backups

- `BackupSnapshot` (`:core:tracker:domain`) holds every table row as stored, so a restore is exact. Settings are not included.
- `BackupArchive`: `EONSLATE` magic, format version, flags, then gzipped JSON. Encrypted files add salt, PBKDF2-HMAC-SHA256 iterations and a nonce, and seal the body with AES-256-GCM using the header as associated data. The iteration count is stored per file (default 210,000; Android's PBKDF2 is slow).
- `BackupCheck` repeats the trigger rules before anything is touched. `RoomBackupRepository.replaceAll` clears and inserts in one transaction, with triggers on.
- `BackupManager` keeps the current data as `files/backups/before-restore.eonslate` before each restore; it replaces the previous point only after a successful restore. Undo restores it and deletes it.
- A schema change must also upgrade older snapshots before restoring them.

## Identity

Monochrome first; color only where it means something. Material 3 Expressive, never a copy of another app.

| Element | Rule |
| :--- | :--- |
| Color | Default `ThemePreset.MONO` with pure black (`monoColorScheme`, hand-built neutrals). Alternatives: wallpaper (Android 12+) or a seeded preset via `eonslateColorScheme`. Presets are stored by name. |
| Context | Pigments for trackers, ember (tertiary) for streaks and records, `LocalSignals` for positive and warning (harmonized unless Mono), error roles for errors. |
| Pigments | `BadgePalette` holds seventeen mineral pigments (Slate, Ember, Moss, Tide...). Tiles and glyphs come from fixed HCT tones, so contrast is the same for every hue. A tracker stores its pigment by index, so new pigments go at the end. |
| Type | `EonslateDisplay` is Roboto Flex at the narrow end of its width axis, set heavy, for titles, headlines and big values. Body text stays at normal width. |
| Shape | Tracker icons sit on `SlateTileShape` (a square with 30% corners), never a circle. Tracker cards use `SlateCardShape`. Empty states use the slanted slate. |
| Accent | Primary: `MainActionButton`, quick log, today's progress, selection. |
| Signature | `TrackerSlate`: a pigment-tinted card with the icon tile, the name in the display face and one line about what was logged. |

## Charts

`BarChart`, `LineChart`, `Meter`, `ChartAxis` and `ChartTicks` in `:core:ui` draw on a Compose `Canvas`, with no chart library. Every chart follows the same rules:

- One series per chart in the tracker's pigment (`BadgePalette.mark`), so no legend; the card title names it. The picked mark uses the accent; records use ember.
- Thin marks: bars at most 28 dp wide with 2 dp gaps and 4 dp rounded data ends on a zero baseline; a 2 dp line with 8 dp markers ringed in the surface color. A hairline baseline, no grid.
- Direct labels are selective: a bar chart names only its peak (`peakLabel`, with headroom reserved above the bar) and a line only its latest value (`endLabel`). Everything else is read by tapping or in the table.
- A tap picks a mark and the card's caption reads it. Every chart card has a table view, and each chart has a spoken summary.
- Big standalone numbers use proportional digits; axis ticks are tabular.

Insights are computed in `:core:tracker:domain` (`Streaks`, `FieldInsights`, and `Periods` for day, week and month buckets that start on the locale's first weekday, `Records` for personal bests, `Today` for Home's progress line) with exact `BigDecimal` sums and averages. The tracker page reads every entry with values only while Insights is open.

## Design system

All screens use `:core:ui`. It provides `EonslateTheme` (system, light, dark and pure black; wallpaper, preset or custom colors), shape, spacing and type tokens, `EonslateScaffold` with the large flexible top bar, connected rows and choices, `ExpressiveSwitch`, `EonslateMenu`, `EonslateBottomSheet`, `InlineSearchField`, `FormField` (always the 16 dp `FieldShape`), `ConfirmDialog`, `MainActionButton` (the one main action per page, in the accent, labelled for screen readers), `LoadingState`, `EmptyState` and `rememberAppHaptics()`. `EonslateScaffold` takes `onBack` for pages below the top level and a `floatingActionButton`. A screen that needs something new adds it to `:core:ui` first.

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

UI tests run on the JVM with Robolectric and Compose's test rule (`FieldInputCardUiTest`, `HomeContentUiTest`, `TrackerEditorUiTest`, `MonthCalendarUiTest`). Each module's `src/test/resources/robolectric.properties` pins SDK 35, and every UI test class sets a phone-sized screen with `@Config(qualifiers = ...)`.

```bash
./gradlew assembleRelease
```

Release signing reads `signing.properties` (or `local.properties`) with `signing.storeFile`, `signing.storePassword`, `signing.keyAlias` and `signing.keyPassword`. Both files are ignored by git.

## Conventions

- Package root: `dev.qtremors.eonslate`, with modules under `core.*` and `feature.*`.
- Release commits are titled `vX.Y.Z: Short Title Case Summary`.
- User-facing text lives in each module's `strings.xml`.
- A template's id and its choice option ids never change once shipped: trackers store them, and later imports and insights rely on them.
- New templates go anywhere in `Templates.all`; each needs at least one required field, a known icon and a valid pigment, which `TemplatesTest` checks.
- Compose stability for classes in pure Kotlin modules is declared in `eonslate-app/compose-stability.conf`.
