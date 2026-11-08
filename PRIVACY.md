# Privacy Policy

*Version of 8 November 2026* (`TERMS_VERSION` in `backend/src/main/java/com/lekseradar/service/AuthService.java`).
The same policy is shown on the site at `/privacy` in Norwegian (bokmål), English and Russian
(`frontend/src/legal/`). If you change one, change the other and the version date.

This policy explains what personal data LekseRadar processes, why, on which legal basis under the GDPR, who
receives it, how long it is kept and what your rights are. Paths to Java files are relative to
`backend/src/main/java/com/lekseradar/`.

## The short version

- We process only what LekseRadar needs to work: your account, connected platforms, homework, chats with the AI
  tutor and learning progress.
- Access to school platforms is stored encrypted. Passwords are stored only as hashes.
- The AI service receives the text of your chats and of the assignment you ask about — not your name or email.
- No advertising, no tracking, no selling of data.
- You can download all your data and delete your account yourself in Settings.
- An account without a login for 24 months is deleted automatically with all its data.

## Who is responsible

LekseRadar is run by the LekseRadar team, which started the project at the Oslo Sprint hackathon
(6–8 November 2026). The team is the data controller for accounts that students create themselves.

If a school gives LekseRadar to its students, the school is the data controller and the team processes the data on
the school's behalf under a data processing agreement (GDPR Art. 28).

## What we process

| Data | Table (migration) |
|---|---|
| Account: name, email, password hash (BCrypt), language, time zone, reminder setting, sign-up and last login time, when and which version of the terms was accepted | `users` (V1, V25, V27) |
| Connected platforms: which platforms, access tokens or session cookies (encrypted, AES-256-GCM), last update time and result | `platform_connections` (V2) |
| Homework: title, course, deadline, status, link, description, own "done" mark | `assignments` (V2, V21) |
| AI tutor chats: title, topic, all messages | `chat_sessions`, `chat_messages` (V3, V13, V14) |
| Learning: quizzes, answers and verdicts, flashcard progress, activity history, achievements | `quizzes`, `exercise_attempts`, `flashcard_progress`, `user_events`, `user_achievements` (V4–V9) |
| Email reminders already sent | `reminders` (V6, V25) |

Not stored: the plain password; the sign-up code (only a SHA-256 hash in memory for at most 15 minutes,
`service/AuthService.java`); IP addresses (only in memory to limit sign-up emails and login attempts,
`security/RateLimiter.java`).

## Why, and on which legal basis

| Purpose | Legal basis |
|---|---|
| Providing LekseRadar as described in the [terms of use](TERMS.md): account, homework collection, AI tutor, quizzes, flashcards, progress | Performance of a contract (GDPR Art. 6(1)(b)) |
| Email reminders 24 hours before a deadline (on by default, can be turned off) | Legitimate interest (Art. 6(1)(f)); right to object by turning them off |
| Security and abuse prevention (rate limits, technical logs) | Legitimate interest (Art. 6(1)(f)) |
| Record of accepting the terms | Demonstrating what was agreed (Art. 7(1)) |

Data is not used for advertising, profiling or training AI models.

## Who receives data

| Recipient | What | Code |
|---|---|---|
| AI service (OpenAI-compatible, e.g. Groq, USA) | Chat messages and history; title, deadline and text of the assignment; subject, topic, level, language; weakest topics; text answers to check. Not name or email. | `service/ai/OpenAiService.java`, `chat_prompt_vars()` (V20) |
| Email service (e.g. Gmail) | Email, name, sign-up code, reminder contents | `service/MailService.java`, `service/ReminderMail.java` |
| Google (OAuth, Classroom API) | Sign-in at Google; read-only access to courses and coursework; revoked on disconnect and account deletion | `service/google/GoogleOAuthClient.java` |
| School platforms | Requests with the student's own access to read assignments | `homework-collector/homework/sources/` |
| Hosting (backend and database servers; Vercel for the website) | All data above is stored on or passes through them; technical logs such as IP addresses | `docker-compose.yml`, `frontend/vercel.json` |

Transfers outside the EU/EEA are made under the safeguards in the providers' data processing terms (EU standard
contractual clauses or the EU–US Data Privacy Framework). The site loads no third-party fonts or scripts
(`frontend/src/main.tsx` uses a bundled font).

## Cookies and browser storage

No tracking or advertising cookies. The browser stores only what the site needs: the login token, theme and language
in `localStorage` (`frontend/src/api/client.ts`, `frontend/src/theme.ts`, `frontend/src/i18n/lang.ts`).

## Retention

| Data | Kept until |
|---|---|
| Account and all data | Deleted by the student, or automatically after 24 months without a login (`service/RetentionScheduler.java`, `RETENTION_INACTIVE_MONTHS`) |
| Platform access and its homework, with chats, quizzes, flashcards and history made for it | Platform disconnected (`service/PlatformService.java`, `repository/AssignmentRepository.java`) |
| A chat | Chat deleted |
| Unfinished sign-up | 15 minutes, in memory |

## Your rights

| Right | How |
|---|---|
| Access and portability (Art. 15, 20) | Settings → *Download my data*: one JSON file (`GET /api/v1/users/me/export`, `service/AccountService.java`) |
| Rectification (Art. 16) | Profile and Settings: name, language, time zone, password |
| Erasure (Art. 17) | Settings → *Delete account*, confirmed with the password (`DELETE /api/v1/users/me`) |
| Restriction and objection (Art. 18, 21) | Turn off reminders in Settings, or contact us |
| Complaint | [Datatilsynet](https://www.datatilsynet.no) |

Requests are answered within one month.

## AI and automated decisions

AI generates tutor answers, quizzes and flashcards and checks text answers in quizzes. These are for practice only.
LekseRadar makes no decisions with legal or similarly significant effects (Art. 22).

## Children and young people

To create an account, the student confirms being at least 13 years old or that a parent or guardian agrees; the
backend refuses sign-up without this confirmation (`dto/auth/RegisterRequestDTO.java`) and stores when it was given
(V27). In Norway the age limit for one's own consent to online services is 13 (Personal Data Act § 5).

## Security

See [SECURITY.md](SECURITY.md). A personal data breach is reported to Datatilsynet within 72 hours, and to the
people affected without undue delay when the law requires it (Art. 33, 34).

## Changes

The version date at the top changes with every update. Important changes are announced on the site before they
apply.

## Contact

Most requests can be done in Settings. Other questions: open a GitHub issue in this repository without personal
data. Security problems: report privately as described in [SECURITY.md](SECURITY.md).
