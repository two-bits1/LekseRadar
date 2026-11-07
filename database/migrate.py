"""То же, что migrate.sh, но без psql и Docker: нужен только Python и psycopg.

    pip install 'psycopg[binary]'
    python database/migrate.py "$DATABASE_URL"                 # применить то, чего ещё нет
    python database/migrate.py "$DATABASE_URL" --baseline 20   # база уже на V20, но без учёта
    python database/migrate.py "$DATABASE_URL" --status

Учёт в schema_migrations (общий с migrate.sh и init.sh). Каждая миграция в своей транзакции.
"""
from __future__ import annotations

import argparse
import pathlib
import re
import sys

DIR = pathlib.Path(__file__).resolve().parent / "migrations"


def migrations():
    found = []
    for f in DIR.glob("V*__*.sql"):
        m = re.match(r"V(\d+)__", f.name)
        if m:
            found.append((int(m.group(1)), f))
    return sorted(found)


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description="Применить новые миграции LekseRadar")
    p.add_argument("url", help="postgresql://user:pass@host:port/db")
    g = p.add_mutually_exclusive_group()
    g.add_argument("--baseline", type=int, metavar="N", help="отметить V1–VN применёнными, не выполняя")
    g.add_argument("--status", action="store_true")
    a = p.parse_args(argv)

    import psycopg
    with psycopg.connect(a.url, autocommit=True, options="-c client_min_messages=warning") as conn:
        conn.execute("CREATE TABLE IF NOT EXISTS schema_migrations ("
                     "version INT PRIMARY KEY, file TEXT NOT NULL, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())")
        applied = {r[0] for r in conn.execute("SELECT version FROM schema_migrations")}
        has_users = conn.execute("SELECT to_regclass('public.users') IS NOT NULL").fetchone()[0]
        if not applied and has_users and a.baseline is None and not a.status:
            print("В базе уже есть таблицы, но нет учёта миграций. Укажите, до какой версии она дошла:\n"
                  f'  python {sys.argv[0]} "$DATABASE_URL" --baseline 20', file=sys.stderr)
            return 1

        for v, f in migrations():
            if v in applied:
                if a.status:
                    print(f"✓ {f.name}")
                continue
            if a.status:
                print(f"○ {f.name}")
                continue
            if a.baseline is not None and v <= a.baseline:
                conn.execute("INSERT INTO schema_migrations (version, file) VALUES (%s, %s)", (v, f.name))
                print(f"отмечена {f.name}")
                continue
            print(f"применяю {f.name}", flush=True)
            with conn.transaction():
                conn.execute(f.read_text(encoding="utf-8"))
                conn.execute("INSERT INTO schema_migrations (version, file) VALUES (%s, %s)", (v, f.name))
    if not a.status:
        print("готово")
    return 0


if __name__ == "__main__":
    sys.exit(main())
