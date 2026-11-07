"""Получить доступ к Teams или itslearning и дописать его в creds.json.

    python -m homework.login teams --creds creds.json [--client-id <Entra app id>]
    python -m homework.login itslearning --creds creds.json --base-url https://<школа>.itslearning.com

Canvas (личный токен) и InSchool (cookie из браузера) вписываются в creds.json руками, см. docs/REAL-DATA.md.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import sys
import time

import httpx

from .models import SourceError, TeamsCreds
from .sources import itslearning, teams


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description="Войти в платформу и сохранить токены в creds.json")
    p.add_argument("platform", choices=["teams", "itslearning"])
    p.add_argument("--creds", required=True, help="файл creds.json (создастся, если его нет)")
    p.add_argument("--client-id", help="Teams: id приложения Entra, иначе TEAMS_CLIENT_ID")
    p.add_argument("--base-url", help="itslearning: адрес школы, например https://sms.itslearning.com")
    a = p.parse_args(argv)
    path = pathlib.Path(a.creds)
    creds = json.loads(path.read_text()) if path.exists() else {}

    with httpx.Client(timeout=30) as client:
        if a.platform == "teams":
            cid = teams.client_id(TeamsCreds(client_id=a.client_id))
            dev = teams.start_device_login(client, cid)
            print(dev.get("message") or f"Откройте {dev['verification_uri']} и введите код {dev['user_code']}")
            deadline = time.time() + int(dev.get("expires_in", 900))
            tok = None
            while tok is None and time.time() < deadline:
                time.sleep(int(dev.get("interval", 5)))
                tok = teams.poll_device_login(client, cid, dev["device_code"])
            if tok is None:
                print("Код истёк, запустите ещё раз.", file=sys.stderr)
                return 1
            creds["teams"] = tok
        else:
            if not a.base_url:
                p.error("для itslearning нужен --base-url")
            print("1. Откройте ссылку и войдите (Feide):\n  ", itslearning.authorize_url(a.base_url))
            print("2. В конце браузер не откроет адрес itsl-itslearning://login?code=...; скопируйте его\n"
                  "   из DevTools → Network (или из строки ошибки) и вставьте сюда.")
            creds["itslearning"] = itslearning.exchange_code(client, a.base_url, input("Адрес или code: ").strip())

    path.write_text(json.dumps(creds, ensure_ascii=False, indent=1))
    print(f"Сохранено в {path}: {a.platform}.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except SourceError as e:
        sys.exit(f"Ошибка: {e}")
