# Privacy Policy for Eonslate

**Last Updated:** 2026-11-24

Eonslate is a personal project built with a privacy-first mindset. This policy explains how the app handles your information.

## No data collection

Eonslate **does not collect or transmit** any personal data, tracked entries, financial records, usage statistics or telemetry. What you track is stored only on your device, as described below.

## Offline by design

Eonslate works entirely offline. It does not declare the `android.permission.INTERNET` permission, so it cannot send anything you track over the network.

## Where your data lives

Everything you create is stored in the app's private storage on your device, which other apps cannot read:

| What | Where |
| :--- | :--- |
| Trackers, fields, entries, reminders, accounts, transactions and the people and businesses you name | The `eonslate.db` database |
| Photos and PDFs attached to transactions, and recurring payment icons | The app's private `attachments` and `icons` folders |
| App settings, such as whether the welcome was seen | The `settings` preferences file |

Uninstalling the app or clearing its storage deletes all of it. Backups, which include attached files, and CSV exports are written to the location you choose.

Payment SMS import reads text you paste or share. Inbox-enabled builds can scan the selected period after you grant SMS access. Eonslate processes messages locally and stores the transactions you approve and fingerprints used to avoid importing them twice.

Taking a photo for an attachment opens your camera app, which hands the photo back to Eonslate; Eonslate itself does not ask for camera access. Links you attach are stored as text and open in your browser only when you tap them.

Turn on Block screenshots in Settings to protect app windows in screenshots and screen sharing.

## Permissions

Eonslate asks for permissions only for features you turn on:

- **Notifications**, asked when you add a reminder.
- **Alarms and reminders** (exact alarms), so reminders arrive on time. You can turn it off in system settings; reminders then arrive within 10 minutes.
- **Run at startup**, to set reminders again after a restart.
- **Read SMS** in inbox-enabled builds, when you choose to scan payment messages.

## Source code

Eonslate's source code is public, so you can check how it handles your data yourself.
