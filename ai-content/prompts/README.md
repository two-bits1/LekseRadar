# Промпты

Грузятся в базу скриптом `python ../load/load.py`. Имя файла без `.md` = ключ в `ai_prompts`.
Изменил файл и запустил загрузку снова — в базе новая версия (откат: `SELECT activate_prompt('tutor', 1)`).
Правила в промптах основаны на [../research/metody-obucheniya.md](../research/metody-obucheniya.md).

| Файл | Чат | Что возвращает ИИ |
|---|---|---|
| `tutor.md` | режим `tutor`: помогает понять тему ДЗ, не делает ДЗ за ученика | текст на языке сайта |
| `quiz_generator.md` | режим `quiz`: квиз по ДЗ | JSON по `database/quiz/quiz.schema.json` |
| `answer_checker.md` | ответ на вопрос `text` в квизе | `{"verdict","explanation"}` |

## Как бэкенд собирает запрос к ИИ

```sql
SELECT content, prompt_id FROM chat_system_prompt(:chat_id);  -- промпт режима чата
SELECT chat_prompt_vars(:chat_id);                            -- JSON: language, language_code, subject, topic, level,
                                                              -- homework_title, homework_due, homework_text, weak_topics
SELECT materials_for_prompt(:user_id, :вопрос_или_тема, :subject_id, 5);  -- {{materials}}
```
Бэкенд заменяет каждое `{{ключ}}` значением. Для квиза ещё `{{count}}` (по умолчанию 8).
Для `answer_checker`: `{{question}}`, `{{correct_answer}}`, `{{explanation}}` из `exercises`, `{{answer}}`, `{{language}}`.

## Чат-репетитор: язык и первый ответ

1. Новый чат: `start_chat(user, 'tutor', предмет, тема, p_assignment => id ДЗ)`.
2. Язык не спрашивается. `{{language}}` — язык, выбранный на сайте (`users.locale`), в виде названия:
   `norsk bokmål (nb)`, `русский (ru)`… (функция `language_name()`, миграция V17).
   Если у чата задан свой язык (`set_chat_language`), берётся он.
   Код языка (`ru`) — в `{{language_code}}` (V20): его квиз пишет в поле `"language"` JSON.
3. Задание, срок, текст ДЗ и слабые темы ИИ получает из базы через `chat_prompt_vars`, поэтому в первом
   ответе он не спрашивает, что задано: коротко называет суть задания и спрашивает, где ученик застрял.

## Файл ДЗ

Текст задания из PDF/DOCX (или описание с платформы) сохраняется так:
`SELECT attach_homework_file(:assignment_id, :text, 'oppgave.pdf', 'application/pdf');`
Потом он попадает в `{{homework_text}}` у любого чата, созданного с этим заданием.

## Чат квизов

`start_chat(user, 'quiz', предмет, тема, p_assignment => id ДЗ)` → ИИ возвращает JSON →
проверить по схеме → `import_quiz_json(json, user)` → `UPDATE chat_sessions SET quiz_set_id = ...` →
`start_quiz(user, quiz_set_id)`. Если JSON не прошёл проверку — один повтор запроса.

Образцы с пояснениями лежат в `examples/` (в базу не грузятся).
