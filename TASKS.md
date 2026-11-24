# Eonslate - Tasks

> **Project:** Eonslate
> **Version:** 2.2.5
> **Last Updated:** 2026-11-24

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
| 29 | 2.1.8 | Goals for number and duration fields, an As needed schedule, sectioned trackers | Done |
| 30 | 2.1.8 | Intervals: due after a time or an amount since the last log; learned rhythms | Done |
| 31 | 2.1.9 | Consistency: year heat map, consistency ring, stat tiles; spending heat map | Done |
| 32 | 2.1.9 | Patterns: weekday breakdowns for trackers and spending, all-trackers overview | Done |
| 33 | 2.1.9 | Widgets: Today checklist and money summary; reminder and editor touches | Done |
| 34 | 2.1.9 | Close the gaps: widget counts, week start, icons, wording, checked on the phone | Done |
| 35 | 2.1.9 | Top bar with bell and profile, notifications, faster start, clear text fields | Done |
| 36 | 2.2.0 | Account cards: kind and last digits, a card carousel, transactions bound to cards | Done |
| 37 | 2.2.0 | Parts: a choice field shown as sub trackers, each with its own last, next and log | Done |
| 38 | 2.2.0 | Onboarding: name, currency, first account, starters; odometer pace between logs | Done |
| 39 | 2.2.0 | Parts set up in place in the editor; usual amount counts only on chosen weekdays | Done |
| 40 | 2.2.0 | Release: theme-aware splash and mark, SMS in every build, docs and website | Done |
| 41 | 2.2.1 | People and businesses on transactions; pending or cleared | Done |
| 42 | 2.2.1 | A person's chat: lend, borrow, split and settle | Done |
| 43 | 2.2.2 | A clean slate: no preconfigured trackers or templates; build your own | Done |
| 44 | 2.2.2 | Balance floors per account and overall, with a warning before going under | Done |
| 45 | 2.2.2 | Piggy banks set aside inside an account | Done |
| 46 | 2.2.3 | Audit fixes: people apart from businesses, Recurring, one row rule, month default | Done |
| 47 | 2.2.3 | Structure: Home as a glance, Money list-first, one place per setting | Done |
| 48 | 2.2.3 | Fixed deposits that grow on the card and are recorded at maturity | Done |
| 49 | 2.2.4 | Payees with totals, price changes, attachments (images, PDFs, links), tracker cards | Done |
| 50 | 2.2.5 | Website, no steering examples, audit fixes, and the first release | Done |

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
- [x] Service templates use existing categories (one was saved under an unlisted category)
- [x] A clear "Choose a service" entry in the subscription editor, not only behind Icon › Change
- [x] Rename the variable switch to "Amount varies"; "Confirm each amount" contradicts "Add the estimate when due"
- [x] Switch rows in the subscription and budget editors get rounded group shapes
- [x] Delete all data no longer says "Settings stay"
- [x] One minus sign everywhere (Net showed a hyphen where Out showed a minus sign)
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

### 2.1.8 Goals, as needed, sections
- [x] Number and duration fields can have a target, per day or per week, at least or at most
- [x] Goals live in the field's settings, so backups carry them with no migration
- [x] Tracker cards show today's progress toward the goal as a meter
- [x] Insights: goal met on how many days or weeks, and the current run
- [x] A day counts as done for streaks and Today once its daily goals are met
- [x] An As needed schedule, never due by itself and without a streak
- [x] Trackers sort into Due today, Not today and As needed once schedules differ; one grid otherwise
- Everything optional stays hidden until turned on, so a simple tracker stays simple

### 2.1.8 Intervals
- [x] An As needed tracker can be due again after a time since its last log: days, weeks, months or years
- [x] Or after a number field grows by an amount since the last log, such as an odometer read from any entry
- [x] Intervals can follow one choice option, so one tracker holds several jobs on their own clocks
- [x] Without any setup, an As needed tracker learns its usual rhythm from three or more logs, per choice and by a growing number
- [x] Cards show what is next: the amount left before an interval, or the learned rhythm and when it next comes
- [x] Insights: each interval with when it was last done, and learned rhythms for jobs without one
- [x] A due interval puts the tracker under Due today; logging the job starts its clock again
- [x] One optional "Due again" list under As needed; nothing new for trackers that do not use it
- Nothing is specific to one use: a bike, haircuts or a shampoo bottle are all plain custom trackers

