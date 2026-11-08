"""Записать настоящие ДЗ ученика в базу, пока нет POST /homework/sync в бэкенде.

    DATABASE_URL=postgresql://lekseradar:<DB_PASSWORD>@127.0.0.1:5432/lekseradar \\
    python -m homework.to_db --email ученик@школа.no --creds creds.json

collect() по creds.json → sync_assignments() (V21): UPSERT в assignments по (user_id, source, external_id).
Обновлённые токены (Teams, itslearning, cookie InSchool) записываются обратно в creds.json.
Ученик должен уже быть зарегистрирован на сайте. Вместо --creds можно передать --from-json result.json
(вывод `python -m homework --creds creds.json --json`), тогда платформы не опрашиваются.
"""
from __future__ import annotations

import argparse
import json
import os
import pathlib
import sys

from .models import CollectResult

def save(conn, user_id: int, result: CollectResult) -> tuple[int, int]:
    """Все задания одним вызовом sync_assignments() (миграция V21); возвращает (новых, обновлённых)."""
    items = json.dumps([a.model_dump(mode="json") for a in result.assignments], ensure_ascii=False)
    row = conn.execute("SELECT added, updated FROM sync_assignments(%s, %s::jsonb)", (user_id, items)).fetchone()
    return row[0], row[1]


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description="Собрать ДЗ ученика и записать в базу LekseRadar")
    p.add_argument("--email", required=True, help="email ученика, зарегистрированного на сайте")
    src = p.add_mutually_exclusive_group(required=True)
    src.add_argument("--creds", help="JSON с доступами к платформам (формат в README)")
    src.add_argument("--from-json", help="готовый вывод `python -m homework --json`")
    p.add_argument("--database-url", default=os.environ.get("DATABASE_URL"))
    a = p.parse_args(argv)
    if not a.database_url:
        p.error("нужен --database-url или DATABASE_URL")

    if a.from_json:
        result = CollectResult.model_validate_json(pathlib.Path(a.from_json).read_text())
    else:
        from . import collect
        path = pathlib.Path(a.creds)
        creds = json.loads(path.read_text())
        result = collect(creds)
        if result.updated_credentials:
            creds.update(result.updated_credentials)
            path.write_text(json.dumps(creds, ensure_ascii=False, indent=1))

    import psycopg
    with psycopg.connect(a.database_url) as conn:
        row = conn.execute("SELECT id FROM users WHERE lower(email) = lower(%s)", (a.email,)).fetchone()
        if not row:
            print(f"Нет ученика {a.email}: сначала зарегистрируйтесь на сайте.", file=sys.stderr)
            return 1
        new, upd = save(conn, row[0], result)

    for name, s in result.sources.items():
        print(f"{name}: {s.count} шт." if s.ok else
              f"{name}: ОШИБКА {s.error}" + (" (нужно войти заново)" if s.needs_reconnect else ""))
    print(f"В базе: {new} новых, {upd} обновлено.")
    return 0 if all(s.ok for s in result.sources.values()) else 2


if __name__ == "__main__":
    sys.exit(main())
