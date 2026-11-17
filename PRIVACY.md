# Privacy Policy for Eonslate

**Last Updated:** 2026-11-17

Eonslate is a personal project built with a privacy-first mindset. This policy explains how the app handles your information.

## No data collection

Eonslate **does not collect or transmit** any personal data, tracked entries, financial records, usage statistics or telemetry. What you track is stored only on your device, as described below.

## Offline by design

Eonslate works entirely offline. It does not declare the `android.permission.INTERNET` permission, so it cannot send anything you track over the network.

## Where your data lives

Everything you create is stored in the app's private storage on your device, which other apps cannot read:

| What | Where |
| :--- | :--- |
| Trackers, fields, entries, reminders, accounts and transactions | The `eonslate.db` database |
| App settings, such as whether the welcome was seen | The `settings` preferences file |

Uninstalling the app or clearing its storage deletes both. Backup files are written to the location you choose.

Payment SMS import reads text you paste or share. Inbox-enabled builds can scan the selected period after you grant SMS access. Eonslate processes messages locally and stores the transactions you approve and fingerprints used to avoid importing them twice.

## Permissions

Eonslate asks for permissions only for features you turn on:

- **Notifications**, asked when you add a reminder.
- **Alarms and reminders** (exact alarms), so reminders arrive on time. You can turn it off in system settings; reminders then arrive within 10 minutes.
- **Run at startup**, to set reminders again after a restart.
- **Read SMS** in inbox-enabled builds, when you choose to scan payment messages.

## Source code

Eonslate's source code is public, so you can check how it handles your data yourself.