### 2.1.9 Consistency
- [x] Insights: a year heat map of logged days, coloured by amount or goal reached, tap a day for its entries
- [x] Insights: a consistency ring, the share of due days logged since the tracker started
- [x] Stat tiles: started on, best streak, current streak filling toward the best
- [x] Transactions: a spending heat map by day, tap a day to filter to it
- [x] Heat map colours from one sequential ramp, readable in light, dark and pure black

### 2.1.9 Patterns
- [x] Insights: entries by weekday
- [x] Transactions: spending by weekday for the shown range
- [x] Trackers tab: an overview card with consistency across all trackers and the most consistent ones
- [x] Overview filters by tag

### 2.1.9 Widgets and touches
- [x] Today widget: trackers due today, tap to log or open
- [x] Money widget: balance and spent this month in the shown currency
- [x] Widget previews in the launcher picker
- [x] A reminder notification clears when that tracker is logged in the app
- [x] New-item editors open with the keyboard on the first field

### 2.1.9 Closing the gaps
Checked on the phone with sample data holding goals, a weekday schedule and As needed trackers with and without intervals, in light and dark, with the widgets on the home screen.
- [x] Today widget follows daily goals and due intervals, so it matches the Trackers tab
- [x] Money widget uses the chosen number format
- [x] Weekday chips in schedules and reminders start on the app's week start
- [x] 52 tracker icons instead of 12, each with a spoken name
- [x] Goal lines say the unit once; interval tiles lead with what is left
- [x] Widget sizes and the money widget's name in the picker

### 2.1.9 Top bar, notifications and speed
- [x] Home bar: the mark and name; every tab ends with a bell and the profile picture
- [x] Notifications: SMS finds, payments to confirm, budgets over, intervals due, payments coming up
- [x] Profile: name, then Settings and the places visited now and then
- [x] Splash holds until Home has its figures; shared reads; sums off the main thread
- [x] Text fields clear inside

### 2.2.0 Account cards
Checked on the phone with a bank, a credit card, a wallet and cash.
- [x] Account kind and last 4 digits (database version 15)
- [x] Card carousel on Home; a tap filters Transactions to that account
- [x] Account filter chip; rows and pickers show the digits
- [x] Payment SMS match an account by its last digits

### 2.2.0 Parts
Checked on the phone with a tracker whose parts were due by amount, by time and by last log.
- [x] Show as parts on a single choice field (no database change)
- [x] Part rows on the tracker card and atop Insights
- [x] A part's log button opens the entry with that part picked

### 2.2.0 Onboarding and odometer pace
- [x] Steps: intro, name, currency and first account, starters; skip anytime; restore a backup
- [x] Distance intervals count on at the learned pace between readings
- [x] Optional usual amount a day on an interval
- [ ] Walk the onboarding on the phone from a fresh install (covered by the screen test so far)

### 2.2.0 Parts in the editor
Checked on the phone: a usual amount a day on chosen weekdays gave the expected amount left and due day.
- [x] Parts section in the tracker editor; a part's sheet has its job fixed
- [x] Counts on weekdays for the usual amount a day and the learned pace

### 2.2.0 Release
- [x] Theme-aware splash with the logo; matching window background
- [x] Top bar mark tinted by theme with its transparency
- [x] SMS inbox scanning in every build
- [x] RELEASES.md, README and website

### 2.2.1 People and pending
- [x] Parties (person or business) with unique names; database version 16
- [x] Paid to / Received from in the editor with recent picks; SMS and CSV fill it in
- [x] Pending flag, row label and filter

