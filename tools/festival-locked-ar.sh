#!/usr/bin/env bash
set -euo pipefail
exec flock "$FESTIVAL_ARCHIVE_LOCK" "$FESTIVAL_REAL_AR" "$@"
