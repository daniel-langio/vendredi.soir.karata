#!/bin/sh
# Builds web-ui/ (the Flutter web client) and copies the release output into
# src/main/resources/static/, so Spring Boot serves it as static resources from
# the same origin as the API - no CORS needed. Requires `flutter` on PATH.
# Run before packaging/deploying; the copied output is never committed (see
# .gitignore) and must be regenerated on every build.
set -e

cd "$(dirname "$0")/.."

echo "Building web-ui..."
cd web-ui
flutter pub get
# --pwa-strategy=none: this is a live-polling multiplayer app, not something that benefits from
# offline support, and Flutter's default service worker caches the app shell aggressively enough
# that a fresh deploy needs a second manual reload before the browser stops serving the old one -
# disabling it means every load fetches the current build.
flutter build web --release --pwa-strategy=none
cd ..

# Belt-and-suspenders: on this project's pinned Flutter version, --pwa-strategy=none does not
# actually stop flutter_bootstrap.js from registering a service worker (its behavior around this
# flag has been in flux - see the deprecation notice Flutter itself prints for it). Strip the
# registration from the generated file directly instead of trusting the flag - a plain grep for
# "serviceWorkerSettings" isn't a safe way to detect this, since that string is also present
# generically inside Flutter's bundled loader library code even when nothing is registered; the
# quoted numeric serviceWorkerVersion value only ever appears in an actual live registration.
BOOTSTRAP=web-ui/build/web/flutter_bootstrap.js
perl -0777 -pi -e 's/_flutter\.loader\.load\(\{\s*serviceWorkerSettings:\s*\{[^}]*\}\s*\}\);/_flutter.loader.load({});/s' "$BOOTSTRAP"
if grep -qE 'serviceWorkerVersion:\s*"[0-9]+"' "$BOOTSTRAP"; then
  echo "ERROR: $BOOTSTRAP still registers a service worker after stripping - Flutter's output shape probably changed, update the pattern in this script." >&2
  exit 1
fi
echo "Confirmed: flutter_bootstrap.js does not register a service worker."

echo "Copying build output into src/main/resources/static..."
rm -rf src/main/resources/static
mkdir -p src/main/resources/static
cp -r web-ui/build/web/. src/main/resources/static/

echo "Done: web-ui is now served from /"
