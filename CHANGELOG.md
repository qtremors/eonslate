# Eonslate Changelog

> **Project:** Eonslate
> **Version:** 2.0.9
> **Last Updated:** 2026-11-16

---

## [2.0.9] - 2026-11-16

- **Reminders**: Per-tracker reminders at a time on chosen days, from the bell on a tracker's page.
- **Reliable**: Each alert schedules the next; restarts, time and zone changes reschedule. Exact when allowed.
- **Skip if logged**: Optionally no reminder when the tracker already has an entry that day.
- **Open to log**: Tapping a reminder opens that tracker's log screen.
- **Backups**: Include reminders.
- **Chips**: Picked chips use the accent, so they stand out in Mono.
- Search Home by tracker name, description, field name or tag.
- Add tags in the tracker editor.
- Filter Home by tag.
- Home remembers the selected tag across launches.
- Search entry notes and text fields in List.
- Backups include tracker tags.

## [2.0.8] - 2026-11-16

- **Backup and restore**: Back up every tracker and entry to one file, optionally encrypted (AES-256-GCM). Restores replace everything in one step and can be undone.
- **Mono**: Monochrome by default, white on true black.
- **Accents**: Wallpaper, Mono and eight accents in one picker, adding Ochre and Rose.
- **Color**: Color only where it means something: tracker pigments, ember streaks, warnings and errors.
- **Copy**: Shorter text across the app.

## [2.0.7] - 2026-11-15

- **Templates**: Start a tracker from twenty ready-made templates: expenses, daily habit, mood, workout, reading, water, sleep, medication, weight, meditation, journal, blood pressure, caffeine, steps, study, savings, gratitude, symptoms, cycle and screen time. Each opens in the builder already filled in, so anything can be changed before saving.
- **See Before You Use**: Tapping a template shows every field it holds before you use it.
- **Find A Template**: Templates sit on shelves (health, mind and habits, activity, money and time) and a search finds them by name or description.
- **Duplicate**: Long-press a tracker on Home to make a copy with the same fields and none of the entries.
- **Starters On An Empty Home**: An empty Home offers a daily habit, mood and water to start with in one tap.
- **New Tracker Gallery**: New tracker now opens a gallery with a blank tracker first and every template after it, each saying what it holds.
- **Welcome**: The first launch shows what Eonslate is and offers a few trackers to start with. Both pages can be skipped, the welcome never shows again, and it never shows to someone who already has trackers.
- **In Your Language and Currency**: Template names, fields and choices come from the app's text, and money uses the device's currency.
- **Privacy Policy**: Now says exactly what is stored on the device and where, and that no permissions are asked for.
- **Room To Read**: A tracker slate gives its description three lines, so a longer one never hides what follows.
- **Clearer Today Line**: Screen readers hear Home's "logged today" line once instead of twice.
- **Settings**: A settings page from the gear on Home. Choose the theme (system, light or dark), pure black for OLED screens, and Eonslate's own palettes or your wallpaper's colors (Android 12 and up). Every change applies at once.
- **Palettes**: Pick Slate, Tide, Moss, Clay, Dusk or Graphite; each keeps the ember accent. Graphite now stays a true grey.
- **Behavior**: Turn haptics off, or reduce motion for calmer transitions.
- **First Day Of The Week**: Start weeks on Monday, Sunday or Saturday, or follow the device. Calendars and weekly insights follow the choice.
- **Default Currency**: Choose the currency new money fields and templates start with, or follow the device. Fields you already made keep their own.
- **About**: The version, the developer, the source code, releases and a way to report an issue, plus this device's details, copied with the app version in one tap for a bug report.
- **Open Source Licenses**: Every open source part Eonslate is built with, by its authors, with the full license text to read in the app.
- **Privacy In The App**: The privacy policy can be read inside the app, without the internet.

## [2.0.6] - 2026-11-15

