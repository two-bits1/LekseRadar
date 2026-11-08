# Privacy

This document describes what personal data LekseRadar stores, why, who receives it and how long it is kept.
It is written from the code in this repository: the database schema in `database/migrations` (V1–V26) and
`database/ENTITIES.md`, and the backend. Paths to Java files are relative to
`backend/src/main/java/com/lekseradar/`. The same information is shown to users on the site at `/privacy`
(`frontend/src/pages/Legal.tsx`).

## What data is stored

All data is stored in one PostgreSQL database run by the backend. The homework collector
(`homework-collector/`) stores nothing (`homework-collector/homework/server.py`).

| Group | Data | Table (migration) |
|---|---|---|
| Account | name, email, password hash (BCrypt, never the password itself), interface and tutor language, time zone, email reminder setting, sign-up time, last login time | `users` (V1, V25) |
| Platform access | which platforms are connected; tokens or session cookies for each platform, **encrypted** with AES-256-GCM; last sync time and last sync error | `platform_connections` (V2) |
| Assignments | title, course, deadline, status, link, description and type of each assignment collected from the connected platforms; your own "done" mark | `assignments` (V2, V21) |
| AI tutor chats | chat title, topic, subject, language, level, archive flag; every message you and the AI tutor write, with the model name and token count of each answer | `chat_sessions`, `chat_messages` (V3, V13, V14) |
| Quizzes, flashcards and progress | quizzes and questions generated for you, your answers with the verdict and explanation, flashcard review progress (spaced repetition), an activity history (for example: logged in, homework synced, quiz completed), achievements | `quizzes`, `quiz_questions`, `exercises`, `exercise_attempts`, `quiz_sets`, `flashcard_progress`, `user_events`, `user_achievements` (V4–V9) |
| Reminders | which assignments you were reminded about by email and when | `reminders` (V6, V25) |
| Homework files | text of a homework file attached to an assignment (the schema supports it; the current site does not upload files) | `materials`, `material_chunks` (V10, V14) |

Not stored in the database:

- **Pending sign-ups.** Until you enter the email code, your name, password hash and a SHA-256 hash of the code are
  kept only in the backend's memory, for at most 15 minutes (`service/AuthService.java`).
- **IP addresses** are used only in memory, to limit how many sign-up emails are sent from one address
  (`service/AuthService.java`, `security/RateLimiter.java`). The hosting providers may log them (see below).
- **In your browser** (`localStorage`): your login token (`token`), theme (`lekseradar-theme`) and language
  (`lekseradar-lang`) (`frontend/src/api/client.ts`, `frontend/src/theme.ts`, `frontend/src/i18n/lang.ts`).
  The site uses no advertising, analytics or tracking scripts.

## Why the data is needed

| Group | Purpose |
|---|---|
| Account | to log you in, show your name, use your language and time zone for deadlines, and send the emails you asked for |
| Platform access | to fetch your assignments from Canvas, Microsoft Teams, itslearning, Visma InSchool and Google Classroom, automatically every 30 minutes and when you press *Refresh* |
| Assignments | to show all homework in one list sorted by deadline, and to give the AI tutor and quizzes the context of the task |
| AI tutor chats | to continue a conversation: the chat history is sent to the AI with each new question |
| Quizzes, flashcards and progress | to check answers, schedule flashcard reviews, and show progress by topic, the activity history and achievements |
| Reminders | to send at most one email per assignment, 24 hours before the deadline (you can turn this off in Settings) |
| Homework files | to give the AI tutor the full text of a task |

## Who receives data

| Recipient | What they receive | Why |
|---|---|---|
| **AI provider** with an OpenAI-compatible API, set by `OPENAI_BASE_URL` (`backend/.env.example` uses **Groq**, a company in the USA) | the text of your chat messages and the chat history; the title, deadline, course and text of the assignment the chat is about; subject, topic, level, language and your weakest topics with percentages; quiz questions and your text answers for checking | to answer questions, generate quizzes and flashcards, and check answers (`service/ai/OpenAiService.java`, `chat_prompt_vars()` in `database/migrations/V20__language_code.sql`). Your name and email are not sent. |
| **Email (SMTP)**, by default **Gmail** (`MAIL_HOST=smtp.gmail.com`) | your email address and name; the sign-up code; in reminders, the titles, courses, deadlines and links of assignments due soon | sign-up confirmation and reminders (`service/MailService.java`, `service/ReminderMail.java`) |
| **Google** (OAuth and Classroom API) | your Google sign-in happens at Google; LekseRadar receives read-only access to your courses and coursework and uses it to read assignments; the access is revoked at Google when you disconnect | Google Classroom integration (`service/google/GoogleOAuthClient.java`) |
| **Google Fonts** | your IP address and browser details when the page loads the Inter font | page typography (`frontend/src/styles.css`) |
| **School platforms**: Canvas (Instructure), Microsoft Teams (Microsoft Graph), itslearning, Visma InSchool, Google Classroom | requests made with your own access, from the collector's server | to read your assignments (`homework-collector/homework/sources/`) |
| **Hosting**: the servers that run the backend, collector and database, and **Vercel** for the website | all data above passes through or is stored on these servers; hosting providers may keep technical logs such as IP addresses | running the service (`docker-compose.yml`, `frontend/vercel.json`) |

LekseRadar does not sell data and does not share it for advertising. Several recipients are outside the EU/EEA;
see [COMPLIANCE.md](COMPLIANCE.md).

## Retention and deletion

- Data is kept until it is deleted. There are no automatic deletion periods yet.
- **Disconnecting a platform** deletes the stored access, all assignments from that platform and everything made for
  them: chats, quizzes, flashcards, answers and activity history. Shared question sets used by other students are kept
  (`repository/AssignmentRepository.java`, `service/PlatformService.java`, `database/migrations/V26__disconnect_removes_assignments.sql`).
- **Deleting a chat** deletes it with all its messages (`controller/ChatController.java`).
- **Email reminders** can be turned off in Settings (`users.email_reminders`).
- **Deleting an account is not possible yet.** There is no button or API for it. Account deletion and data export
  are planned. Until then, ask for deletion as described under [Contact](#contact).

## Children and young people

LekseRadar is meant for school students, many of whom are under 18.

- In Norway, a child can give their own consent to the processing of personal data for online services from the
  age of **13** (Personal Data Act, *personopplysningsloven* § 5). For younger children, a parent or guardian has to
  consent.
- When a school uses LekseRadar with its students, the **school is normally the data controller**. The school must
  take the age limit into account, choose the legal basis for the processing, and inform students and parents.

## Contact

Questions, or a request to delete data: **open a GitHub issue without personal data** in this repository.
Do not write your name, email, school or any student data in the issue. Report security problems privately as
described in [SECURITY.md](SECURITY.md).
