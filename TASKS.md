# Eonslate - Tasks

> **Project:** Eonslate
> **Version:** 2.1.4
> **Last Updated:** 2026-11-19

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
| 7 | 2.0.7 | Templates gallery, welcome flow, and the brand: logo and themed adaptive icons | Done, with the brand added in 2.1.4 |
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
| 19 | 2.1.4 | Payment SMS parity, personalization, and v2.0.0 JSON restore | Done |
| 20 | 2.1.4 | Transactions parity: filters, sorts, notes, full breakdown, atomic delete | Done |
| 21 | 2.1.5 | Budgets parity: spent chart, CSV, details, scope, early warnings | Planned |
| 22 | 2.1.5 | Subscriptions parity: filters, net flow, full chart, CSV, quick actions, variable posting | Planned |
| 23 | 2.1.6 | Home cash flow: ranges, period details, interactive categories | Planned |
| 24 | 2.1.6 | SMS parity: rescans, inline review, feedback, duplicates, provenance | Planned |
| 25 | 2.1.6 | Categories, settings and data: delete, full reset, wide nav, icons | Planned |

## v2.0.0 parity

From the v2.0.0 comparison on 2026-11-13. Upgrade and legacy-import paths are out of scope (no existing users).

### 2.1.4 Transactions
- [x] Type filter: All, Expense, Income
- [x] Date filters: 90 days and custom From / To
- [x] Sorts: lowest amount, category
- [x] Category filter lists configured categories, not just used ones
- [x] Full expense breakdown: every category, tappable, shown for one category too
- [x] Separate description and notes (2,000 chars), note preview in the list
- [x] Category required, preselected for new transactions
- [x] Same-day ordering by creation time
- [x] Matching count, "Show older" paging; swipe to delete with Undo
- [x] Clear selection when filters change
- [x] Atomic batch delete in one database transaction
- [x] Settings shortcut on finance tabs

### 2.1.5 Budgets
- [ ] Chart compares actual spending, grouped by currency, shown for one budget too
- [ ] Budget CSV: name, category, limit, spent, period, utilization
- [ ] Rows show period, remaining and percentage used
- [ ] Hourly periods
- [ ] New budgets start today
- [ ] Optional all-accounts scope for a category
- [ ] Home warns from 80% with the percentage
- [ ] Home budget card opens budgets; empty state with an add action

### 2.1.5 Subscriptions
- [ ] Type filter: All, Expenses, Incomes
- [ ] Monthly income, expenses and net per currency
- [ ] Chart covers every active item matching search and type, income included
- [ ] Recurring CSV: name, amount, cycle, next billing, active, monthly equivalent
- [ ] Row actions: pause/resume, edit, delete; create a schedule paused
- [ ] Rows show exact next date, full interval ("every 3 months"), Variable badge, note preview
- [ ] Color picker; income templates; template fills only an empty name
- [ ] Name required; notes up to 2,000 chars
- [ ] Variable schedules: "Add estimate automatically" or "Ask each time"
- [ ] Skip posting when a matching transaction (name, date, type) exists
- [ ] Catch up on resume, after restore, and every elapsed cycle on save
- [ ] Label nominal hourly rates apart from expected postings

### 2.1.6 Home cash flow
- [ ] Ranges: 7 days, 30 days, 6 months, 1 year, all time
- [ ] Spending, income and net for the selected range
- [ ] Tap a period for its spending, income and top category
- [ ] Interactive category chart without folding into Other
- [ ] Currency badge and animated balance; category subtitle on recent rows

### 2.1.6 SMS
- [ ] Rescan on resume and right after enabling detection or granting access
- [ ] Scan past SMS from Settings; pending banner on Transactions
- [ ] Exclude handled messages before the 200-item cap
- [ ] Pick category and approve on each card
- [ ] Cancelling an opened item keeps it pending
- [ ] One discard state for automatic and manual scans
- [ ] Bulk approval reports added, skipped and failed items
- [ ] Duplicate check against existing ledger entries, manual ones included
- [ ] Keep sender and account/card ending on approved payments
- [ ] Sender fallbacks (prefix, substring), alphabetic references

### 2.1.6 Categories, settings and data
- [ ] Delete a category; keep history, drop it from suggestions; guard the last one
- [ ] Delete all data also resets preferences and removes icon files
- [ ] Preferences in backups
- [ ] Navigation rail at 840 dp and wider
- [ ] About: package ID and changelog link
- [ ] Debug StrictMode
- [ ] Custom icons keep aspect ratio up to 512 px
- [ ] Validate restored raster icons; clean up replaced, deleted and cancelled icon files

### Out of scope
- Package ID, Android 7 support and old-install handoff
- Legacy JSON and CSV import fixes, legacy settings import, legacy export format
- Browser app
- Accent palettes, launcher art, Bills/Salary defaults, 60-char display name
