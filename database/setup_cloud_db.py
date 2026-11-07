"""Подготовить облачную базу (Railway) одной командой: миграции → промпты и материалы → проверка.

    python3 database/setup_cloud_db.py

Скрипт сам спросит адрес базы (DATABASE_PUBLIC_URL из Railway → Postgres → Variables, кнопка копирования)
и не покажет его на экране. Можно и так: DATABASE_URL=… python3 database/setup_cloud_db.py
"""
from __future__ import annotations

import getpass
import os
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent


def ask_url() -> str:
    url = os.environ.get("DATABASE_URL", "").strip()
    while True:
        if not url:
            url = getpass.getpass("Вставь DATABASE_PUBLIC_URL (Cmd+V, символы не видны) и нажми Enter: ").strip().strip("'\"<>")
        problem = None
        if not url.startswith(("postgresql://", "postgres://")):
            problem = f"адрес должен начинаться с postgresql://, а начинается с «{url[:12]}»"
        elif "${{" in url:
            problem = "это шаблон Railway, а не адрес: включи Public Access у Postgres и скопируй DATABASE_PUBLIC_URL заново"
        elif "railway.internal" in url:
            problem = "это внутренний адрес (DATABASE_URL), с компьютера нужен DATABASE_PUBLIC_URL"
        if not problem:
            return url
        print("✗", problem)
        url = ""


def run(title: str, *args: str, env: dict) -> None:
    print(f"\n== {title}", flush=True)
    r = subprocess.run([sys.executable, *args], cwd=ROOT, env=env)
    if r.returncode:
        raise SystemExit(f"✗ {title}: ошибка (код {r.returncode}), вывод выше")


def main() -> int:
    url = ask_url()
    host = url.split("@")[-1].split("/")[0]
    print(f"База: {host}")
    env = {**os.environ, "DATABASE_URL": url}
    run("Миграции", "database/migrate.py", url, env=env)
    run("Промпты и учебные материалы", "ai-content/load/load.py", env=env)
    run("Проверка", "database/migrate.py", url, "--status", env=env)
    print("\n✓ База готова. Public Access у Postgres теперь можно удалить.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
