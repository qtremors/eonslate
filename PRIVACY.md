# Privacy Policy for Eonslate

**Last Updated:** 2026-11-16

Eonslate is a personal project built with a privacy-first mindset. This policy explains how the app handles your information.

## No data collection

Eonslate **does not collect or transmit** any personal data, tracked entries, financial records, usage statistics or telemetry. What you track is stored only on your device, as described below.

## Offline by design

Eonslate works entirely offline. It does not declare the `android.permission.INTERNET` permission, so it cannot send anything you track over the network.

## Where your data lives

Everything you create is stored in the app's private storage on your device, which other apps cannot read:

| What | Where |
| :--- | :--- |
| Trackers, fields, entries and their values | The `eonslate.db` database |
| App settings, such as whether the welcome was seen | The `settings` preferences file |

Uninstalling the app or clearing its storage deletes both. Backups and exports, when they arrive, are written only to a location you choose.

## Permissions

Eonslate asks for permissions only for features you turn on:

- **Notifications**, asked when you add a reminder.
- **Alarms and reminders** (exact alarms), so reminders arrive on time. You can turn it off in system settings; reminders then arrive within 10 minutes.
- **Run at startup**, to set reminders again after a restart.

## Source code

Eonslate's source code is public, so you can check how it handles your data yourself.
