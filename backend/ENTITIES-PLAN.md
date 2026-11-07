# Сущности и репозитории бэкенда: как сделать и почему так

Документ для команды бэкенда. Здесь объяснено, как довести бэкенд до состояния, где:

- вход и регистрация идут только через Java (Spring Security + JWT);
- у каждой таблицы из `database/` есть JPA-сущность и репозиторий;
- пробных данных нет: база стартует пустой, всё приходит через регистрацию, вход и вызовы API.

Всё ниже уже один раз написали и проверили на пустой базе (V1–V16). Потом по просьбе Ильи код откатили.
Он лежит в истории ветки `JavaAPI-setup` (коммиты `f6f7e46` … `d4de4c9`, откат `2b943e1`).
Вернуть его целиком: `git revert 2b943e1`.

Схема базы по таблицам: `database/ENTITIES.md` в ветке `PythonAPI`.

---

## 1. Вход и регистрация (Spring Security)

В текущем бэкенде это уже сделано правильно, менять нужно только документацию.

| Шаг | Где | Что происходит |
|---|---|---|
| Регистрация | `POST /api/v1/auth/register` → `AuthService.register` | email без учёта регистра (`existsByEmailIgnoreCase`, занят → 409), пароль → `BCryptPasswordEncoder`, строка в `users`, событие в `user_events`, ответ `AuthResponse` с JWT |
| Вход | `POST /api/v1/auth/login` → `AuthService.login` | проверка bcrypt (неверно → 401), `last_login_at = now()`, JWT на `JWT_TTL_HOURS` (72 ч) |
| Остальные запросы | `JwtAuthenticationFilter` | `Authorization: Bearer <token>` → `UserRepository.findById` → пользователь в `SecurityContext`; без токена 401 JSON |
| Настройка | `SecurityConfig` | без сессий (`STATELESS`), CSRF выключен (JWT), открыты только `/api/v1/auth/**`, health и Swagger |
| Чужие данные | `findByIdAndUserId` в репозиториях | чужой чат, квиз, ДЗ или напоминание → 404 |

Проверено через HTTP на пустой базе:

| Запрос | Ответ |
|---|---|
| `GET /users/me` без токена | 401 |
| вход `demo@lekseradar.no` / `demo1234` | 401 (такого пользователя больше нет) |
| регистрация | 201 |
| тот же email другими буквами | 409 |
| неверный пароль | 401 |
| `GET /users/me` с токеном | 200 |
| `POST /chats` | 201, чат в базе с `mode = tutor` |

## 2. Без пробных данных

| Где | Что было | Что сделать |
|---|---|---|
| `database/seed.sql` | демо-ученик, ДЗ, чат, квиз | удалён в PythonAPI (`9dfebec`) |
| `homework-collector` | `collect(demo=True)` выдумывал задания | удалено там же |
| `assignments.is_demo` | флаг демо-задания | миграция `V16__no_demo_data.sql`: `ALTER TABLE assignments DROP COLUMN IF EXISTS is_demo;` |
| `backend/README.md` | «создаётся демо-ученик demo@lekseradar.no» | написать, что база пустая и ученик появляется через регистрацию; в «Как проверить» начинать со `POST /auth/register` |
| `docs/API.md` | «Демо-вход: demo@… / demo1234» и пример входа | убрать строку; в примере `{ "email": "ola@example.com", "password": "hemmelig123" }` |
| `frontend/src/api/api.test.ts` | email demo@ в тесте контракта | это только строка в тесте, на данные не влияет |

Пробными данными **не** считаются справочники, которые заполняют миграции: список ачивок (`achievements`, V6)
и режимы чата (`chat_mode_prompts`, V13). Без них ничего не работает.

В enum `material_source` есть значение `seed`. Его никто не пишет, оно просто осталось в типе.

## 3. Как маппить PostgreSQL-ENUM

В базе 9 своих типов: `chat_role`, `chat_mode`, `platform`, `assignment_status`, `exercise_level`,
`answer_type`, `verdict`, `quiz_status`, `material_source`. Значения в нижнем регистре (`in_progress`),
а в Java константы в верхнем (`IN_PROGRESS`). Поэтому `@Enumerated` не подходит, нужен конвертер.
Это тот же приём, что уже есть у `MessageRole`, только общий для всех.

