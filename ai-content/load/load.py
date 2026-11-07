"""Загрузка промптов и учебных файлов в базу LekseRadar.

    python load/load.py                      # всё из ai-content/prompts и ai-content/materials
    python load/load.py --dry-run            # только показать, что будет загружено
    python load/load.py --owner-id 5         # материалы видит только пользователь 5 (по умолчанию общие)

Подключение: переменная DATABASE_URL, например
    postgresql://lekseradar:lekseradar@127.0.0.1:5432/lekseradar

Промпты:   prompts/<key>.md|.txt          → save_prompt('<key>', текст)   (README.md пропускается)
Материалы: materials/<Предмет>/<Тема>/файл → add_material(owner, Предмет, Тема, название, текст)
           materials/<Предмет>/файл        → без темы
Поддерживаются .txt .md .pdf .docx .html. Повторный запуск ничего не дублирует,
изменённый файл заменяет свою прошлую версию.
"""
from __future__ import annotations

import argparse
import html
import mimetypes
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEXT_EXT = {".txt", ".md"}
SUPPORTED = TEXT_EXT | {".pdf", ".docx", ".html", ".htm"}


def extract_text(path: Path) -> str:
    ext = path.suffix.lower()
    if ext in TEXT_EXT:
        return path.read_text(encoding="utf-8", errors="replace")
    if ext == ".pdf":
        from pypdf import PdfReader
        pages = [p.extract_text() or "" for p in PdfReader(str(path)).pages]
        return "\n\n".join(p.strip() for p in pages if p.strip())
    if ext == ".docx":
        import docx
        d = docx.Document(str(path))
        parts = [p.text for p in d.paragraphs]
        for table in d.tables:
            for row in table.rows:
                parts.append(" | ".join(c.text.strip() for c in row.cells))
        return "\n\n".join(p.strip() for p in parts if p.strip())
    if ext in {".html", ".htm"}:
        raw = path.read_text(encoding="utf-8", errors="replace")
        raw = re.sub(r"(?is)<(script|style).*?</\1>", " ", raw)
        raw = re.sub(r"(?i)<(br|/p|/div|/h\d|/li)[^>]*>", "\n\n", raw)
        return html.unescape(re.sub(r"<[^>]+>", " ", raw))
    raise ValueError(f"не поддерживается: {ext}")


def clean(text: str) -> str:
    text = text.replace("\x00", "")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def find_prompts(folder: Path):
    for f in sorted(folder.glob("*")):
        if f.suffix.lower() in TEXT_EXT and f.stem.lower() != "readme":
            key = re.sub(r"[^a-z0-9_]", "_", f.stem.lower())
            yield key, f


def find_materials(folder: Path):
    for f in sorted(folder.rglob("*")):
        if not f.is_file() or f.name.startswith(".") or f.name.lower().startswith("readme"):
            continue
        rel = f.relative_to(folder)
        if len(rel.parts) < 2:
            print(f"  пропуск {rel}: положи файл в папку предмета", file=sys.stderr)
            continue
        if f.suffix.lower() not in SUPPORTED:
            print(f"  пропуск {rel}: формат {f.suffix} не поддерживается", file=sys.stderr)
            continue
        subject = rel.parts[0]
        topic = rel.parts[1] if len(rel.parts) >= 3 else None
        title = f.stem.replace("_", " ")
        yield subject, topic, title, f, rel


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--prompts", type=Path, default=ROOT / "prompts")
    ap.add_argument("--materials", type=Path, default=ROOT / "materials")
    ap.add_argument("--owner-id", type=int, default=None, help="id пользователя; без него материалы общие")
    ap.add_argument("--language", default="nb")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    prompts = list(find_prompts(args.prompts)) if args.prompts.is_dir() else []
    materials = list(find_materials(args.materials)) if args.materials.is_dir() else []

    if args.dry_run:
        for key, f in prompts:
            print(f"промпт   {key:<20} {f.name}")
        for subject, topic, title, f, rel in materials:
            text = clean(extract_text(f))
            print(f"материал {subject} / {topic or '-'} / {title}: {len(text)} символов")
        return 0

    import psycopg
    dsn = os.environ.get("DATABASE_URL")
    if not dsn:
        print("укажи DATABASE_URL", file=sys.stderr)
        return 2

    with psycopg.connect(dsn) as conn:
        for key, f in prompts:
            ver = conn.execute("SELECT save_prompt(%s, %s, %s)",
                               (key, f.read_text(encoding="utf-8"), f"файл {f.name}")).fetchone()[0]
            print(f"промпт   {key}: версия {ver}")
        for subject, topic, title, f, rel in materials:
            text = clean(extract_text(f))
            if not text:
                print(f"  пропуск {rel}: текст не извлёкся (скан? нужен OCR)", file=sys.stderr)
                continue
            # Файл изменился: старая версия с тем же путём удаляется, иначе тьютор цитирует устаревший текст
            gone = conn.execute(
                "DELETE FROM materials WHERE owner_id IS NOT DISTINCT FROM %s AND storage_key = %s"
                " AND content_hash <> md5(%s)",
                (args.owner_id, str(rel), text),
            ).rowcount
            if gone:
                print(f"  {rel}: файл изменился, старая версия удалена")
            mid = conn.execute(
                "SELECT add_material(%s, %s, %s, %s, %s, p_file_name => %s, p_mime_type => %s,"
                " p_source => 'teacher', p_language => %s, p_storage_key => %s)",
                (args.owner_id, subject, topic, title, text, f.name,
                 mimetypes.guess_type(f.name)[0], args.language, str(rel)),
            ).fetchone()[0]
            chunks = conn.execute("SELECT chunk_count FROM materials WHERE id = %s", (mid,)).fetchone()[0]
            print(f"материал {subject} / {topic or '-'} / {title}: id {mid}, фрагментов {chunks}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
