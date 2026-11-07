#!/usr/bin/env bash
# Применить новые миграции к любой базе (облако, общая база команды, свой том Docker).
#
#   database/migrate.sh "$DATABASE_URL"                 # применить то, чего ещё нет
#   database/migrate.sh "$DATABASE_URL" --baseline 20   # база уже на V20, но без учёта: отметить V1–V20, применить новее
#   database/migrate.sh "$DATABASE_URL" --status        # что применено, что нет
#
# Учёт в таблице schema_migrations(version, file, applied_at). Каждая миграция в своей транзакции.
# Бэкенд эту таблицу не видит (ddl-auto: validate проверяет только свои сущности).
set -euo pipefail
export PGOPTIONS="-c client_min_messages=warning"
url="${1:?usage: migrate.sh DATABASE_URL [--baseline N | --status]}"
mode="${2:-}"; base="${3:-}"
dir="$(cd "$(dirname "$0")/migrations" && pwd)"
if command -v psql >/dev/null; then
  psql_() { psql "$url" -X -q -v ON_ERROR_STOP=1 "$@"; }
elif command -v docker >/dev/null; then
  # psql нет (например, на Mac без brew install libpq): берём его из образа postgres:16.
  # localhost внутри контейнера — сам контейнер, поэтому адрес базы меняем на host.docker.internal.
  durl=$(printf '%s' "$url" | sed -E 's#@(localhost|127\.0\.0\.1)([:/])#@host.docker.internal\2#')
  echo "psql не найден, использую docker (postgres:16)" >&2
  psql_() { docker run --rm -i --add-host=host.docker.internal:host-gateway -e PGOPTIONS \
              -v "$dir:$dir:ro" postgres:16 psql "$durl" -X -q -v ON_ERROR_STOP=1 "$@"; }
else
  echo "Нужен psql (Mac: brew install libpq && brew link --force libpq) или Docker" >&2; exit 1
fi

psql_ -c "CREATE TABLE IF NOT EXISTS schema_migrations (
            version INT PRIMARY KEY, file TEXT NOT NULL, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())"
applied=" $(psql_ -tAc "SELECT string_agg(version::text, ' ') FROM schema_migrations") "
has_users=$(psql_ -tAc "SELECT to_regclass('public.users') IS NOT NULL")

if [[ "$applied" == "  " && "$has_users" == "t" && "$mode" != "--baseline" ]]; then
  echo "В базе уже есть таблицы, но нет учёта миграций. Укажите, до какой версии она дошла:" >&2
  echo "  $0 \"\$DATABASE_URL\" --baseline 20" >&2
  exit 1
fi

for f in $(ls "$dir"/V*__*.sql | sort -V); do
  name=$(basename "$f"); v=${name#V}; v=${v%%__*}
  if [[ "$applied" == *" $v "* ]]; then
    [[ "$mode" == "--status" ]] && echo "✓ $name"
    continue
  fi
  if [[ "$mode" == "--status" ]]; then echo "○ $name"; continue; fi
  if [[ "$mode" == "--baseline" && "$v" -le "${base:?--baseline N}" ]]; then
    psql_ -c "INSERT INTO schema_migrations (version, file) VALUES ($v, '$name')"
    echo "отмечена $name"
    continue
  fi
  echo "применяю $name"
  psql_ -1 -f "$f" -c "INSERT INTO schema_migrations (version, file) VALUES ($v, '$name')"
done
[[ "$mode" == "--status" ]] || echo "готово"