### 2.2.1 People
Checked on the phone: a lent payment showed "Owes you" in the chat, and Settle up cleared it.
- [x] Shares on transactions (database version 17); own part counts as spending
- [x] People strip, People list, a chat per person or business
- [x] Lent, borrowed, paid back, got back, equal splits; settle up
- [x] Editor extras behind More

### 2.2.2 A clean slate
- [x] New tracker opens blank; template gallery removed
- [x] Onboarding without starter trackers
- [x] No default categories; category optional on payments; last category deletable

### 2.2.2 Balance floors
Checked on the phone: an expense that took a balance under its floor warned by how much and offered Save anyway.
- [x] Account floor (database version 18) and an overall floor in settings
- [x] Editor warning and Save anyway; Notifications and cards show breaches

### 2.2.2 Piggy banks
Checked on the phone: a piggy bank filled toward its goal and said what was left to go.
- [x] Piggy banks and their moves (database version 19), in backups
- [x] Add and take out; free and set-aside amounts on accounts

### 2.2.3 Audit fixes: names and rows
From walking every screen on the moto with full data.
- [x] People strip shows people; the list groups people and businesses
- [x] Subscriptions renamed Recurring, grouped Going out and Coming in
- [x] One row rule; the usual account goes unsaid
- [x] Transactions opens on this month; lighter heat map; less instruction text

### 2.2.3 Structure
Checked on the phone with full data: Home fits one screen, Money shows the list on the first screen.
- [x] Home: balance, cards, today
- [x] Money: links row, filters, list; Accounts, Budgets, Piggy banks and Insights as pages
- [x] Profile and Settings without duplicates; Archived on Trackers

### 2.2.3 Fixed deposits
Checked on the phone: quarterly compounding for a year matches what bank calculators give.
- [x] A deposit is an account with its terms; database version 20
- [x] Paid from an account by a transfer, or already there as its opening balance
- [x] Worth today, earned and maturity value on its card, page and sheet
- [x] Close or close early: interest or charges recorded, then paid into an account
- [x] Matured and maturing deposits in Notifications; overall floor leaves deposits out
- [x] Delete removes its transactions; backups keep the terms

### 2.2.4 Payees, price changes and attachments
Checked on the phone: naming a recurring payment's payee named its past payments, and the payee's page totals them.
- [x] Recurring payments have a payee; posted payments carry it; naming it later names the past
- [x] Payee page: paid this year, month and in all, pinned; by category; their recurring payments
- [x] People and businesses sorted by what you paid this year
- [x] New price from a day, up or down: past days re-price what was posted, future days wait
- [x] Tracker cards say when they were last logged; Due today carries today's progress
- [x] Attachments: photo, camera, PDF and link per transaction; files by hash; in backups; Undo keeps them
- [ ] On the phone: the New price from field and attaching a photo, PDF and link (covered by tests; not yet seen on the device)

### 2.2.5 Website, audit and first release
Design to be revisited; the structure and live data are in place.
- [x] Nav, hero with live GitHub stats, features, privacy, design, tech, developer, credits, FAQ, download, footer
- [x] Download buttons link the latest release's APK and show its version
- [x] Favicon light on dark tabs; light logo on the dark page
- [x] Placeholders, preset terms and example wording removed from the app and README
- [x] Template catalog, Home starter chips and the editor's template route removed; `templateId` kept only so old data round-trips
- [x] Subscription wording replaced with recurring payment in every user-facing string and the export name
- [x] Unused strings removed from every module
- [x] Price and payee changes find posted payments by id range; party deletion matches shares exactly; single-row lookups
- [x] Ledger reads conflated; Transactions filters and sorts off the main thread
- [x] One `FinanceRequest` replaces seven open and opened pairs; dead budget and category hooks from Home removed
- [x] `FinanceViewModel` tests: first account, payees by name, Undo with attachments, refused and overlapping saves
- [x] Website: repeated privacy claims, swatches, slogans and hard-coded versions removed
- [ ] On the phone: empty Trackers tab, Recurring editor titles, and a quick add, account card and SMS review from Home
- [x] RELEASES.md: v2.2.5 as Eonslate's first release