```java
// model/DbEnum.java — значение в базе и в JSON
public interface DbEnum {
    @JsonValue String value();

    static <E extends Enum<E> & DbEnum> E fromValue(Class<E> type, String value) {
        return Arrays.stream(type.getEnumConstants()).filter(e -> e.value().equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + type.getSimpleName() + ": " + value));
    }
}

// model/converter/DbEnumConverter.java — один конвертер на все enum'ы
public abstract class DbEnumConverter<E extends Enum<E> & DbEnum> implements AttributeConverter<E, String> {
    private final Class<E> type;
    protected DbEnumConverter(Class<E> type) { this.type = type; }
    public String convertToDatabaseColumn(E v) { return v == null ? null : v.value(); }
    public E convertToEntityAttribute(String v) { return v == null ? null : DbEnum.fromValue(type, v); }
}

// пример enum'а
public enum QuizStatus implements DbEnum {
    IN_PROGRESS("in_progress"), COMPLETED("completed"), ABANDONED("abandoned");
    private final String value;
    QuizStatus(String value) { this.value = value; }
    public String value() { return value; }
}

@Converter(autoApply = true)
public class QuizStatusConverter extends DbEnumConverter<QuizStatus> {
    public QuizStatusConverter() { super(QuizStatus.class); }
}
```

У поля сущности:

```java
@ColumnTransformer(write = "?::quiz_status")                       // запись: строка → enum базы
@Column(nullable = false, columnDefinition = "quiz_status")        // чтобы ddl-auto: validate не ругался
private QuizStatus status = QuizStatus.IN_PROGRESS;
```

`exercises.question_type` в базе TEXT (`single_choice` / `multiple_choice` / `text`), для него тоже enum
`QuestionType`, но без `@ColumnTransformer`.

**Обязательно в `application.yml`**, иначе любой поиск по enum падает
(`ERROR: operator does not exist: platform = character varying`, например `findByUserIdAndPlatform`):

```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        stringtype: unspecified   # строки уходят без типа, PostgreSQL сам приводит их к своим ENUM
  jpa:
    hibernate:
      ddl-auto: validate          # при старте сверить все сущности с таблицами
```

С `validate` бэкенд по-прежнему не создаёт и не меняет таблицы, но не запустится, если колонка или тип
в Java не совпадают с базой, и напишет, что именно не так. Со всеми сущностями ниже он стартует без ошибок.

## 4. Какие сущности нужны

Правило: если таблицу пишет функция базы (она проверяет JSON, считает баллы и т. п.), сущность делается
только для чтения (`@Immutable`, без `@GeneratedValue`), а запись идёт методом репозитория, который вызывает
функцию. Так в Java не повторяется логика, которая уже есть в базе.

| Сущность | Таблица | Кто пишет | Репозиторий: главные методы |
|---|---|---|---|
| `User` ✅ есть | `users` | Java | `findByEmailIgnoreCase`; + `findDashboard`, `findTopicProgress`, `findDailyActivity` (виды) |
| `ChatSession` ✅ есть | `chat_sessions` | Java или `start_chat()` | + `startChat`, `systemPrompt`, `promptVars`, `setLanguage` |
| `ChatMessage` ✅ есть | `chat_messages` | Java | `findContext` (вид `chat_context`) |
| `PlatformConnection` | `platform_connections` | Java (`credentialsEnc` шифровать AES-GCM) | `findByUserIdAndPlatform`, `findByIdAndUserId` |
| `Assignment` | `assignments` | Java, upsert из `collect()` | `findForUser`, `upsert(...)`, `attachHomeworkFile` |
| `Subject` | `subjects` | функции базы | `findByName` (через `norm_key`) |
| `QuizSet` | `quiz_sets` | `import_quiz_json()` | `find` (= `find_quiz_set`), `importJson` |
| `Exercise` | `exercises` | `import_quiz_json()` | `findByQuizSetIdOrderByPosition` |
| `Quiz` | `quizzes` | `start_quiz()`, `finish_quiz()` | `start`, `findPublicQuestions`, `saveAnswer`, `finish`, `findProgress` |
| `QuizQuestion` | `quiz_questions` | `start_quiz()` | `findByQuizIdOrderByPosition` |
| `ExerciseAttempt` | `exercise_attempts` | `save_quiz_answer()` | `findByQuizIdOrderByCreatedAt` |
| `FlashcardProgress` | `flashcard_progress` | функции V9 и триггер | `findDueCards`, `addQuizSet`, `review`, `findStats` |
| `Material` | `materials` | `add_material()`, `attach_homework_file()` | `findVisible`, `add`, `search`, `forPrompt` |
| `MaterialChunk` | `material_chunks` | `add_material()` | `findByMaterialIdOrderByPosition` (колонку `tsv` не маппить) |
| `AiPrompt` | `ai_prompts` | `save_prompt()` из `load.py` | `findByKeyAndActiveTrue`, `getActiveContent` |
| `ChatModePrompt` | `chat_mode_prompts` | миграция V13 | `findAll` |
| `Reminder` | `reminders` | Java | `findDue` |
| `Achievement` | `achievements` | миграция V6 | `findAll` |
| `UserAchievement` | `user_achievements` | Java | `unlock` (повтор ничего не делает) |
| `UserEvent` | `user_events` | Java и функции базы | `add`, лента новых сверху |

