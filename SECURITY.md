# Security Policy

## Reporting a vulnerability

Please report security problems **privately** through GitHub:
open the repository on GitHub → **Security** tab → **Report a vulnerability**.

- Do **not** report vulnerabilities in a public issue, pull request or discussion.
- Describe what is affected (frontend, backend, homework collector, database), how to reproduce it and what an
  attacker could do with it. Do not include real personal data or real platform tokens in the report.
- There is no separate security email address for this project.

> For maintainers: the **Report a vulnerability** button is only shown when private vulnerability reporting is
> enabled (repository **Settings → Code security → Private vulnerability reporting**). GitHub offers this only for
> public repositories; while the repository is private, only people with access to it can see it and report.

## Project status and supported versions

LekseRadar is a **prototype built during the Oslo Sprint hackathon (6–8 November 2026)**.

- The code has **not had a security audit** or a penetration test.
- **No versions are supported.** There are no releases, no security updates and no response-time commitment.
- Do not run it with real student data without the work listed under
  [Known limitations](#known-limitations-and-recommendations-before-production). See also [PRIVACY.md](PRIVACY.md)
  and [COMPLIANCE.md](COMPLIANCE.md).

## What is already implemented

Every item below was checked in the code. Paths to backend Java files are relative to
`backend/src/main/java/com/lekseradar/`.

| Measure | How it works | Where |
|---|---|---|
| Encryption of platform access | Tokens and cookies for school platforms are stored only encrypted (AES-256-GCM, a random 12-byte nonce per value). The key is `CREDENTIALS_KEY` (base64, exactly 32 bytes); the backend refuses to start without it. | `security/CredentialsCipher.java`, `app.credentials.key` in `backend/src/main/resources/application.yml` |
| Login with JWT and Spring Security | Stateless sessions, `Authorization: Bearer <token>` on every request except `/api/v1/auth/**`, `/actuator/health` and the API docs. The JWT secret must be at least 32 bytes. Passwords are hashed with BCrypt and must be 8–100 characters. | `config/SecurityConfig.java`, `security/JwtService.java`, `security/JwtAuthenticationFilter.java`, `dto/auth/RegisterRequestDTO.java` |
| Lockout after failed logins | After `MAX_FAILED_LOGINS = 5` wrong passwords for one email within 15 minutes, login for that email is closed until the window ends. A missing email is checked against a dummy hash, so the response time does not reveal whether an account exists. | `service/AuthService.java` |
| Email verification on sign-up | A 6-digit code, valid for 15 minutes, at most 5 tries. Only a SHA-256 hash of the code is kept, in memory. At most 3 codes per email and 10 per IP address in 15 minutes. | `service/AuthService.java` |
| Per-user limits on AI requests | Chat messages, new quizzes and AI flashcards: 30 per 10 minutes. Quiz answers (text answers are checked by the AI): 120 per 10 minutes. | `config/AiRateLimitConfig.java` |
| Per-user limits on sync and test email | Manual homework sync: once per minute. Test reminder email: 3 per hour. Platform connection attempts: 10 per 10 minutes. | `security/RateLimiter.java`, `service/HomeworkSyncService.java` (`SYNC_WINDOW`), `service/ReminderService.java` (`TEST_PER_HOUR`), `service/PlatformService.java` (`CONNECT_LIMIT`) |
| Host allow-lists for platforms (SSRF protection) | The server only calls `https` addresses without a port or user info, inside `CANVAS_DOMAINS` (`instructure.com`), `ITSLEARNING_DOMAINS` (`itslearning.com`) and `INSCHOOL_DOMAINS` (`inschool.visma.no`), plus hosts listed in `CANVAS_HOSTS`. Path and query are dropped. | `service/PlatformService.java` (`allowedUrl`, `hostOf`) |
| Open-redirect protection for Google sign-in | Google may only send the student back to an exact address: a site from `CORS_ORIGINS` + `/settings/platforms`. The `state` parameter is signed with HMAC-SHA256, bound to the user and valid for 10 minutes. | `service/PlatformService.java` (`classroomRedirect`), `service/google/OAuthStates.java` |
| Read-only Google scopes | Only `classroom.courses.readonly` and `classroom.coursework.me.readonly` are requested. The Google client secret stays on the backend. When a student disconnects Classroom, the token is revoked at Google. | `service/google/GoogleOAuthClient.java` |
| Internal-only collector | Every collector endpoint except `/health` requires the `X-Internal-Key` header (compared in constant time; the service will not start with a key shorter than 16 characters). In Docker Compose the collector listens only on `127.0.0.1:8090`; on Railway it runs as a private service reached at `*.railway.internal`. The collector stores nothing and does not log request bodies. | `homework-collector/homework/server.py`, `database/docker-compose.yml`, `docs/DEPLOY-RAILWAY.md` |
| Platform errors return 422, not 401 | A rejected platform token gives `422 Unprocessable Entity`, so the frontend does not log the student out. Collector errors are mapped to 502/503. | `service/PlatformService.java`, `service/collector/CollectorClient.java`, `controller/PlatformController.java` |
| Security headers on the frontend | `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Strict-Transport-Security: max-age=63072000`. | `frontend/vercel.json` |
| Secrets only in `.env` | Secrets are read from environment variables or a local `.env` file. `.env` files are in `.gitignore`; only `backend/.env.example` (without real values) is committed. | `backend/src/main/resources/application.yml`, `.gitignore`, `backend/.gitignore`, `frontend/.gitignore`, `homework-collector/.gitignore` |
| Health endpoint without details | `/actuator/health` shows only `UP`/`DOWN`. | `backend/src/main/resources/application.yml` (`show-details: never`) |
| Local database not exposed | The Docker Compose database listens only on `127.0.0.1:5432`. | `database/docker-compose.yml` |

## Known limitations and recommendations before production

1. **Change the development defaults.** They are fine on one laptop and unsafe anywhere else:
   - database password `lekseradar` (`database/docker-compose.yml`, default of `DB_PASSWORD` in
     `backend/src/main/resources/application.yml`);
   - collector key `local-dev-collector-key` (default of `COLLECTOR_KEY` in `application.yml` and
     `database/docker-compose.yml`) — generate one with `openssl rand -hex 32`;
   - the sample `JWT_SECRET` in `backend/.env.example` — generate a random secret of at least 32 bytes.
2. **No account deletion.** There is no endpoint to delete an account or export one's data
   (`controller/UserController.java` has only read, update, change password and test reminder). The database would
   delete a user's data in cascade (`ON DELETE CASCADE` in `database/migrations`), but nothing in the app triggers it.
3. **No two-factor authentication.**
4. **Unofficial platform access.** itslearning is connected through the OAuth client of the itslearning mobile app,
   not an official integration (`homework-collector/homework/sources/itslearning.py`, marked *Unofficial* in the UI).
   Visma InSchool is read through the web app's internal API with the student's session cookies
   (`homework-collector/README.md`). Both can break or be blocked at any time.
5. **Token handling in the browser.** The JWT is kept in `localStorage` (`frontend/src/api/client.ts`) and is valid
   for 72 hours by default (`JWT_TTL_HOURS`). There is no server-side logout or token revocation. Add a
   Content-Security-Policy and consider a shorter lifetime.
6. **Missing headers.** `frontend/vercel.json` has no `Content-Security-Policy` and no clickjacking protection
   (`X-Frame-Options` or `frame-ancestors`).
7. **In-memory state.** Rate limits and pending sign-ups live in the memory of one backend instance
   (`security/RateLimiter.java`, `service/AuthService.java`). They reset on restart and are not shared between
   instances.
8. **Public API docs.** Swagger UI (`/swagger-ui.html`) and `/v3/api-docs` are open without login
   (`config/SecurityConfig.java`). Consider turning them off in production.
9. **Dependencies.** Python requirements use minimum versions (`>=`) and frontend dependencies use ranges (`^`).
   There is no dependency scanning; CI covers only the frontend (`.github/workflows/frontend.yml`). Enable Dependabot
   or a similar tool and pin versions.
10. **No backup, monitoring or alerting** is configured in this repository.
11. **Leaked credential in git history.** An email app password was committed to this repository's git history.
    The owner of that account must revoke it. Deleting it from the current files does **not** remove it from the
    git history.
