# Eonslate - Tasks

> **Project:** Eonslate
> **Version:** 2.1.3
> **Last Updated:** 2026-11-18

## Roadmap

Each step is planned as one release commit.

| # | Version | Step | Status |
| ---: | :--- | :--- | :--- |
| 1 | 2.0.1 | Project foundation, toolchain, Expressive design system | Done |
| 2 | 2.0.2 | Tracker domain model, Room database, schema export, migration tests | Done |
| 3 | 2.0.3 | Tracker builder: create and edit trackers and their fields | Done |
| 4 | 2.0.4 | Logging entries: quick add, typed inputs for every field type | Done |
| 5 | 2.0.5 | Tracker detail: list and calendar views, edit and delete with undo | Done |
| 6 | 2.0.6 | Insights: charts, streaks, totals and averages | Done |
| 7 | 2.0.7 | Templates gallery, welcome flow, and the brand: logo (`Eonslate.png`, `Eonslate.svg`) and themed adaptive icons | Done, except the brand |
| 8 | 2.0.7 | Settings, About and Licenses screens | Done |
| 9 | 2.0.8 | Backup and restore: encrypted archives, recoverable restores | Done |
| 10 | 2.0.9 | Local reminders that reschedule after each alert and after reboot | Done |
| 11 | 2.0.9 | Search, tags, filters and saved views | Done |
| 12 | 2.1.0 | Finance: accounts, exact money, transfers | Done |
| 13 | 2.1.0 | Finance: budgets and recurring payments | Done |
| 14 | 2.1.1 | Finance: optional on-device SMS reader | Done |
| 15 | 2.1.2 | CSV import and export, log widget and Quick Settings tile, accessibility, screenshot blocking, architecture tests and release readiness | Done |
| 16 | 2.1.2 | Four tabs, money overview, transactions and subscriptions redesign, budgets in Transactions, new icons | Done |
| 17 | 2.1.3 | Automatic payment SMS detection; subscription icons, start date, notes; filters, sort, quick add, delete all | Done |
| 18 | 2.1.3 | Transaction selection, finance charts, filtered CSV, budget styling, category management, hourly billing | Done |
| 19 | 2.1.4 | Payment SMS parity, personalization, and v2.0.0 JSON restore | Planned |


## v2.0.0 parity checklist

Everything the old app did must work here, with a better design. Done items are checked.

**SMS**
- [x] Parse a pasted payment SMS; scan the inbox (7, 30 or 90 days) and review each payment
- [x] Detect payment SMS automatically when the app opens (setting, off by default)
- [x] Auto-approve detected payments (setting)
- [x] Home banner with the count of detected payments, one tap to review
- [x] Approve all / discard all; discarded messages never come back
- [ ] Parser parity with v2.0.0 (300+ brands, accounts and cards, merchants, UPI refs, suggested category, confidence)

**Home**
- [x] Net balance, month in and out, spending by category, recent activity, budget status and alerts
- [x] Quick "+ Expense" and "+ Income" from Home

**Transactions**
- [x] Add, edit, delete; search; date presets; CSV export and import
- [x] Category filter chips
- [x] Sorting
- [x] Selection mode with batch delete
- [x] Expense breakdown chart view
- [x] Export only the filtered transactions

**Budgets**
- [x] Category budgets with periods, progress, over-budget warning
- [x] Budget name, icon and color
- [x] Allocation chart view

**Subscriptions**
- [x] Recurring income and expenses with hourly, daily, weekly, monthly, and yearly cycles, variable amounts, pause, and next payment
- [x] Search subscriptions
- [x] Monthly cost chart view (treemap or ranked bars)
- [x] Service directory (113 services, no brand logos)
- [x] Custom icons (PNG, JPEG, WebP, SVG), sanitized, included in backups
- [x] Notes on a subscription
- [x] Start date on a subscription

**Categories**
- [x] Pick from chips, most used first, defaults
- [x] Manage categories: add, rename, icon, color, type (income, expense, both)

**Settings and data**
- [x] Theme, pure black, accents, haptics, currency, backup and restore
- [ ] Display name
- [ ] Number format (locale) and date format
- [x] Clear all local data, with confirmation (undoable)
- [ ] Restore v2.0.0 JSON backups

## Waiting on a decision

- **Brand**: the logo (`Eonslate.png`, `Eonslate.svg`) and themed adaptive icons, planned in 2.0.7, wait on the logo discussion. They land in the first version after a design is chosen. The launcher still uses the placeholder icon.
  The decision needs: a mark that works at 24 dp as a monochrome themed icon, a full-color launcher version on the slate and ember palette, and the wordmark for `Eonslate.svg`.