- **Insights**: Every tracker page has an Insights view next to List and Calendar.
- **Streaks**: See the current and longest run of days with an entry, and when the longest one ended. A streak stays alive until the day is over, and a record streak glows in ember.
- **Days, Weeks and Months**: Look back 30 days, 12 weeks or 12 months. Weeks start on your locale's first day; numbers show each week's or month's average, while money and time show its total.
- **Compared With Before**: See how many entries today, this week or this month has against the one before.
- **Logged Per Period**: A bar chart shows how often the tracker was logged in each day, week or month, with the current one in ember. Tap a bar to read it.
- **Streaks On Home**: A tracker with a streak of two days or more shows it on its slate with an ember flame.
- **Personal Records**: Numbers show their record and the day it was set; money and time show their best day.
- **Today On Home**: Home says how many trackers have been logged today, with an ember meter.
- **Labels Where They Help**: Each chart names its busiest day or its latest value right on the chart, and nothing else.
- **Tap To Read, Tap Again To Let Go**: Picking a bar or point on a chart and tapping it again now clears the pick.
- **What Each Field Adds Up To**: Numbers show their average and range on a line chart, money and time show their total and average on daily bars, ratings show their average and how often each score was given, yes or no shows its yes rate, and choices show how often each was picked.
- **Exact Numbers**: Totals and averages are computed exactly, in each field's own unit and currency, never with rounding errors.
- **Readable Without Color**: Every chart can switch to a table, and screen readers hear a summary of each chart.

## [2.0.5] - 2026-11-14

- **Tracker Pages**: Tapping a tracker on Home opens its own page with its totals (entries, the last seven days, days logged) and every entry grouped by day, newest first. Editing the tracker moves to the pencil at the top.
- **Calendar**: Switch any tracker to a month calendar. Days with entries wear the tracker's pigment, today is ringed in ember, and tapping a day shows only its entries. A far month offers a way back to this one.
- **Entry Details**: Tap an entry to see all of it: the full date and time, the zone it was logged in when that differs from the phone's, every value and the note, which can be selected and copied.
- **Edit and Undo**: Edit or delete an entry from its details or by long-pressing it. A deleted entry can be brought back with Undo, exactly as it was.
- **Organize Home**: Long-press a tracker on Home to move it earlier or later, edit it, or archive it with Undo.
- **The Right Day**: Entries are grouped by the day they happened in the time zone they were logged in, so a late night entry never jumps to the next day after travelling.
- **Archived Trackers**: Archived trackers have their own page from Home, and each can be brought back from its tracker page.
- **Shorter Times**: Durations under an hour read as minutes, such as "32 m".
- **Long Histories**: A tracker page shows its newest entries first and loads older ones with Show older, while its totals and calendar still count every entry. Years of daily logging stay quick to open.

## [2.0.4] - 2026-11-14

- **Logging Entries**: Log to any tracker from the button on its slate. Every field type has its own input: numbers with their unit, money with its currency, hours and minutes, rating and yes or no buttons, choices, text and a date picker.
- **One Tap Habits**: A tracker made of a single yes or no logs "Yes" straight from Home, with a check on its slate instead of a plus.
- **When It Happened**: Each entry starts at the current time and can be moved to any day and time. It keeps the time zone it was logged in, so the day stays right after travelling.
- **Exact Values**: Input with more decimals than a field allows is refused in place instead of being rounded, and required fields say so before saving.
- **Notes**: Any entry can carry a note, and a note alone is enough to log.
- **Same As Last Time**: A new entry can be filled with the values of the tracker's latest one in one tap, keeping its own time and note.
- **Screen Reader Names**: The main action on every page (Save, Log it, New tracker) is now announced by its name.
- **Readable Values**: Values are shown in the device's language and number format, with each currency's own symbol and digits.

## [2.0.3] - 2026-11-13

- **Tracker Builder**: Create and edit trackers with a live preview, a name and description, an icon, a pigment and up to 24 fields. Long-press a field to move or remove it.
- **Field Settings**: Each field type has its own settings: unit, decimals and limits for numbers, currency for money, the scale for ratings, editable choices, and one or several lines for text. Any field can be required.
- **Clear Problems**: Mistakes show under the input that caused them, such as a duplicate field name or a limit with too many decimals, and nothing is saved until they are fixed.
- **Safe Edits**: Unsaved changes ask before leaving, a half-built tracker survives the app being closed by the system, and changing a field's type warns that its logged values will be cleared.
- **Archive and Delete**: Archive a tracker to hide it from Home, or delete it and everything logged to it after a confirmation.
- **Eonslate's Own Look**: A new "Slate and Ember" identity: a slate blue brand palette with an ember accent as the default instead of wallpaper colors, seventeen mineral pigments for trackers, condensed display lettering, slate tile icons and a slanted slate for empty states.
- **Home Grid**: Home now shows trackers as pigment-tinted slates in a grid that adapts to the screen, with a New tracker button.

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
