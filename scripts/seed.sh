#!/usr/bin/env bash
# Seed the ACME salary-management database with 10,000 employees.
# Usage (from the repo root):  bash scripts/seed.sh [--db PATH] [--reset]
set -euo pipefail

cd "$(dirname "$0")/.."
python3 scripts/seed.py "$@"
