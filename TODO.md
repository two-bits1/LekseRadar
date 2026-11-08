# What is left (main-test-4.0)

Run everything locally: `cp .env.example .env && docker compose up --build` (see [README.md](README.md#getting-started)).
Test results: [README-TEST.md](README-TEST.md), change log: [CHANGES.md](CHANGES.md).

## Urgent

- [ ] Revoke the Gmail app password that was committed to the git history (https://myaccount.google.com/apppasswords).
  It is no longer in the files, but it stays in the history.

## Local setup (optional)

- [ ] `OPENAI_API_KEY` in `.env` for the AI tutor, quizzes and AI flashcards (free key: https://console.groq.com/keys).
- [ ] `MAIL_USERNAME`, `MAIL_PASSWORD` in `.env` to send real emails; without them the sign-up code is in
  `docker compose logs backend`.
- [ ] `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` for Google Classroom; redirect URI
  `http://localhost:5173/settings/platforms` in Google Cloud.
- [ ] `TEAMS_CLIENT_ID` for the Teams device-code sign-in.

## Check after `docker compose up`

- [ ] Sign up → code from the log → logged in.
- [ ] Platforms: Canvas (token), Google Classroom → homework shows up, *Refresh* adds no duplicates;
  disconnecting removes the homework, chats, quizzes and flashcards made for it.
- [ ] AI: `python3 ai-content/check/check_ai.py --api http://localhost:8080/api/v1 --email … --password …`.

## Nice to have

- [ ] Study materials in `ai-content/materials/<Subject>/<Topic>/` (loaded by the `content` service on start).
- [ ] `GET /achievements` for the profile (`frontend/docs/backend-changes.md`, section 3).
- [ ] Split the frontend bundle (`import()` for pages).
- [ ] Account deletion and data export (see [PRIVACY.md](PRIVACY.md), [COMPLIANCE.md](COMPLIANCE.md)).
