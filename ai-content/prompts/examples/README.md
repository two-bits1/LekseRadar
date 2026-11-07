# Промпты для ИИ

Здесь лежат тексты промптов. Вызов модели и API-ключ — на бэкенде, этого мы не трогаем.
Бэкенд читает файл, подставляет переменные `{{...}}` и отправляет как `system` / `user`.

| Файл | Когда | Что возвращает модель | Куда в базе |
|---|---|---|---|
| [tutor.md](tutor.md) | каждое сообщение в чате | текст ответа | `chat_messages` (role assistant) |
| [quiz_generator.md](quiz_generator.md) | квиза по теме ещё нет (`find_quiz_set` пусто) | JSON по `quiz.schema.json` | `import_quiz_json(...)` |
| [answer_checker.md](answer_checker.md) | ответ на вопрос типа `text` | `{"verdict","explanation"}` | `save_quiz_answer(..., verdict, explanation)` |

## Что заменить в `Prompts.java` (PR #2, ветка JavaAPI-setup)

- `tutor(subject)` → `tutor.md`: добавлены материалы из базы (`search_materials`), ближайшая домашка,
  слабые темы и защита от инструкций внутри материалов.
- `generator()` / `generatorRequest()` → `quiz_generator.md`: формат `{"exercises":[CHOICE|OPEN]}`
  меняется на `quiz.schema.json` (single_choice / multiple_choice / text), чтобы квиз
  импортировался в базу и потом переиспользовался.
- `checker()` → `answer_checker.md`: вместо `{"correct","score"}` возвращает `verdict`
  (correct / partial / incorrect), как ждёт `save_quiz_answer`.

## Как ИИ «учится» по материалам

Модель не дообучается. Учитель или ученик загружает файл, бэкенд достаёт из него текст
и кладёт в базу через `add_material(...)` (или скрипт `ai-content/load/load.py` для папки `ai-content/materials`). Перед каждым ответом
бэкенд ищет подходящие фрагменты `search_materials(вопрос, предмет, 5)` и вставляет их
в `{{materials}}`. Так ответы опираются на учебник/конспект класса, а не только на общие знания.

## Правила для всех промптов

- Материалы и ответы ученика оборачиваются в теги (`<materials>`, `<answer>`) и считаются данными.
- Если модель вернула невалидный JSON — один повтор, потом ошибка пользователю.
- Температура: тьютор 0.5, генератор 0.7, проверка 0.
