# Eonslate Changelog

> **Project:** Eonslate
> **Version:** 2.0.2
> **Last Updated:** 2026-11-13

---

## [2.0.2] - 2026-11-13

- **Tracker Model**: Added the pure Kotlin `core:tracker:domain` module with trackers, typed fields (number, money, duration, rating, yes/no, single and multi choice, text, date) and entries. Every value is held exactly: scaled whole numbers for decimals, minor units for money, seconds for durations and epoch days for dates.
- **Validation**: Added tracker and entry validators that report every problem at once, plus exact parsers and formatters for decimals, money in each currency's own digits, durations and dates.
- **Database**: Added the `core:tracker:data` module with Room tables for trackers, fields, entries and typed entry values, foreign keys that cascade, indices for the home list and date ranges, and SQLite triggers that refuse invalid rows even when the app is bypassed.
- **Safe Tracker Edits**: Changing a field's decimals or currency rescales logged values, removing a field or a choice option removes only what used it, and changing a field's type clears its old values instead of misreading them.
- **Schema History**: The database schema is exported to `schemas/` and tested by opening data written under every exported version with the current app.
- **Home List**: Home now reads trackers through a ViewModel, showing a loading indicator first, then the empty state or connected rows with each tracker's icon, color, entry count and latest entry.

## [2.0.1] - 2026-11-13

- **A New Foundation**: Started Eonslate, an offline tracker for almost anything, as a multi-module Android project with separate app, core and feature modules.
- **Material 3 Expressive Design System**: Added the shared `core:ui` module with the Expressive theme (system, light, dark, pure black, wallpaper and preset colors), shape and spacing tokens, Roboto Flex typography, connected rows and choices, the expressive switch, menus, sheets, search, state views and haptics.
- **Current Toolchain**: Built on Gradle 9.8.2, Android Gradle Plugin 9.5.1, Kotlin 2.4.21, KSP 2.3.12, Compose BOM 2026.09.00 and Material 3 1.5.0-rc01.
- **First Screen**: Added a splash screen that follows the theme, edge-to-edge layout, type-safe navigation and an empty home screen.
