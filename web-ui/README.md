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

**`main.dart` and its navigation are a second, larger intentional divergence.** This package uses
real per-page URL paths (`/menu`, `/table/<gameId>`, etc. - see `_onGenerateRoute` in `main.dart`),
`usePathUrlStrategy()` to drop the `#`, and a small loader (`_TableRouteLoader`) that reloads the
session from `shared_preferences` when `/table/<gameId>` is opened directly (a link someone was
sent, or a browser refresh) rather than via in-app navigation. None of this makes sense for the
native app - there's no browser URL bar to reflect paths in - so it isn't ported to
`vendredi.soir.karata-mobile`, unlike most UI changes. The server side of this (forwarding an
unmatched, non-API path to `index.html` so a refresh on `/table/<id>` doesn't 404 before the SPA's
own router even runs) lives in `SpaFallbackController` back in the main API app, not here.

`TableScreen`'s "copy invite" button copies a real link on web (`<origin>/table/<gameId>`) but
falls back to a bare table ID on the native build (`kIsWeb`-gated in the shared `table_screen.dart`
- no `origin` concept to build a link from there) - this one stays byte-identical between the two
repos despite the divergence above, since the fallback is self-contained in that one file.

## Building

Requires the Flutter SDK on `PATH`. From the repo root:

```
./.shell/build_web_ui.sh
```

This runs `flutter build web --release` here and copies the output into
`src/main/resources/static/`, which Spring Boot serves automatically. The copied output is
git-ignored and must be regenerated on every build/deploy - it isn't committed.
