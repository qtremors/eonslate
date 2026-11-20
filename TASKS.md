# Eonslate - Tasks

> **Project:** Eonslate
> **Version:** 2.1.7
> **Last Updated:** 2026-11-20

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
| 21 | 2.1.5 | Budgets parity: spent chart, CSV, details, scope, early warnings | Done |
| 22 | 2.1.5 | Subscriptions parity: filters, net flow, full chart, CSV, quick actions, variable posting | Done |
| 23 | 2.1.6 | Home cash flow: ranges, period details, interactive categories | Done |
| 24 | 2.1.6 | SMS parity: rescans, inline review, feedback, duplicates, provenance | Done |
| 25 | 2.1.6 | Categories, settings and data: delete, full reset, wide nav, icons | Done |
| 26 | 2.1.7 | Device test fixes and a distinct chart for each job | Done |
| 27 | 2.1.7 | One currency at a time, unsaved-change prompts, final fixes | Done |
| 28 | 2.1.7 | Schedules: due days per tracker, a Today filter, honest daily progress | Done |
| 29 | 2.1.8 | Goals: daily or weekly targets for number and duration fields | Planned |
| 30 | 2.1.8 | Consistency: year heat map, consistency ring, stat tiles; spending heat map | Planned |
| 31 | 2.1.9 | Patterns: weekday breakdowns for trackers and spending, all-trackers overview | Planned |
| 32 | 2.1.9 | Widgets: Today checklist and money summary; reminder and editor touches | Planned |

## v2.0.0 parity

From the v2.0.0 comparison on 2026-11-13. Upgrade and legacy-import paths are out of scope (no existing users). Versions 2.1.4 to 2.1.6 are done; 2.1.7 holds what the device run found.

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
- [x] Chart compares actual spending, grouped by currency, shown for one budget too
- [x] Budget CSV: name, category, limit, spent, period, utilization
- [x] Rows show period, remaining and percentage used
- [x] Hourly periods
- [x] New budgets start today
- [x] Optional all-accounts scope for a category
- [x] Home warns from 80% with the percentage
- [x] Home budget card opens budgets; empty state with an add action

### 2.1.5 Subscriptions
- [x] Type filter: All, Expenses, Incomes
- [x] Monthly income, expenses and net per currency
- [x] Chart covers every active item matching search and type, income included
- [x] Recurring CSV: name, amount, cycle, next billing, active, monthly equivalent
- [x] Row actions: pause/resume, edit, delete; create a schedule paused
- [x] Rows show exact next date, full interval ("every 3 months"), Variable badge, note preview
- [x] Color picker; income templates; template fills only an empty name
- [x] Name required; notes up to 2,000 chars
- [x] Variable schedules: "Add estimate automatically" or "Ask each time"
- [x] Skip posting when a matching transaction (name, date, type) exists
- [x] Catch up on resume, after restore, and every elapsed cycle on save
- [x] Label nominal hourly rates apart from expected postings

### 2.1.6 Home cash flow
- [x] Ranges: 7 days, 30 days, 6 months, 1 year, all time
- [x] Spending, income and net for the selected range
- [x] Tap a period for its spending, income and top category
- [x] Interactive category chart without folding into Other
- [x] Currency badge and animated balance; category subtitle on recent rows

### 2.1.6 SMS
- [x] Rescan on resume and right after enabling detection or granting access
- [x] Scan past SMS from Settings; pending banner on Transactions
- [x] Exclude handled messages before the 200-item cap
- [x] Pick category and approve on each card
- [x] Cancelling an opened item keeps it pending
- [x] One discard state for automatic and manual scans
- [x] Bulk approval reports added, skipped and failed items
- [x] Duplicate check against existing ledger entries, manual ones included
- [x] Keep sender and account/card ending on approved payments
- [x] Sender fallbacks (prefix, substring), alphabetic references

### 2.1.6 Categories, settings and data
- [x] Delete a category; keep history, drop it from suggestions; guard the last one
- [x] Delete all data also resets preferences and removes icon files
- [x] Preferences in backups
- [x] Navigation rail at 840 dp and wider
- [x] About: package ID and changelog link
- [x] Debug StrictMode
- [x] Custom icons keep aspect ratio up to 512 px
- [x] Validate restored raster icons; clean up replaced, deleted and cancelled icon files

### 2.1.7 Device test fixes and distinct charts
From a second v2.0.0 source comparison and a full run on a moto g(40) fusion (Android 12) on 2026-11-14.

