"""Проверка ИИ на работающем бэкенде с настоящим ключом (шаг 4 PLAN-PYTHON.md).

    python ai-content/check/check_ai.py --email test@lekseradar.no --password LekseRadar2026 \\
        [--api http://localhost:8080/api/v1] [--langs ru,nb] [--database-url postgresql://…]

Для каждого языка: язык профиля → чат-репетитор по первому несданному ДЗ → новый квиз от ИИ →
ответ на вопрос (текстовый проверяет answer_checker). В конце таблица для README-TEST.md.
Только стандартная библиотека; psycopg нужен лишь для --database-url (проверка quiz_sets.language, V20).
Ключ ИИ скрипт не видит: он в backend/.env. Каждый язык тратит ~4 запроса к ИИ (лимит 30 за 10 минут).
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import time
import urllib.error
import urllib.request

QUESTION = {"ru": "С чего мне начать это задание? Объясни коротко.",
            "nb": "Hvordan bør jeg begynne på denne oppgaven? Forklar kort.",
            "en": "How should I start this assignment? Explain briefly."}
TOPIC = {"ru": "Механические волны", "nb": "Mekaniske bølger", "en": "Mechanical waves"}
TEXT_ANSWER = {"ru": "Скорость волны равна длине волны, умноженной на частоту.",
               "nb": "Bølgefarten er bølgelengden ganger frekvensen.",
               "en": "Wave speed equals wavelength times frequency."}


def guess_lang(text: str) -> str:
    """Грубо: кириллица → ru; æøå или частые норвежские слова → nb; иначе en."""
    letters = [c for c in text if c.isalpha()]
    if not letters:
        return "?"
    if sum("а" <= c.lower() <= "я" or c in "ёЁ" for c in letters) / len(letters) > 0.3:
        return "ru"
    words = set(re.findall(r"[a-zæøå]+", text.lower()))
    if re.search(r"[æøå]", text.lower()) or words & {"og", "ikke", "er", "det", "som", "på", "med", "du", "en", "til"}:
        return "nb"
    return "en"


class Api:
    def __init__(self, base: str):
        self.base, self.token = base.rstrip("/"), None

    def __call__(self, method: str, path: str, body=None):
        req = urllib.request.Request(self.base + path, method=method,
                                     data=None if body is None else json.dumps(body).encode(),
                                     headers={"Content-Type": "application/json",
                                              **({"Authorization": f"Bearer {self.token}"} if self.token else {})})
        t = time.monotonic()
        try:
            with urllib.request.urlopen(req, timeout=180) as r:
                raw = r.read()
                return (json.loads(raw) if raw else None), time.monotonic() - t
        except urllib.error.HTTPError as e:
            detail = e.read().decode(errors="replace")[:300]
            raise SystemExit(f"{method} {path} → {e.code}: {detail}")


def short(s: str | None, n=160) -> str:
    s = " ".join((s or "").split())
    return s if len(s) <= n else s[:n] + "…"


def check(api: Api, lang: str, db) -> list[tuple[str, str, str]]:
    rows = []
    api("PATCH", "/users/me", {"locale": lang})

    hw = [a for a in api("GET", "/homework")[0] if a["status"] in ("todo", "overdue")]
    if hw:
        a = hw[0]
        chat, _ = api("POST", "/chats", {"mode": "tutor", "assignmentId": a["id"], "subject": a["course"] or None,
                                         "topic": a["title"][:100]})
        reply, sec = api("POST", f"/chats/{chat['id']}/messages", {"content": QUESTION[lang]})
        text = reply["assistantMessage"]["content"]
        rows.append(("чат по ДЗ", "✅" if guess_lang(text) == lang else f"⚠️ язык {guess_lang(text)}",
                     f"«{short(a['title'], 50)}», {sec:.0f} с: {short(text)}"))
    else:
        rows.append(("чат по ДЗ", "—", "нет несданных ДЗ"))

    topic = f"{TOPIC[lang]} {int(time.time()) % 10000}"   # новая тема: квиз точно от ИИ, а не из готовых
    quiz, sec = api("POST", "/quizzes", {"subject": "Fysikk", "topic": topic, "count": 3, "fresh": True})
    qs = quiz["questions"]
    qtext = " ".join(q["question"] for q in qs)
    note = f"{len(qs)} вопр., {sec:.0f} с: {short(qs[0]['question'] if qs else '', 100)}"
    ok = len(qs) > 0 and guess_lang(qtext) == lang
    if db is not None:
        saved = db.execute("SELECT language FROM quiz_sets WHERE id = %s", (quiz["quizSetId"],)).fetchone()
        note += f"; quiz_sets.language = {saved[0] if saved else '?'}"
        ok = ok and saved is not None and saved[0] == lang
    rows.append(("квиз от ИИ", "✅" if ok else "⚠️", note))

    q = next((q for q in qs if q["type"] == "text"), qs[0] if qs else None)
    if q:
        answer = TEXT_ANSWER[lang] if q["type"] == "text" else (
            json.dumps([q["options"][0]]) if q["type"] == "multiple_choice" else q["options"][0])
        res, sec = api("POST", f"/quizzes/{quiz['id']}/answers", {"position": q["position"], "answer": answer})
        rows.append((f"ответ ({q['type']})", "✅" if res.get("verdict") else "⚠️",
                     f"{res.get('verdict')}, {sec:.0f} с: {short(res.get('explanation'), 120)}"))
    return rows


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description="Проверить чат, квиз и проверку ответов с настоящим ИИ")
    p.add_argument("--api", default="http://localhost:8080/api/v1")
    p.add_argument("--email", required=True)
    p.add_argument("--password", required=True)
    p.add_argument("--langs", default="ru,nb")
    p.add_argument("--database-url", help="для проверки quiz_sets.language (V20)")
    a = p.parse_args(argv)

    api = Api(a.api)
    auth, _ = api("POST", "/auth/login", {"email": a.email, "password": a.password})
    api.token = auth["token"]
    original = auth["user"]["locale"]
    db = None
    if a.database_url:
        import psycopg
        db = psycopg.connect(a.database_url, autocommit=True)

    out = ["| Язык | Проверка | Итог | Детали |", "|---|---|---|---|"]
    try:
        for lang in a.langs.split(","):
            for name, status, note in check(api, lang.strip(), db):
                out.append(f"| {lang} | {name} | {status} | {note.replace('|', '/')} |")
                print(out[-1], flush=True)
    finally:
        api("PATCH", "/users/me", {"locale": original})
    print("\n" + "\n".join(out))
    return 0 if all("⚠️" not in r for r in out) else 1


if __name__ == "__main__":
    sys.exit(main())
