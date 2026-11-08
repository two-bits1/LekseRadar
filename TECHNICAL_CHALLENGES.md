# Technical Challenges

This document lists real problems the LekseRadar team ran into during the Oslo Sprint Hackathon and how each was
solved. Every item is based on commits and code in this branch; the commit hashes are given so the change can be
checked with `git show <hash>`.

Each item uses the same structure: **Problem**, **Why It Happened**, **Solution**, **Result**.

- [Frontend](#frontend)
- [Backend (Java)](#backend-java)
- [Python API, Database & AI Content](#python-api-database--ai-content)

---

## Frontend

### 1. Moving from demo data to the real API without breaking the UI
- **Problem:** The first version of the site ran on built-in demo data. The backend API was being written at the
  same time, and its contract kept changing (quizzes, flashcards, subjects, chat modes).
- **Why It Happened:** Frontend and backend were built in parallel by different people during the hackathon, so
  the frontend could not wait for a finished API.
- **Solution:** The switch was split into milestones M1–M7. M1 (`fe081f5`) typed the whole API contract from
  `docs/API.md` in `api/types.ts` and `api/endpoints.ts`; M4–M6 (`95c1137`, `88083bc`, `a4ca6db`) moved the
  dashboard, tutor, quizzes and flashcards to the real endpoints; M7 (`8f208fd`) checked mobile and docs.
  Backend error messages are shown by HTTP status code for nb/en, and the backend text is used for ru.
- **Result:** All pages use the real API with no demo data. A later automated check found that all 40 frontend
  API calls exist in the backend (see [README-TEST.md](README-TEST.md)).

### 2. Registration had to become a two-step flow
- **Problem:** The backend added email verification, so `POST /auth/register` no longer returned a token.
- **Why It Happened:** Sign-up was changed to confirm that the student owns the email address
  (backend `db45af8`).
- **Solution:** `479f38c` turned the form into two steps: register (HTTP 202, no token), then enter the code from
  the email via `POST /auth/register/verify`, with *Send new code* and *Change email* actions.
- **Result:** A student can log in only after confirming the email, and the form handles a lost or mistyped code.

### 3. Low-contrast AI tutor button in one of the themes
- **Problem:** The AI tutor button on a homework card was hard to read after switching between dark and light themes.
- **Why It Happened:** A more specific style for the card's action area overrode the button's own colours, so the
  text and background did not follow the theme.
- **Solution:** `c538edd` gave the button explicit colours for both themes.
- **Result:** The button is readable in dark, light and automatic theme.

### 4. Chat input scrolled away with the messages
- **Problem:** In a long tutor chat the message input moved off screen and the student had to scroll down to type.
- **Why It Happened:** The input was part of the normal page flow under the message list.
- **Solution:** `df22d37` placed the input in a dock that stays fixed at the bottom of the screen, with space
  reserved so it does not cover the last message.
- **Result:** The input is always visible, on desktop and on phones.

### 5. AI quizzes with empty options and several correct answers
- **Problem:** Some AI-generated questions showed empty answer buttons, and a question with two correct answers
  allowed only one choice.
- **Why It Happened:** The language model did not always follow the JSON format: it sometimes returned empty strings
  as options or marked a question `single_choice` while giving several correct answers.
- **Solution:** The frontend hides empty options (`7e6b3bf`); the backend normalizes new questions
  (`ded63db`, see Backend item 5) and migration `V23` cleans quizzes that were already saved.
- **Result:** Questions show only real options, and multi-answer questions allow several choices.

### 6. A wrong answer looked "correct"
- **Problem:** After answering, the student's wrong choice was highlighted green, the same colour as the correct
  answer.
- **Why It Happened:** The option style marked the *picked* answer, not whether it was right.
- **Solution:** `3126aac` added a state per option (`picked`, `right`, `right missed`, `wrong`) in
  `quizLogic.ts`, with unit tests.
- **Result:** A wrong pick is red, the correct answer is green, and a correct answer the student missed has a dashed
  green outline.

### 7. Flashcard progress seemed not to be saved
- **Problem:** After a flashcard session the dashboard still showed 0 learned cards.
- **Why It Happened:** A card counted as "learned" only once its review interval reached three weeks, and cards
  answered *Again* were simply dropped from the session.
- **Solution:** `efd27da` repeats *Again* cards in the same session and shows the review count; migration `V24`
  counts a card as learned once its interval is at least one day.
- **Result:** Progress is visible right after a session, and difficult cards are practised until the student knows
  them.

### 8. Deep links failed after deploy
- **Problem:** On a static host, opening a page like `/homework` directly or reloading it gives a 404.
- **Why It Happened:** React Router handles routes in the browser, but the host looks for a real file at that path.
- **Solution:** `frontend/vercel.json` rewrites all paths to `index.html` and adds security headers (`398f021`).
- **Result:** Every page can be opened by URL and reloaded.

---

## Backend (Java)

### 1. Sharing one database schema with the Python part
- **Problem:** The Java backend first had its own tables, while the database (migrations, views and stored
  functions) was being built in the Python part of the repository.
- **Why It Happened:** Two parts of the team designed data storage in parallel.
- **Solution:** The backend moved to the shared schema (`6257b79`, `8ce8192`) and stopped creating tables itself.
  Hibernate runs with `ddl-auto: validate`, and the JDBC URL uses `stringtype=unspecified` so Java strings can be
  compared with PostgreSQL enum columns (`d7a0e67`).
- **Result:** One schema, owned by SQL migrations; the backend fails at startup if its entities do not match.

### 2. An accidental revert of the entity layer
- **Problem:** During integration the backend entities, repositories and a test were removed by a revert commit.
- **Why It Happened:** Code from a separate AI-assisted session was rolled back (`2b943e1`) before it was clear
  that other code depended on it.
- **Solution:** The rollback was itself reverted (`5abc468`), which restored the files without rewriting Git
  history.
- **Result:** The entity layer was back in one commit, and the history shows both steps.

### 3. The free AI tier returned "rate limit" errors
- **Problem:** After a quiz and a few tutor replies, students got a 502 error.
- **Why It Happened:** The Groq free tier allows about 8000 tokens per minute and answers HTTP 429 when the limit
  is reached.
- **Solution:** `23eabcc` reads the wait time from `Retry-After` or the "try again in Xs" message and repeats the
  request once if the wait is up to 10 seconds; `57e7f8d` also understands waits given in milliseconds and allows
  up to two retries. A per-user AI rate limit (`AiRateLimitConfig`) stops a single user from using up the quota.
- **Result:** Short limits are absorbed without the student seeing an error.

### 4. Every quiz was saved in Norwegian
- **Problem:** A quiz generated for a student using Russian or English was stored as Norwegian, so it could not be
  found again or checked in the right language.
- **Why It Happened:** The prompt variable `{{language}}` holds a readable name such as "русский (ru)", but the quiz
  JSON needs a code, and the backend took the language from the AI's reply.
- **Solution:** Migration `V20` added `language_code` to `chat_prompt_vars()`; `57e7f8d` makes
  `QuizGeneratorService` always take the quiz language from that code, and turns the database errors
  `quiz answer: …` into HTTP 400.
- **Result:** Quizzes are saved, reused and checked in the student's language.

### 5. AI output that does not match the quiz format
- **Problem:** Some AI questions had empty or duplicate options or the wrong question type.
- **Why It Happened:** A language model does not reliably follow a JSON schema.
- **Solution:** `ded63db` added `fixQuestion` in `QuizGeneratorService`: it removes empty and duplicate options
  and turns a `single_choice` question with several correct answers into `multiple_choice` before
  `import_quiz_json` stores it.
- **Result:** New quizzes are valid when they reach the database and the frontend.

### 6. Platform errors logged the student out
- **Problem:** A bad or expired platform token could make the frontend send the student back to the login page.
- **Why It Happened:** The frontend treats HTTP 401 as "your session has expired".
- **Solution:** `PlatformService` and `CollectorClient` never return 401 for platform problems: wrong platform
  credentials give 422, and an unavailable collector gives 503.
- **Result:** The student stays logged in and sees what is wrong with the platform connection.

### 7. Storing school platform credentials safely
- **Problem:** The backend has to keep tokens, refresh tokens and session cookies for the student's school
  platforms.
- **Why It Happened:** Homework is refreshed automatically every 30 minutes, so credentials have to be stored, not
  only used once.
- **Solution:** `CredentialsCipher` encrypts credentials with AES-GCM using `CREDENTIALS_KEY`, which is required at
  startup. Platform URLs are checked against allow-lists (`instructure.com`, `itslearning.com`,
  `inschool.visma.no`) so a user cannot make the server call an arbitrary host, and the Google sign-in can only
  return to a fixed path.
- **Result:** Credentials are not stored in plain text, and the sync cannot be used to reach internal addresses.

### 8. Sign-up emails without a mail server
- **Problem:** Email verification needs SMTP, which is not configured on a developer's machine.
- **Why It Happened:** Sign-up was made email-verified (`db45af8`), so local testing needed a mail account.
- **Solution:** With `MAIL_DEV_LOG_CODES=true` the code is written to the log instead of being sent; without mail
  and without that flag the API returns 503 instead of failing silently (`MailService`).
- **Result:** Developers can test sign-up locally, and a missing mail setup in production is reported clearly.

---

## Python API, Database & AI Content

### 1. Five platforms, five different sign-in methods
- **Problem:** Homework had to come from Canvas, Microsoft Teams, itslearning, Visma InSchool and Google Classroom.
- **Why It Happened:** Each platform authenticates differently: Canvas uses a personal token, Teams uses Microsoft
  Graph with device-code sign-in, itslearning uses OAuth with refresh tokens, InSchool uses session cookies, and
  Classroom uses Google OAuth.
- **Solution:** Each platform has its own adapter in `homework-collector/homework/sources/`, and all of them return
  the same `Assignment` model. Google sign-in and refresh live in the Java backend; the Classroom adapter only reads
  data with the access token it is given. Updated credentials (for example a new refresh token) are returned to the
  backend to be stored.
- **Result:** The backend calls one `collect` endpoint and gets one format, whatever the platform.

### 2. itslearning sometimes answers with HTML instead of JSON
- **Problem:** The collector crashed with a JSON parse error when itslearning returned an error page.
- **Why It Happened:** On a wrong school address or an expired session itslearning replies with an HTML page or an
  empty body, not a JSON error.
- **Solution:** `8d34b89` (token request) and `4c3086a` (task list) check the reply first. A server error (5xx)
  becomes "try again later", anything else becomes "sign in again" with `needs_reconnect`.
- **Result:** The student gets a clear message instead of an internal error.

### 3. Syncing without losing the student's own progress
- **Problem:** Each sync could overwrite what the student had changed on the site.
- **Why It Happened:** Platforms often keep showing an assignment as *to do* after the student has marked it done,
  and may send an empty description.
- **Solution:** Migration `V21` added `sync_assignments(user_id, jsonb)`: an upsert on
  `(user_id, source, external_id)` that keeps a *submitted* or *graded* status, does not replace a saved
  description with an empty one, and does not delete assignments that disappear from the platform, because chats
  and quizzes may be linked to them.
- **Result:** Repeated syncs are safe, and the function returns how many assignments were added and updated.

### 4. Keeping the collector away from the internet
- **Problem:** The collector receives platform credentials, so it must not be open to anyone.
- **Why It Happened:** It runs as a separate service next to the backend.
- **Solution:** The FastAPI service (`homework/server.py`) requires an `X-Internal-Key` header, binds to
  `127.0.0.1` in Docker Compose and is reached by the backend inside the Compose network. The contract is documented in
  [homework-collector/COLLECTOR-API.md](homework-collector/COLLECTOR-API.md).
- **Result:** Only the backend can call the collector.

### 5. Prompts that can change without a redeploy
- **Problem:** The tutor, quiz generator and answer checker prompts needed frequent tuning during the hackathon.
- **Why It Happened:** Prompt quality was found in testing, and redeploying the backend for each text change was too
  slow.
- **Solution:** Prompts are Markdown files in `ai-content/prompts`, loaded into the `ai_prompts` table with versions
  (`V12`) by `ai-content/load/load.py`. Variables such as `{{language}}` and `{{homework_text}}` come from
  `chat_prompt_vars()`. The answer checker marks the student's answer as data and tells the model not to follow
  instructions inside it, and the tutor prompt forbids revealing its instructions.
- **Result:** Prompts are updated by running the loader, and each version is kept.

### 6. Not generating the same quiz twice
- **Problem:** Each AI quiz costs time and tokens, which matters on a free tier.
- **Why It Happened:** Several students, or the same student, often ask for a quiz on the same topic and level.
- **Solution:** Quizzes are imported as JSON with `import_quiz_json`, and a ready quiz set for the same subject,
  topic, level and language is found and reused before the AI is called. Answers are scored by `finish_quiz`, and
  quiz questions can become flashcards (`V9`).
- **Result:** Repeated requests are answered from the database, and AI calls are only made for new topics.

### 7. Quizzes that were already saved with bad options
- **Problem:** The backend fix for empty and duplicate options (Backend item 5) only applies to new quizzes.
- **Why It Happened:** Quizzes generated before the fix were already stored.
- **Solution:** Migration `V23__quiz_options_cleanup.sql` removes empty and duplicate options in stored questions,
  keeping their order, and turns `single_choice` questions with several correct answers into `multiple_choice`.
- **Result:** Old and new quizzes behave the same way.

### 8. Database scripts hung on Windows
- **Problem:** Python database scripts hung when connecting to the local database on a team member's Windows PC.
- **Why It Happened:** On Windows, connecting to `localhost` hung, while the explicit IPv4 address `127.0.0.1`
  worked. A related Windows issue was CRLF line endings: with CRLF, the database init script in the Linux container
  could not run and `docker compose up` created an empty database.
- **Solution:** `0e075c6` uses `127.0.0.1` in the Python database URLs (`migrate.py`, `to_db.py`, docs), and a
  `.gitattributes` file keeps LF line endings for `*.sh` scripts.
- **Result:** The same commands work on macOS, Linux and Windows.
