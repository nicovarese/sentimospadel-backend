# Sentimos Padel Backend

Backend for the Sentimos Padel application. The project is being built as a modular monolith with a stable foundation first: PostgreSQL, Flyway, shared infrastructure, and the first domain slices for auth, users, players, and clubs.

`AGENTS.md` is the operational rules file for repository work. This README is the practical project guide.

## Stack
- Java 21
- Spring Boot 4
- Maven
- PostgreSQL
- Flyway
- Spring Data JPA
- Spring Security

## Local Run
1. Ensure PostgreSQL is running locally.
2. Ensure the local database and user exist:
   - database: `sentimospadel`
   - schema: `public`
   - user: `sentimospadel_user`
3. Default local connection assumptions from `src/main/resources/application.yml`:
   - `DB_URL=jdbc:postgresql://localhost:5432/sentimospadel?currentSchema=public`
   - `DB_USERNAME=sentimospadel_user`
   - `DB_PASSWORD=sentimospadel_local_password`
   - `JWT_SECRET=change-this-local-jwt-secret-change-this-local-jwt-secret`
   - `JWT_EXPIRATION_MS=3600000`
4. Start the backend:
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

If `8080` is free and you prefer the default port:
```powershell
.\mvnw.cmd spring-boot:run
```

## Local PostgreSQL Assumptions
- The backend uses its own local database: `sentimospadel`
- The backend uses the default schema: `public`
- Flyway owns schema creation and updates
- Hibernate validates the schema only
- No tables should be created manually outside Flyway

## Current Modules
- `config`
- `shared`
- `auth`
- `user`
- `player`
- `club`

Future modules such as match, tournament, ranking, reservation, payment, and notification are intentionally not implemented yet.

## Available Endpoints
- `GET /api/health`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/onboarding/initial-survey`
- `GET /api/onboarding/initial-survey`
- `GET /api/users`
- `GET /api/users/{id}`
- `GET /api/players`
- `GET /api/players/{id}`
- `GET /api/clubs`
- `GET /api/clubs/{id}`
- `POST /api/clubs`

## Port Notes
- The backend defaults to port `8080`
- Another local app may already be using `8080`
- For local isolation, run this backend on `8081` when needed

## Authentication
- `POST /api/auth/register` creates a user with a BCrypt-hashed password
- `POST /api/auth/login` verifies real credentials and returns a JWT access token
- `GET /api/auth/me` requires a Bearer token and returns the authenticated user
- The onboarding survey endpoints are also JWT-protected and are intentionally separate from registration

Example login request:
```json
{
  "email": "player@example.com",
  "password": "secret123"
}
```

Example login response shape:
```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "id": 1,
  "email": "player@example.com",
  "role": "PLAYER",
  "status": "ACTIVE"
}
```

Example authenticated request:
```powershell
curl http://localhost:8081/api/auth/me `
  -H "Authorization: Bearer <access-token>"
```

The default JWT secret is only a local development fallback. Override `JWT_SECRET` outside local development.

## Initial Onboarding Survey
- `POST /api/onboarding/initial-survey` accepts the authenticated player onboarding survey answers `q1` through `q10`
- Each answer must be one of `A`, `B`, `C`, `D`, or `E`
- The backend stores both the raw survey answers and the calculated onboarding result in a dedicated `initial_survey_submissions` table
- The current onboarding result is also mirrored onto `player_profiles` so the current player profile has `surveyCompleted`, `surveyCompletedAt`, `initialRating`, `estimatedCategory`, `requiresClubVerification`, and `clubVerificationStatus`
- Re-submitting the initial survey is currently blocked after the first successful submission
- `GET /api/onboarding/initial-survey` returns the authenticated user's saved onboarding result and returns `404` when no survey has been submitted yet

Example onboarding request:
```json
{
  "q1": "B",
  "q2": "C",
  "q3": "D",
  "q4": "A",
  "q5": "B",
  "q6": "C",
  "q7": "D",
  "q8": "A",
  "q9": "B",
  "q10": "C"
}
```

The onboarding calculation currently does only the initial survey scoring flow:
- answer mapping `A=0`, `B=1`, `C=2`, `D=3`, `E=4`
- weighted scoring and normalized score calculation
- the anti-inflation rule on `q9` when `q6` is below `C`
- the rating gates for `Primera` and `Segunda`
- Uruguay category mapping from the resulting rating

The rating and estimated category are always visible.

If the estimated category is `PRIMERA` or `SEGUNDA`:
- `requiresClubVerification=true`
- `clubVerificationStatus=PENDING`
- the rating and category still remain visible

For `TERCERA` through `SEPTIMA`:
- `requiresClubVerification=false`
- `clubVerificationStatus=NOT_REQUIRED`

The actual club verification workflow, ranking engine, match logic, and tournament logic are still pending.

## Project Structure
```text
src/main/java/com/sentimospadel/backend
  config/
  shared/
  auth/
  user/
  player/
  club/
  onboarding/

src/main/resources
  application.yml
  db/migration/
```

## Current State
- Flyway migration `V1__init_core_schema.sql` is applied at runtime
- Core entities exist for `User`, `PlayerProfile`, and `Club`
- Club creation is available
- User and player reads are available
- Registration now stores hashed passwords with BCrypt
- Login now authenticates real credentials through Spring Security
- Login now returns a JWT access token
- `/api/auth/me` resolves the authenticated user from the Bearer token
- The authenticated onboarding survey flow now persists raw answers plus calculated initial rating and Uruguay category
- Primera and Segunda onboarding results are persisted as visible results with `PENDING` club verification
- Refresh tokens are intentionally not implemented yet
