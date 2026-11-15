# Privacy Policy for Eonslate

**Last Updated:** 2026-11-15

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

Eonslate currently asks for no permissions. It will ask for one only when you turn on a feature that needs it, and explain why first. Each optional permission will be listed here when its feature ships.

## Source code

Eonslate's source code is public, so you can check how it handles your data yourself.
