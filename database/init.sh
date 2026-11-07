#!/bin/bash
# Применяет миграции по порядку версий. Запускается образом postgres при первом старте.
set -e
for f in $(ls /docker-entrypoint-initdb.d/migrations/*.sql | sort -V); do
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f "$f"
done