**Charts** (each chart suits its data instead of every one being ranked bars)
- [x] Spending by category: an interactive donut with a legend (Home and Transactions)
- [x] Subscriptions per month: a treemap of every active item, income set apart
- [x] Budgets: a ring per budget for the share used, plus total limit and total spent
- [x] Cash flow: Spending, In vs out (paired bars) and Net (bars above and below zero)

**v2.0.0 details still missing**
- [x] Budgets header with total limit and total spent
- [x] Empty states with an add action: no matching transactions, no subscriptions
- [x] Income schedules say "Payout" where expenses say "Next"

**Found on the device**
- [x] Custom subscription images fill their tile (centre crop), the full image still stored
- [x] Service templates use existing categories (Netflix was saved as an unlisted "Streaming", income as "Income")
- [x] A clear "Choose a service" entry in the subscription editor, not only behind Icon › Change
- [x] Rename the variable switch to "Amount varies"; "Confirm each amount" contradicts "Add the estimate when due"
- [x] Switch rows in the subscription and budget editors get rounded group shapes
- [x] Delete all data no longer says "Settings stay"
- [x] One minus sign everywhere (Net shows "-₹" while Out shows "−₹")
- [x] Home "By category" says it covers this month
- [x] After Add budget from Home, Transactions scrolls to the new budget
- [x] Due labels in brackets start lower case: "(in 30 days)"
- [x] Subscription rows drop "Since …" (it is in the details) so they fit in two lines
- [x] Wide screens cap the content width
- [x] Custom date range dialog title has room above it
- [x] Welcome mentions money, budgets and subscriptions, not only trackers

**Not yet checked on the device**
- [ ] SMS inbox scan on a phone with a SIM (the test phone has no SIM and its SMS provider is disabled; a scan there ends with "No new payments found")
- [x] Sharing an SMS into the app
- [x] All-accounts budget scope with a second account
- [x] Backup file round trip with settings

### 2.1.7 One currency at a time and final fixes
From a stress test with a generated backup (40 subscriptions, 3,147 transactions, five currencies) on 2026-11-14.

- [x] Totals and charts show one currency at a time on every tab, picked from the currency tag; rows keep their own currency
- [x] Editors ask before discarding unsaved changes
- [x] Overdue subscriptions read "N days overdue"
- [x] New transactions, subscriptions and budgets start in the most used account
- [x] Folded chart slices read "N more", so they never share a name with the "Other" category
- [x] A zero subscription total is not drawn in red

### Out of scope
- Package ID, Android 7 support and old-install handoff
- Legacy JSON and CSV import fixes, legacy settings import, legacy export format
- Browser app
- Accent palettes, launcher art, Bills/Salary defaults, 60-char display name

## Trackers, goals and insights

### 2.1.7 Schedules
- [x] A tracker can be due every day, on chosen weekdays, or a number of times a week
- [x] Room migration for due days, with schema export and a migration test; backups carry it, older files read as every day
- [x] Tracker editor: a schedule row with connected weekday choices
- [x] Trackers tab: a Due today filter beside the tags shows only what is due
- [x] "Logged today" progress counts only trackers due today
- [x] Reminders offer the schedule's days by default
- [x] Streaks count due days only, so a day off the schedule never breaks one

### 2.1.8 Goals
- [ ] Number and duration fields can have a target, per day or per week, at least or at most
- [ ] Backups and CSV carry the target
- [ ] Tracker cards show today's progress toward the goal as a meter
- [ ] Insights: goal met on how many days, and the current run of days met
- [ ] A day counts as done for streaks and Today once its goal is met

### 2.1.8 Consistency
- [ ] Insights: a year heat map of logged days, coloured by amount or goal reached, tap a day for its entries
- [ ] Insights: a consistency ring, the share of due days logged since the tracker started
- [ ] Stat tiles: started on, best streak, current streak filling toward the best
- [ ] Transactions: a spending heat map by day, tap a day to filter to it
- [ ] Heat map colours from one sequential ramp, readable in light, dark and pure black

### 2.1.9 Patterns
- [ ] Insights: entries by weekday
- [ ] Transactions: spending by weekday for the shown range
- [ ] Trackers tab: an overview card with consistency across all trackers and the most consistent ones
- [ ] Overview filters by tag

### 2.1.9 Widgets and touches
- [ ] Today widget: trackers due today, tap to log or open
- [ ] Money widget: balance and spent this month in the shown currency
- [ ] Widget previews in the launcher picker
- [ ] A reminder notification clears when that tracker is logged in the app
- [ ] New-item editors open with the keyboard on the first field
