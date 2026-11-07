"""CLI для проверки: python -m homework --creds creds.json [--json]"""
import argparse
import json
import pathlib

from . import collect

ICON = {"overdue": "!!", "todo": "  ", "submitted": "ok", "graded": "★ "}

p = argparse.ArgumentParser(description="Собрать ДЗ со всех платформ")
p.add_argument("--creds", help="JSON с учётными данными (формат Credentials)")
p.add_argument("--json", action="store_true", help="вывести JSON")
a = p.parse_args()
creds = json.loads(pathlib.Path(a.creds).read_text()) if a.creds else {}
res = collect(creds)
if a.json:
    print(json.dumps(res.model_dump(mode="json"), ensure_ascii=False, indent=1))
else:
    for name, s in res.sources.items():
        print(f"# {name}: {s.count} шт." if s.ok else f"# {name}: ОШИБКА {s.error}")
    for x in res.assignments:
        due = x.due.astimezone().strftime("%d.%m %H:%M") if x.due else "без срока"
        print(f"{ICON[x.status]} {due:>11}  [{x.source:<11}] {x.course}: {x.title}")
