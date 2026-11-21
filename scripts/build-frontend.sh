#!/usr/bin/env bash
# Build the Angular frontend and copy it into the Spring Boot static
# resources, so the packaged jar serves the UI + API together on :8080.
# Usage (from the repo root):  bash scripts/build-frontend.sh
set -euo pipefail

cd "$(dirname "$0")/.."

cd frontend/salary-app
npm ci --no-audit --no-fund || npm install --no-audit --no-fund
npx ng build
cd ../..

STATIC_DIR="backend/src/main/resources/static"
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR"
cp -r frontend/salary-app/dist/salary-app/browser/. "$STATIC_DIR"/

echo "Frontend copied to $STATIC_DIR:"
ls "$STATIC_DIR"
