# GDPR and compliance

How LekseRadar meets the GDPR (which applies in Norway through the EEA Agreement together with the Personal Data
Act, *personopplysningsloven*) and other rules, mapped to the code. The last section lists what the operator has to
do outside the code before launch. This is the team's own assessment, not legal advice; a school using LekseRadar
should have it reviewed by its data protection officer.

The user-facing texts are [PRIVACY.md](PRIVACY.md) (site: `/privacy`) and [TERMS.md](TERMS.md) (site: `/terms`).
Technical measures: [SECURITY.md](SECURITY.md). Paths to Java files are relative to
`backend/src/main/java/com/lekseradar/`.

## 1. Principles (Art. 5)

| Principle | How it is met |
|---|---|
| Lawfulness, transparency | Legal basis per purpose in the privacy policy; policy and terms accepted at sign-up, with time and version stored (`users.terms_accepted_at`, `terms_version`, V27) |
| Purpose limitation | Data is used only to provide the service; no ads, profiling or AI training |
| Data minimisation | The AI provider gets no name or email (`chat_prompt_vars()`, V20); IP addresses are not stored; the collector stores nothing |
| Accuracy | Profile editable; homework refreshed from the platforms every 30 minutes |
| Storage limitation | Accounts inactive for 24 months are deleted (`service/RetentionScheduler.java`); disconnecting a platform deletes its data; unfinished sign-ups are forgotten after 15 minutes |
| Integrity and confidentiality | See [SECURITY.md](SECURITY.md): encryption of platform access, hashing, rate limits, CSP, secrets generated per installation |

## 2. Legal basis (Art. 6) and children (Art. 8)

| Processing | Basis |
|---|---|
| Account, homework collection, AI tutor, quizzes, flashcards, progress | Contract, Art. 6(1)(b) |
| Email reminders (opt-out in Settings) | Legitimate interest, Art. 6(1)(f) |
| Security, rate limiting, technical logs | Legitimate interest, Art. 6(1)(f) |

Sign-up requires confirming age 13+ or a parent's or guardian's agreement (`dto/auth/RegisterRequestDTO.java`,
`acceptTerms`), in line with the Norwegian age limit of 13 (personopplysningsloven § 5). Texts for students are
written in plain language (Art. 12).

## 3. Data subject rights (Art. 12–22)

| Right | Implementation |
|---|---|
| Information (13, 14) | `/privacy` in three languages |
| Access (15), portability (20) | Settings → *Download my data*: `GET /api/v1/users/me/export`, machine-readable JSON (`service/AccountService.java`, `repository/UserRepository.java`) |
| Rectification (16) | Profile and settings (`PATCH /api/v1/users/me`, `PUT /api/v1/users/me/password`) |
| Erasure (17) | Settings → *Delete account*: `DELETE /api/v1/users/me` with the password; platform tokens revoked first, then everything deleted in cascade |
| Restriction, objection (18, 21) | Reminders can be turned off; other requests by contact |
| Automated decisions (22) | None with legal or similarly significant effect; AI results are for practice |

Requests are answered within one month (Art. 12(3)).

## 4. Records of processing (Art. 30)

| Category | Data subjects | Data | Recipients | Retention |
|---|---|---|---|---|
| Accounts | Students | Name, email, password hash, settings, consent record | Hosting, email service | Until deletion or 24 months inactive |
| Homework collection | Students | Platform access (encrypted), assignments | School platforms, Google, hosting | Until disconnect / deletion |
| AI learning | Students | Chats, quizzes, answers, flashcards, activity | AI provider, hosting | Until deletion |
| Reminders | Students | Email, assignment titles and deadlines | Email service | Until deletion |

## 5. Processors (Art. 28) and transfers (Chapter V)

| Processor | Role | Location |
|---|---|---|
| AI provider (OpenAI-compatible; example configuration: Groq) | Generates tutor answers, quizzes, flashcards; checks answers | USA |
| Email provider (example: Gmail) | Sends sign-up codes and reminders | USA / EEA depending on account |
| Google (OAuth, Classroom API) | Classroom sign-in and read-only API | USA / EEA |
| Server hosting (backend, collector, database) | Runs the service | Chosen by the operator |
| Vercel | Hosts the website | USA / global edge |

## 6. Security (Art. 32), breaches (Art. 33–34), privacy by design (Art. 25)

- Measures: [SECURITY.md](SECURITY.md).
- By default: reminders can be turned off, minimum data to the AI, no tracking, no third-party scripts or fonts,
  read-only platform scopes, secrets generated per installation.
- Breach procedure: contain the issue, assess the risk, notify Datatilsynet within 72 hours if there is a risk to
  people, notify affected students without undue delay if the risk is high, and record the breach.

## 7. Other rules

| Topic | Status |
|---|---|
| Cookies (*ekomloven*) | Only strictly necessary browser storage (login token, theme, language); no consent banner needed |
| AI Act | AI is used for learning support, not for grading or admission; users are told they talk to an AI ("AI tutor") and that answers can be wrong (`/terms`) |
| Platform terms | Canvas, Microsoft Graph and Google Classroom are official APIs; itslearning and Visma InSchool use unofficial access and are marked *Unofficial* in the UI |
| Accessibility | Semantic HTML, keyboard use and both themes; a formal WCAG audit has not been done |
| Licenses | No license file yet — the contributors must choose one. Dependencies use MIT, ISC, Apache-2.0, BSD and OFL (Inter font); psycopg (LGPL-3.0) is used only by scripts |

## 8. Operator to-do before launch

These cannot be done in code:

1. Name the data controller and a contact address for privacy requests on `/privacy` (currently: GitHub issues and
   the self-service functions).
2. Sign data processing agreements with every processor in section 5 and confirm their transfer safeguards (EU
   standard contractual clauses or the EU–US Data Privacy Framework), as the privacy policy states.
3. Choose server hosting, preferably in the EEA, and record it in section 5.
4. Carry out a DPIA (Art. 35) — the service processes children's data with AI.
5. For schools: sign a data processing agreement with each school (the school is then the controller).
6. Revoke the email app password that was once committed to git.
