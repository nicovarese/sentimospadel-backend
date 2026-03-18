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
- `match`
- `rating`

Future modules such as tournament, reservation, payment, and notification are intentionally not implemented yet.

## Available Endpoints
- `GET /api/health`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/onboarding/initial-survey`
- `GET /api/onboarding/initial-survey`
- `POST /api/matches`
- `POST /api/matches/{id}/join`
- `POST /api/matches/{id}/leave`
- `POST /api/matches/{id}/cancel`
- `POST /api/matches/{id}/teams`
- `POST /api/matches/{id}/result`
- `POST /api/matches/{id}/result/confirm`
- `POST /api/matches/{id}/result/reject`
- `GET /api/matches`
- `GET /api/matches/{id}`
- `GET /api/matches/{id}/result`
- `GET /api/rankings`
- `GET /api/players/me/rating-history`
- `GET /api/players/me/matches`
- `GET /api/players/{id}/rating-history`
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
- Match creation, join, and leave are JWT-protected
- Match cancellation and result submission are JWT-protected
- Match team assignment, result confirmation, and result rejection are also JWT-protected
- `GET /api/players/me/rating-history` is JWT-protected
- `GET /api/players/me/matches` is JWT-protected
- Match list/detail endpoints are readable without JWT in this first slice
- `GET /api/players/{id}/rating-history` is public in this slice

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

The official player rating scale is `1.00..7.00`.
- onboarding initial rating uses this scale
- ongoing match-based rating progression uses this same scale
- `player_profiles.current_rating` is the active live rating on this scale

The rating and estimated category are always visible.

If the estimated category is `PRIMERA` or `SEGUNDA`:
- `requiresClubVerification=true`
- `clubVerificationStatus=PENDING`
- the rating and category still remain visible

For `TERCERA` through `SEPTIMA`:
- `requiresClubVerification=false`
- `clubVerificationStatus=NOT_REQUIRED`

The actual club verification workflow, ranking engine, and tournament logic are still pending.

## Initial Social Matches
- `POST /api/matches` creates a social match for the authenticated user
- `POST /api/matches/{id}/join` joins the authenticated player to a match
- `POST /api/matches/{id}/leave` removes the authenticated player from a match they already joined
- `POST /api/matches/{id}/cancel` cancels a match
- `POST /api/matches/{id}/teams` assigns participants to `TEAM_ONE` and `TEAM_TWO`
- `POST /api/matches/{id}/result` submits a result in pending-confirmation state
- `POST /api/matches/{id}/result/confirm` confirms a pending result
- `POST /api/matches/{id}/result/reject` rejects a pending result and returns the match to playable state
- `GET /api/matches` lists saved matches
- `GET /api/matches/{id}` returns match detail
- `GET /api/matches/{id}/result` returns the saved result for a completed match
- This is the first thin social-match slice only
- Ranking logic, tournament logic, reservations, and payments are still out of scope

Current match rules:
- The creator is automatically added as the first participant
- `maxPlayers` is fixed at `4`
- A player cannot join the same match twice
- A player cannot join a full or cancelled match
- A player can leave only if they are currently a participant
- A match becomes `FULL` at 4 participants
- A match returns to `OPEN` if participants drop below 4, unless it is cancelled in a future flow
- Only the creator can cancel a match
- Team assignment is creator-only in this first version
- Results require explicit `TEAM_ONE` / `TEAM_TWO` assignment with a valid `2 vs 2` distribution
- A completed or pending-result match cannot be cancelled or modified
- Result submission is only allowed for non-cancelled matches with all 4 players joined and valid teams assigned
- Submitting a result moves the match to `RESULT_PENDING`
- A player from the opposite team must confirm the submitted result
- A player from the opposite team can reject a pending result, optionally with a short rejection reason
- Rejecting a result moves the result to `REJECTED`, returns the match from `RESULT_PENDING` to `FULL`, and keeps team assignments intact
- After rejection, a new result can be submitted again and the same pending confirmation flow applies
- Only after confirmation does the match become `COMPLETED`
- Only confirmed results trigger rating updates
- Pending and rejected results never update rating
- Each confirmed result can update ratings only once

## Rankings
- `GET /api/rankings` returns players ordered by current rating descending
- Each entry includes basic player info, current rating, current Uruguay category, and rated match count
- Match-based rating progression only applies when a social match result reaches `CONFIRMED`
- The rating history is persisted in `player_rating_history`
- `GET /api/players/me/rating-history` returns the authenticated player's rating history ordered from newest to oldest
- `GET /api/players/{id}/rating-history` returns the same history for a public player profile
- Rating history entries include the history row id, match id, old rating, delta, new rating, creation timestamp, and a compact match/result summary when available
- Onboarding initial rating and ongoing progression share the same official `1.00..7.00` scale
- Tournament ranking is still out of scope

## Player Match History
- `GET /api/players/me/matches` returns the authenticated player's match history ordered by `scheduledAt` descending
- Each entry includes the match summary, participant list, team assignment if present, result summary if present, and whether the authenticated player won when there is a confirmed result
- This gives the frontend a direct navigation path between player profile, match history, and rating history
- Status/scope filters are intentionally not implemented yet in this first read slice

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
  match/

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
- The first social match module now supports create, join, leave, list, and detail
- The match lifecycle now also supports cancellation and result submission
- Matches now support explicit team assignment and result confirmation
- Matches now support result rejection and controlled resubmission after rejection
- The first rating slice now updates player ratings from confirmed social match results on the official `1.00..7.00` scale
- Rating history now exists, rating history read endpoints are available, and `GET /api/rankings` is available
- Player-facing match history is now available through `GET /api/players/me/matches`
- DB-backed integration tests now cover Flyway + confirmed result -> rating update -> history persistence
- Tournament ranking logic is intentionally not implemented yet
- Refresh tokens are intentionally not implemented yet
