#!/bin/bash
# Применяет миграции по порядку версий. Запускается образом postgres при первом старте.
# Учёт в schema_migrations — как у migrate.sh, чтобы потом новые миграции докатывать им.
set -e
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -q -c \
  "CREATE TABLE IF NOT EXISTS schema_migrations (version INT PRIMARY KEY, file TEXT NOT NULL, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())"
for f in $(ls /docker-entrypoint-initdb.d/migrations/*.sql | sort -V); do
  name=$(basename "$f"); v=${name#V}; v=${v%%__*}
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -1 -f "$f" \
    -c "INSERT INTO schema_migrations (version, file) VALUES ($v, '$name')"
done
