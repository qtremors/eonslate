# Eonslate - Developer Documentation

> **Project:** Eonslate
> **Version:** 2.0.1
> **Last Updated:** 2026-11-13

## Architecture

Eonslate is a multi-module Android project in `eonslate-app/`. Modules depend inward: features use core modules, and core modules never depend on features.

| Module | Kind | Role |
| :--- | :--- | :--- |
| `:app` | Android application | Entry point, splash, navigation host, dependency injection root |
| `:core:navigation:api` | Kotlin JVM | Type-safe `@Serializable` destinations shared by features |
| `:core:ui` | Android library | Material 3 Expressive theme, tokens and shared components |
| `:feature:home` | Android library | Home screen |

Planned modules follow the same pattern: pure Kotlin `domain` modules for models and rules, Android `data` modules for Room and storage, and one `feature` module per screen area.

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

## Design system

All screens use `:core:ui`. It provides `EonslateTheme` (system, light, dark and pure black; wallpaper, preset or custom colors), shape, spacing and type tokens, `EonslateScaffold` with the large flexible top bar, connected rows and choices, `ExpressiveSwitch`, `EonslateMenu`, `EonslateBottomSheet`, `InlineSearchField`, `LoadingState`, `EmptyState` and `rememberAppHaptics()`. A screen that needs something new adds it to `:core:ui` first.

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
./gradlew assembleRelease
```

Release signing reads `signing.properties` (or `local.properties`) with `signing.storeFile`, `signing.storePassword`, `signing.keyAlias` and `signing.keyPassword`. Both files are ignored by git.

## Conventions

- Package root: `dev.qtremors.eonslate`, with modules under `core.*` and `feature.*`.
- Release commits are titled `vX.Y.Z: Short Title Case Summary`.
- User-facing text lives in each module's `strings.xml`.
- Compose stability for classes in pure Kotlin modules is declared in `eonslate-app/compose-stability.conf`.
