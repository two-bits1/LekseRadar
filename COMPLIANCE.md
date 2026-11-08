# Compliance notes

> **This is not legal advice.** These notes were written by the development team from the code in this
> repository. They list questions that **should be reviewed** by a qualified person (for example the school's
> data protection officer or a lawyer) before LekseRadar is used with real students. Nothing here is a promise
> that the project complies with any law or contract.

LekseRadar is a hackathon prototype (Oslo Sprint, 6–8 November 2026) aimed at school students in Norway.
What data it processes is described in [PRIVACY.md](PRIVACY.md); technical measures and gaps are in
[SECURITY.md](SECURITY.md).

## 1. Data protection law

The GDPR applies in Norway through the EEA Agreement, together with the Norwegian Personal Data Act
(*personopplysningsloven*). The national supervisory authority is Datatilsynet.

### Roles

- If a **school or municipality** offers LekseRadar to its students, the school is likely the **controller** and
  whoever runs LekseRadar is likely a **processor**. A data processing agreement (*databehandleravtale*,
  GDPR Art. 28) would then be needed. This needs review.
- If LekseRadar is offered **directly to students**, the operator would likely be the controller. This needs review.
- The services that receive data (AI provider, Railway, Vercel, Gmail, Google; see [PRIVACY.md](PRIVACY.md#who-receives-data))
  would likely be sub-processors or independent recipients. Their terms and data processing agreements should be
  reviewed.

### Legal basis

The legal basis (GDPR Art. 6) has not been chosen. It should be decided by the controller, separately for at least:
collecting homework from school platforms, sending data to the AI provider, email reminders, and the activity
history. Consent is not the only possible basis, and for a school it may not be the right one. This needs review.

### Children

- Most users are likely to be minors. Under *personopplysningsloven* § 5, the age limit for a child's own consent to
  online services in Norway is **13**; below that, consent from a parent or guardian is needed where consent is the
  legal basis.
- Information to children should be in clear, simple language (GDPR Art. 12). The privacy page on the site
  (`/privacy`) should be reviewed with this in mind.
- A school acting as controller needs to take the age limit into account.

### Data protection impact assessment

The processing combines data about children, an AI service, and access to several school platforms. A DPIA
(GDPR Art. 35) is likely to be needed before real use. This needs review.

### Transfers outside the EU/EEA

Several recipients are companies based in the USA: the example AI provider Groq (`OPENAI_BASE_URL` in
`backend/.env.example`), Railway, Vercel and Google (Gmail, Google Fonts, Google Classroom). The transfer mechanism
(adequacy decision such as the EU–US Data Privacy Framework, standard contractual clauses, or another basis under
GDPR Chapter V) and the hosting regions should be checked for each of them.

### Data subject rights

| Right | Status in the code |
|---|---|
| Access (Art. 15) | Partly: the student can see their profile, assignments, chats, quizzes and history in the app. No full export. |
| Rectification (Art. 16) | Partly: name, language, time zone and password can be changed (`PATCH /api/v1/users/me`, `PUT /api/v1/users/me/password`). |
| Erasure (Art. 17) | **Missing**: no account deletion (`backend/src/main/java/com/lekseradar/controller/UserController.java`). Chats can be deleted; disconnecting a platform deletes its assignments. |
| Portability (Art. 20) | **Missing**: no export. |
| Objection / withdrawal | Email reminders can be turned off. Other processing has no opt-out. |

Account deletion and export should be implemented before real use.

### Data minimisation and retention

- There are no retention periods: chats, answers and the activity history (`user_events`) are kept until deleted.
  Retention periods should be defined (GDPR Art. 5(1)(e)).
- The AI provider receives assignment text and the student's weakest topics with percentages, but not the name or
  email (`chat_prompt_vars()` in `database/migrations/V20__language_code.sql`). Whether this is the minimum needed
  should be reviewed.

### Security and breaches

- Technical measures (GDPR Art. 32) are listed in [SECURITY.md](SECURITY.md). The project has had no security audit.
- A personal data breach generally has to be reported to Datatilsynet within 72 hours (GDPR Art. 33). There is no
  incident process yet; one should be defined.
- An email app password was committed to the git history of this repository. Its owner needs to revoke it.

## 2. AI

- The EU AI Act becomes relevant in Norway through the EEA Agreement and national law. The timeline and the exact
  obligations for Norway need review.
- Some AI systems used in education are listed as high-risk (AI Act Annex III, point 3), for example systems that
  evaluate learning outcomes. LekseRadar's AI checks practice quiz answers (`answer_checker` prompt in
  `ai-content/prompts`) but is not meant for grading. Whether any feature falls under Annex III needs review.
- Users should be told clearly that they are talking to an AI and that its answers can be wrong. The site calls the
  feature "AI tutor", and the terms of use (`/terms`) say that answers can be wrong; the wording should be reviewed.
- The AI provider's terms on data retention and use of inputs for training should be reviewed, especially because
  the inputs come from children.

## 3. School platforms

| Platform | How LekseRadar connects | What should be reviewed |
|---|---|---|
| Canvas | personal access token from the student | whether the school allows students to create tokens for third-party tools |
| Microsoft Teams | Microsoft Graph, device-code sign-in, an Entra ID app (`TEAMS_CLIENT_ID`) | the school's Microsoft 365 tenant may require admin consent |
| itslearning | the OAuth client of the itslearning **mobile app** (`homework-collector/homework/sources/itslearning.py`); marked *Unofficial* in the UI | this is not an official integration and may conflict with itslearning's terms and the school's agreement; permission should be obtained or the feature removed |
| Visma InSchool | the web app's internal API with the student's **session cookies** (`homework-collector/README.md`) | not an official integration; the same review as for itslearning |
| Google Classroom | official Classroom API with read-only scopes (`backend/src/main/java/com/lekseradar/service/google/GoogleOAuthClient.java`) | Google's API Services User Data Policy and OAuth app verification for the Classroom scopes; in testing mode Google expires the access after 7 days |

## 4. Email

Emails are sign-up codes and homework reminders (`backend/src/main/java/com/lekseradar/service/MailService.java`,
`ReminderMail.java`). Reminders can be turned off in Settings. They are service messages, not marketing; this
should be confirmed if the content changes.

## 5. Browser storage and third-party requests

- The site stores the login token, theme and language in `localStorage` (`frontend/src/api/client.ts`,
  `frontend/src/theme.ts`, `frontend/src/i18n/lang.ts`). There are no analytics or advertising scripts. Under the
  Norwegian Electronic Communications Act (*ekomloven*), storing information on the user's device needs consent
  unless it is strictly necessary for the service the user asked for. Whether all three items count as strictly
  necessary needs review.
- The font Inter is loaded from Google Fonts (`frontend/src/styles.css`), which sends the visitor's IP address to
  Google. Hosting the font together with the site should be considered.

## 6. Accessibility

If LekseRadar is used by schools, the Norwegian rules on universal design of ICT (*forskrift om universell
utforming av IKT-løsninger*, based on WCAG) may apply. The site has not been tested against WCAG; this needs review.

## 7. Licenses

### This project

The repository has **no license file**. Without a license, copyright law gives others no right to copy, change or
distribute the code. The contributors should choose a license together (for example MIT or Apache-2.0) and add a
`LICENSE` file.

### Third-party software

Main dependencies and the licenses they declare. This list is not complete and should be checked with a license
scanner before the software is distributed.

| Component | Used in | License |
|---|---|---|
| React, React DOM, React Router, react-markdown, remark-math, rehype-katex, KaTeX, Vite, Tailwind CSS | `frontend/package.json` | MIT (from `node_modules/*/package.json`) |
| lucide-react | `frontend/package.json` | ISC |
| Inter font | Google Fonts (`frontend/src/styles.css`) | SIL Open Font License 1.1 |
| Spring Boot, Spring Security, springdoc-openapi, jjwt | `backend/pom.xml` | Apache-2.0 |
| PostgreSQL JDBC driver | `backend/pom.xml` | BSD-2-Clause |
| FastAPI, pydantic, python-docx | `homework-collector/requirements.txt`, `ai-content/load/requirements.txt` | MIT |
| httpx, uvicorn, pypdf | same files | BSD-3-Clause |
| psycopg 3 | database and loading scripts (`homework-collector/homework/to_db.py`, `ai-content/load`, `database/migrate.py`) | LGPL-3.0 — the obligations should be checked if the scripts are distributed |

### Content

- Prompts in `ai-content/prompts` were written by the team.
- Study materials loaded into the database (`ai-content/materials/`) must be ones the team has the right to use.
  Text from textbooks or platforms should not be added without permission.