### Новые поля в существующих сущностях (V13–V14)

```java
// ChatSession
@Column(name = "assignment_id") private Long assignmentId;
@ColumnTransformer(write = "?::chat_mode")
@Column(nullable = false, updatable = false, columnDefinition = "chat_mode")
private ChatMode mode = ChatMode.TUTOR;                      // не меняется (триггер chat_mode_locked)
@Column(name = "subject_id") private Long subjectId;
@Column(name = "quiz_set_id") private Long quizSetId;
private String language;                                     // null — тьютор сначала спросит язык
@ColumnTransformer(write = "?::exercise_level")
@Column(nullable = false, columnDefinition = "exercise_level")
private ExerciseLevel level = ExerciseLevel.INTERMEDIATE;

// ChatMessage
@Column(name = "prompt_id") private Long promptId;           // с какой версией промпта ответил ИИ
// и у role: @Column(nullable = false, columnDefinition = "chat_role")

// User
@Column(nullable = false, unique = true, columnDefinition = "citext") private String email;
```

### Особые случаи

- **JSON-колонки** (`quiz_sets.source_json`, `exercises.options`, `user_events.payload`):
  `@JdbcTypeCode(SqlTypes.JSON) private JsonNode ...;`
- **Массив** `quiz_sets.material_ids bigint[]`:
  `@JdbcTypeCode(SqlTypes.ARRAY) @Column(columnDefinition = "bigint[]") private Long[] materialIds;`
- **Составной ключ** (`quiz_questions`, `flashcard_progress`, `user_achievements`): `@IdClass(X.Key.class)`,
  где `Key` — вложенный `static class implements Serializable` с `equals` и `hashCode`.
- **`chat_mode_prompts.mode` — первичный ключ и enum.** JPA не применяет конвертеры к `@Id`, при чтении
  падает `Bad value for type byte : tutor`. Решение: id как `String`, геттер
  `getMode() { return ChatMode.fromValue(mode); }`, репозиторий `JpaRepository<ChatModePrompt, String>`.

## 5. Как вызывать функции базы из репозитория

Native-запрос `SELECT функция(...)`. Методы, которые пишут, помечаются `@Transactional`: иначе Spring Data
может выполнить их в транзакции только для чтения, и функция не сможет сделать INSERT.

```java
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Transactional
    @Query(value = "SELECT start_quiz(:userId, :setId)", nativeQuery = true)
    Long start(@Param("userId") Long userId, @Param("setId") Long setId);

    interface AnswerResult { Long getAttemptId(); String getVerdict(); String getExplanation(); String getCorrectAnswer(); }

    // single/multiple_choice проверяет база (verdict = null), для text передать вердикт ИИ
    @Transactional
    @Query(value = """
            SELECT attempt_id AS attemptId, CAST(verdict AS text) AS verdict, explanation, correct_answer AS correctAnswer
            FROM save_quiz_answer(:quizId, :position, :answer, CAST(:verdict AS verdict), :explanation)
            """, nativeQuery = true)
    AnswerResult saveAnswer(@Param("quizId") Long quizId, @Param("position") Integer position,
                            @Param("answer") String answer, @Param("verdict") String verdict,
                            @Param("explanation") String explanation);

    // вопросы для фронта БЕЗ правильных ответов — только из вида quiz_questions_public
    interface PublicQuestion { Integer getPosition(); Long getExerciseId(); String getQuestionType(); String getQuestion(); String getOptions(); }

    @Query(value = """
            SELECT position, exercise_id AS exerciseId, question_type AS questionType, question,
                   CAST(options AS text) AS options
            FROM quiz_questions_public WHERE quiz_id = :quizId ORDER BY position
            """, nativeQuery = true)
    List<PublicQuestion> findPublicQuestions(@Param("quizId") Long quizId);
}
```

Upsert ДЗ после `collect()`:

