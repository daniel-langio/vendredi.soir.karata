# web-ui

The Flutter **web** client for the poker API, served by this same Spring Boot app as a static
single-page app (so browser calls to the API are same-origin - no CORS configuration needed).

This is a copy of the UI code from `vendredi.soir.karata-mobile` (the "Karata" app), trimmed to the
web-relevant parts (`lib/`, `web/`, `test/`, no `android/`). The two currently diverge on purpose:
this package defaults its server URL to wherever the page itself was loaded from (see
`lib/screens/welcome_screen.dart`), since it's always served same-origin with the API here, while
the mobile app defaults to the deployed Lambda URL, since a native app has no "origin" to inherit.

If you change UI code that should apply to both the web and native (Android) builds, port the
change to `vendredi.soir.karata-mobile` too - there's no automated sync between the two right now.

## Building

Requires the Flutter SDK on `PATH`. From the repo root:

```
./.shell/build_web_ui.sh
```

This runs `flutter build web --release` here and copies the output into
`src/main/resources/static/`, which Spring Boot serves automatically. The copied output is
git-ignored and must be regenerated on every build/deploy - it isn't committed.
