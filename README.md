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

## Project Structure
```text
src/main/java/com/sentimospadel/backend
  config/
  shared/
  auth/
  user/
  player/
  club/

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
