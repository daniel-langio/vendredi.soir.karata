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
flutter build web --release
cd ..

echo "Copying build output into src/main/resources/static..."
rm -rf src/main/resources/static
mkdir -p src/main/resources/static
cp -r web-ui/build/web/. src/main/resources/static/

echo "Done: web-ui is now served from /"
