# Security Policy

## Reporting a vulnerability

Report security problems **privately** through GitHub: repository → **Security** tab → **Report a vulnerability**.

- Do **not** report vulnerabilities in a public issue, pull request or discussion.
- Describe what is affected (frontend, backend, homework collector, database), how to reproduce it and what an
  attacker could do with it. Do not include real personal data or real platform tokens in the report.

> For maintainers: the button is shown only when private vulnerability reporting is enabled
> (**Settings → Code security → Private vulnerability reporting**). GitHub offers it only for public repositories.

## Supported versions

Only the latest version of the main branch receives security fixes.

## What is implemented

Paths to backend Java files are relative to `backend/src/main/java/com/lekseradar/`.

### Authentication and accounts

| Measure | Where |
|---|---|
| Passwords hashed with BCrypt, 8–100 characters | `config/SecurityConfig.java`, `dto/auth/RegisterRequestDTO.java` |
| Stateless JWT (HMAC, secret of at least 32 bytes), valid 24 hours by default (`JWT_TTL_HOURS`) | `security/JwtService.java`, `backend/src/main/resources/application.yml` |
| Every API address except sign-up/login, health and (if enabled) API docs requires the token | `config/SecurityConfig.java` |
| 5 wrong passwords for one email in 15 minutes → login for it is blocked until the window ends; the response time does not reveal whether an account exists | `service/AuthService.java` (`MAX_FAILED_LOGINS`) |
| Email verification: 6-digit code, 15 minutes, 5 tries, only its SHA-256 kept in memory; at most 3 codes per email and 10 per IP in 15 minutes | `service/AuthService.java` |
| Account deletion and data export need a logged-in user; deletion also needs the current password | `controller/UserController.java`, `service/AccountService.java` |

### Secrets

| Measure | Where |
|---|---|
| No keys or passwords in the repository: Docker Compose generates random ones on first start into the `keys` volume, or takes them from `.env` | `scripts/keys.sh`, `docker-compose.yml` |
| The backend and the collector refuse to start with any value that was ever a public default in this repository (only SHA-256 hashes are kept) | `security/InsecureSecrets.java`, `homework-collector/homework/server.py` |
| Secrets only in `.env` files, which are in `.gitignore` | `.gitignore`, `backend/.gitignore` |

### Data protection

| Measure | Where |
|---|---|
| Platform access (tokens, cookies) encrypted with AES-256-GCM, random nonce per value; key `CREDENTIALS_KEY` required | `security/CredentialsCipher.java` |
| Data export contains no password hash and no platform credentials | `repository/UserRepository.java` (`exportJson`) |
| Disconnecting a platform deletes its access and data; for Google the token is revoked | `service/PlatformService.java`, `service/google/GoogleOAuthClient.java` |
| Accounts without a login for 24 months are deleted with all data | `service/RetentionScheduler.java` |

### Abuse and network

| Measure | Where |
|---|---|
| Per-student limits: 30 AI requests and 120 quiz answers per 10 minutes, 1 manual sync per minute, 10 platform connection attempts per 10 minutes, 3 test emails per hour, 5 data exports per hour | `config/AiRateLimitConfig.java`, `security/RateLimiter.java`, services |
| SSRF protection: the server calls only `https` hosts on allowed platform domains (`CANVAS_DOMAINS`, `ITSLEARNING_DOMAINS`, `INSCHOOL_DOMAINS`, plus `CANVAS_HOSTS`), no ports or user info | `service/PlatformService.java` |
| Google sign-in: exact redirect address only (no open redirect), HMAC-signed `state` bound to the user, valid 10 minutes, read-only scopes | `service/PlatformService.java`, `service/google/OAuthStates.java`, `service/google/GoogleOAuthClient.java` |
| Collector reachable only with the `X-Internal-Key` header (constant-time compare), only inside the Compose network / localhost; stores nothing, does not log bodies | `homework-collector/homework/server.py`, `docker-compose.yml` |
| Platform errors return 422, not 401, so a student is not logged out by a rejected platform token | `service/PlatformService.java`, `service/collector/CollectorClient.java` |
| API docs (Swagger UI, `/v3/api-docs`) off unless `API_DOCS_ENABLED=true` | `backend/src/main/resources/application.yml` |
| `/actuator/health` shows only UP/DOWN | `backend/src/main/resources/application.yml` |
| Website headers: Content-Security-Policy (`script-src 'self'`, `frame-ancestors 'none'`, `object-src 'none'`), X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy, COOP, HSTS | `frontend/vercel.json` |
| No third-party scripts or fonts on the site | `frontend/index.html`, `frontend/src/main.tsx` |
| All local ports bind to 127.0.0.1 | `docker-compose.yml` |

## Before going live (operator checklist)

1. Serve the site and the API only over HTTPS; keep the database and the collector on a private network.
2. Set your own `JWT_SECRET`, `CREDENTIALS_KEY`, `COLLECTOR_KEY` and database password (or let Compose generate
   them) and keep a secure backup of `CREDENTIALS_KEY` — without it all platform connections must be redone.
3. Set `CORS_ORIGINS` to the real site address only; keep `API_DOCS_ENABLED` off.
4. Enable encrypted database backups and decide how long backups and server logs are kept.
5. Sign data processing agreements with the hosting, AI and email providers (see [COMPLIANCE.md](COMPLIANCE.md)).
6. Enable Dependabot or another dependency scanner, and private vulnerability reporting on GitHub.

## Known limitations

1. **No two-factor login.**
2. **Tokens.** The JWT is kept in the browser's `localStorage`; there is no server-side logout, and changing the
   password does not end existing sessions (they expire after `JWT_TTL_HOURS`).
3. **One backend instance.** Rate limits and pending sign-ups live in memory; they reset on restart and are not
   shared between instances.
4. **Unofficial platform access.** itslearning uses the OAuth client of its mobile app; Visma InSchool is read with
   session cookies. Both can break or conflict with those platforms' terms.
5. **CI** runs only the frontend tests (`.github/workflows/frontend.yml`).
6. **Leaked credential.** An email app password was once committed to this repository. It must be revoked by its
   owner; rewriting git history does not remove copies that others already fetched.