```java
@Modifying
@Transactional
@Query(value = """
        INSERT INTO assignments (user_id, source, external_id, title, course, due_at, status, url, description, kind)
        VALUES (:userId, CAST(:source AS platform), :externalId, :title, :course, :dueAt,
                CAST(:status AS assignment_status), :url, :description, :kind)
        ON CONFLICT (user_id, source, external_id) DO UPDATE
        SET title = EXCLUDED.title, course = EXCLUDED.course, due_at = EXCLUDED.due_at, status = EXCLUDED.status,
            url = EXCLUDED.url, description = EXCLUDED.description, kind = EXCLUDED.kind, updated_at = now()
        """, nativeQuery = true)
int upsert(...);
```

Чат с режимом и промптом из базы (вместо `Prompts.java`):

```java
@Transactional
@Query(value = "SELECT start_chat(:userId, CAST(:mode AS chat_mode), :subject, :topic, :title, :assignmentId)",
        nativeQuery = true)
Long startChat(...);

// перед каждым запросом к ИИ: промпт режима + subject, topic, language
@Query(value = """
        SELECT prompt_id AS promptId, prompt_key AS promptKey, version, content, CAST(mode AS text) AS mode,
               user_id AS userId, subject_id AS subjectId, subject, topic, language
        FROM chat_system_prompt(:sessionId)
        """, nativeQuery = true)
SystemPrompt systemPrompt(@Param("sessionId") Long sessionId);

@Query(value = "SELECT CAST(chat_prompt_vars(:sessionId) AS text)", nativeQuery = true)
String promptVars(@Param("sessionId") Long sessionId);   // JSON для {{...}}
```

`UserEventRepository` можно оставить на `JdbcTemplate` или перевести на JPA: сущность `UserEvent` только для
чтения, запись через `@Modifying @Transactional @Query(native INSERT ...) void add(...)`. Сигнатура
`add(userId, type, refTable, refId)` не меняется, `AuthService` и `ChatService` трогать не нужно.

## 6. Как проверить, что всё сходится

Интеграционный тест `src/test/java/com/lekseradar/repository/RepositoriesIntegrationTest.java`:

```java
@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_DB_URL}",
        "spring.datasource.username=${TEST_DB_USER:lekseradar}",
        "spring.datasource.password=${TEST_DB_PASSWORD:lekseradar}",
        "app.jwt.secret=integration-test-secret-integration-test",
        "app.openai.api-key=sk-not-used"})
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")   // без базы тест пропускается
@Transactional                                                           // откат: в базе ничего не остаётся
class RepositoriesIntegrationTest { ... }
```

Пять тестов: регистрация и вход через `AuthService`; ДЗ и платформы (upsert дважды → одна строка, файл ДЗ,
напоминание, ачивка); чат с промптом (`save_prompt` → `start_chat` → `chat_system_prompt`); квиз → ответы →
балл → карточки → прогресс; материалы и полнотекстовый поиск.

Запуск: `TEST_DB_URL=jdbc:postgresql://localhost:5432/lekseradar ./mvnw test`. Результат на базе V1–V16:
8 из 8 тестов прошли (5 интеграционных + 3 `JwtServiceTest`), после запуска в базе 0 пользователей.

Имя класса должно кончаться на `Test`, а не на `IT`, иначе `mvn test` его не запустит.

## 7. Что нашлось при проверке

| Проблема | Где | Решение |
|---|---|---|
| `operator does not exist: platform = character varying` | любой `findBy...(enum)` | `stringtype: unspecified` |
| `Bad value for type byte : tutor` | `ChatModePrompt` с enum в `@Id` | id как `String` |
| функция с INSERT в транзакции только для чтения | методы репозиториев, которые вызывают функции | `@Transactional` на методе |
| в `database/ENTITIES.md` остались `is_demo` и `AssignmentResponse.demo` | документ | убрать |
| роль сообщения названа `ChatRole` | `database/ENTITIES.md` | класс называется `MessageRole` |
| multiple_choice с частью верных вариантов | `save_quiz_answer` | это `partial`, не `incorrect`: так и задумано |

## 8. Что останется после сущностей

- Нет сервисов и контроллеров для ДЗ, платформ, квизов, карточек, дашборда, материалов и напоминаний.
  Список DTO для них есть в `database/ENTITIES.md` (раздел «DTO, которые ещё нужны для API»).
- `ChatService` создаёт чат сам и берёт промпт из `Prompts.java`. Нужно: `startChat` + `systemPrompt` +
  `promptVars`, а в ответ ИИ записывать `promptId`.
- В `CreateChatRequest` добавить `mode`, `subject`, `assignmentId`, `level`; в `ChatResponse` — `mode`,
  `subject`, `language`, `assignmentId`, `quizSetId`.
- `UpdateProfileRequest.locale` пропускает `nb|nn|en|ru`, а репетитор предлагает ещё `uk`.
