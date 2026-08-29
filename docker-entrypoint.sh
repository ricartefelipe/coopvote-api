#!/bin/sh

set -eu

if [ -n "${DATABASE_URL:-}" ]; then
    export DB_URL="jdbc:postgresql://${DATABASE_URL##*@}"
fi

if [ -n "${RENDER_EXTERNAL_HOSTNAME:-}" ]; then
    export CALLBACK_BASE_URL="https://${RENDER_EXTERNAL_HOSTNAME}"
fi

exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
