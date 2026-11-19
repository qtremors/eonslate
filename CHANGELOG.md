# Eonslate Changelog

> **Project:** Eonslate
> **Version:** 2.1.5
> **Last Updated:** 2026-11-19

---

## [2.1.5] - 2026-11-19

- **Budget spending**: A chart of what each budget has spent this period, per currency.
- **Budget rows**: How often each renews, what is left or over, and the share used.
- **All accounts**: A category budget can count every account in its currency.
- **Hourly budgets**: Counted by whole days.
- New budgets start today.
- Export budgets as CSV, with spent, remaining and utilization.
- Home warns from 80% of a budget, shows a Manage budgets button, and offers Add budget when there are none.
- Recent transactions on Home show their category.
- **Filter subscriptions**: All, Expenses, or Income.
- **Per month**: Income, expenses and net for each currency.
- **Chart**: Every active subscription and income matching the search, by its monthly amount. Tap one to open it.
- **Rows**: The exact next date, the full interval ("Every 3 months"), whether the amount varies, and the start of the notes.
- **Long press** a subscription to pause, resume, edit or delete it. Resuming skips what fell due while paused.
- **Varying amounts**: Choose to add the estimate when due, or wait for the real amount.
- Payments already entered by hand with the same name, day and type are not added again.
- Missed payments are caught up whenever the app returns, after a restore, and all at once on save.
- Export subscriptions as CSV, with the monthly equivalent.
- Pick a subscription color apart from its icon. Income shows income templates.
- New subscriptions can start paused, need a name, and take notes of up to 2,000 characters.
- Hourly payments show that they are added at most once a day.

## [2.1.4] - 2026-11-19

- Recognize more payment SMS formats, including masked accounts and cards, merchants, and UPI references.
- Open shared payment messages from Home while Eonslate is running.
- Auto-add high-confidence payments and review the rest.
- Set a display name for Home and choose number and date formats.
- Restore Earnslate 2.0.0 JSON backups with Undo.
- Match system bar icon contrast to the app theme.
- See the black Eonslate logo on white on the launcher, splash screen, and About page. The launcher icon also includes a monochrome layer for Android theming.
- **Filters**: Show only income or expenses, the last 90 days, or any range of dates.
- **Sort**: Smallest first, or by category.
- **Categories**: Every category can be picked as a filter, not just the ones already used.
- **Spending by category**: Lists every category with its share, even when there is only one. Tap one to filter.
- **Notes**: Transactions have a description and separate notes, shown under each row.
- New income and expenses start in the most used category, and need one to save.
- Transactions on the same day stay in the order they were added.
- See how many transactions match, and load older ones 50 at a time.
- Swipe a transaction away to delete it, with Undo.
- Deleting selected transactions happens all at once or not at all.
- Selection drops transactions a new filter hides.
- Open Settings from Transactions and Subscriptions.
- **Subscription history**: Open a subscription to see recorded payments, monthly history, and a 12-month forecast.

## [2.1.3] - 2026-11-18

- **Payment SMS**: Found automatically when the app opens (Settings, off by default), with an option to add them without review.
- **Review**: A Home banner shows how many were found; add all, review one by one, or discard. Discarded ones never come back.
- **Subscription icons**: Pick from 113 services or use your own PNG, JPEG, WebP or SVG; images are kept in backups.
- **Subscription details**: Start date and notes.
- **Quick add**: + Income and − Expense on Home open the editor ready to go.
- **Delete all data**: From Backup and restore, after a confirmation; undo brings it all back.
- **Find things**: Filter transactions by category, sort by newest, oldest or largest, and search subscriptions.
- **Select transactions**: Long-press to select, tap to add more, then delete them together.
- **Charts**: Spending by category on Transactions, following the filters; monthly cost per subscription; budget limits side by side.
- Export matching transactions in the selected sort order.
- Name budgets and choose their icons and colors.
- Add and rename categories, set their icons and colors, and choose income, expense, or both.
- Schedule hourly subscriptions with a billing time.

## [2.1.2] - 2026-11-18

- Import and export tracker entries as CSV.
- Import and export finance transactions as CSV.
- Review import counts before adding records.
- Open a chosen tracker’s log from a Home screen widget.
- Choose a tracker from the Quick Settings tile.
- Block screenshots from Privacy settings.
- Screen readers announce section headings and switch states.
- **Tabs**: Home, Transactions, Subscriptions and Trackers in a floating bar; each tab keeps its place.
- **Home**: Balance, this month in and out, spending trend, spending by category, budgets, payments due, recent transactions and today's trackers.
- **Transactions**: Period chips, in/out/net totals, days grouped, income green and expenses red.
- **One account by default**: Account pickers and transfers appear once you add a second account.
- **Dates**: Picked from a calendar instead of typed.
- **Subscriptions tab**: Monthly cost up front; each subscription with its next payment and amount.
- **Budgets**: Lead the Transactions tab as meters, red when over.
- **Icons**: New tab icons; letter tiles for categories and subscriptions; one menu for rarer actions.
- **Categories**: Picked from chips, most used first, with icons for the common ones; "New" for any other.
- **Touch**: Cards and tracker slates spring when pressed.

## [2.1.1] - 2026-11-17

- Review payment details from pasted, shared or scanned SMS.
- Skip payment alerts already recorded.
- Search and filter transactions with per-currency totals.
- Set category budgets with custom cycles.
- Schedule income and bills with custom intervals.
- Pause schedules or confirm variable payments.

## [2.1.0] - 2026-11-17

- Track account balances in their own currencies.
- Record income and expenses in exact amounts.
- Transfer money between accounts in the same currency.
- Backups include accounts and transactions.
- Set a monthly budget for each account.
- Review spending by month in Plans.
- Post weekly and monthly payments when you open the app.
- Backups include budgets and recurring payments.

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
