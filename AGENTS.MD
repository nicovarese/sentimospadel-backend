# AGENTS.md

## Project Overview
This repository contains the backend for the Sentimos Padel application.

The backend is built with:
- Java 21
- Spring Boot
- Maven
- PostgreSQL
- Flyway

This project is intended to support:
- player registration and login
- player profiles
- clubs
- social matches
- tournaments
- rankings and ELO
- reservations and payments in later phases

This backend must be developed as a **modular monolith**, not as microservices.

---

## Current Goal
The current priority is to bootstrap the backend foundation correctly.

At this stage, focus on:
1. clean Spring Boot project structure
2. PostgreSQL integration
3. Flyway migrations
4. modular package organization
5. base entities and shared infrastructure
6. first domain modules:
   - auth
   - user
   - player
   - club
7. health endpoint
8. clean error handling
9. no business overengineering yet

Do not jump into advanced features until the foundation is stable.

---

## Architecture Rules

### High-level architecture
Use a **modular monolith**.

The codebase must be organized by business module, not by global technical layers.

Preferred package structure:

- `config`
- `shared`
- `auth`
- `user`
- `player`
- `club`
- `match`
- `tournament`
- `ranking`
- `reservation`
- `payment`
- `notification`

Each business module should contain its own internal layers when needed, for example:
- `controller`
- `service`
- `repository`
- `entity`
- `dto`
- `mapper`
- `validator`

Example:
`com.sentimospadel.backend.player.controller`
`com.sentimospadel.backend.player.service`
`com.sentimospadel.backend.player.entity`

Do not create a flat architecture where all controllers, services, repositories, and entities live together globally.

---

## Package Root
Use this package root:

`com.sentimospadel.backend`

All Java code should follow that base package.

---

## Database Rules

### Database engine
Use PostgreSQL.

### Migrations
Use **Flyway only** for schema evolution.

Rules:
- all schema changes must go through Flyway migration files
- never rely on Hibernate to create or update schema automatically
- never use `ddl-auto=update`
- prefer `ddl-auto=validate` or `none`

### Naming conventions
Database naming must use `snake_case`.

Examples:
- `player_profiles`
- `created_at`
- `current_elo`

Java naming must use `camelCase`.

Examples:
- `playerProfile`
- `createdAt`
- `currentElo`

### IDs
For the MVP, use numeric IDs in PostgreSQL:
- `BIGSERIAL` in database
- `Long` in Java

Do not introduce UUIDs unless explicitly requested later.

---

## Entity Rules

### Entities and DTOs
Do not expose JPA entities directly in API responses.

Always use DTOs for:
- request bodies
- response bodies

Entities are persistence models, not API contracts.

### Shared base entity
Prefer a shared base entity or equivalent shared logic for common audit fields when appropriate.

Typical shared fields:
- `createdAt`
- `updatedAt`

Do not add unnecessary inheritance if it complicates the model.

### Enums
Use enums for stable domain concepts such as:
- user role
- user status
- match status
- tournament status

Persist enums in a readable way, preferably as strings.

---

## API Rules

### Style
Use REST APIs.

### Conventions
- use nouns, not verbs, in route naming when possible
- use plural resource names
- keep routes predictable and consistent

Examples:
- `/api/health`
- `/api/users`
- `/api/players`
- `/api/clubs`
- `/api/matches`
- `/api/tournaments`

### Response design
Keep responses simple and explicit.

Avoid returning raw maps unless there is a strong reason.

Prefer structured DTOs.

### Error handling
Implement centralized error handling.

Use:
- meaningful HTTP status codes
- structured error responses
- validation messages that are readable and useful

Do not leak internal stack traces in API responses.

---

## Security Rules

Security will be implemented in phases.

### Current phase
For now:
- prepare Spring Security dependency and structure
- do not overbuild the auth system yet
- do not implement JWT unless explicitly requested
- do not add OAuth login yet unless explicitly requested

### Future expectation
The project will later support:
- email/password login
- JWT
- refresh tokens
- Google login
- Apple login

But those are not the first implementation target unless requested.

---

## Coding Rules

### General
- write clean and readable Java
- prefer clarity over cleverness
- avoid premature abstractions
- avoid unnecessary generics and patterns
- avoid large God classes

### Lombok
Lombok is allowed, but use it carefully.
Prefer:
- `@Getter`
- `@Setter`
- `@Builder` when it improves clarity
- `@NoArgsConstructor`
- `@AllArgsConstructor`

Avoid excessive Lombok usage that hides important logic.

### Constructor injection
Prefer constructor injection over field injection.

Do not use `@Autowired` on fields.

### Null handling
Avoid returning `null` when possible.
Prefer explicit handling and meaningful exceptions.

### Mapping
Use explicit mapping between entities and DTOs.
MapStruct may be introduced later, but do not add unnecessary tooling unless requested.

---

## Testing Rules

At minimum, keep the code testable.

Preferred testing progression:
1. unit tests for core business logic
2. integration tests for repositories and controllers
3. minimal smoke tests for app startup and health endpoint

Do not write fake or useless tests only to increase numbers.

If adding tests:
- keep them readable
- keep them focused
- prefer meaningful coverage over volume

---

## Configuration Rules

Use:
- `application.yml`
- profile-specific files when needed:
  - `application-dev.yml`
  - `application-prod.yml`

Do not hardcode secrets.

Use environment variables or external configuration for:
- database credentials
- JWT secrets in future
- API keys
- storage credentials
- payment provider credentials

---

## Initial Domain Priorities

### Priority 1
Bootstrap backend foundation:
- Spring Boot setup
- Maven setup
- PostgreSQL config
- Flyway config
- package structure
- base error handling
- health endpoint

### Priority 2
Initial domain model:
- `User`
- `PlayerProfile`
- `Club`

### Priority 3
Basic auth flow:
- user registration
- login skeleton
- current user endpoint

### Priority 4
Core MVP gameplay features:
- social matches
- match participants
- results
- tournaments
- ELO history
- rankings

### Priority 5
Later modules:
- reservations
- payments
- notifications
- admin/reporting

Do not skip directly to later modules unless explicitly asked.

---

## Initial Data Model Guidance

The initial core entities should be close to:

### User
Fields expected:
- id
- email
- passwordHash
- role
- status
- createdAt
- updatedAt

### PlayerProfile
Fields expected:
- id
- user
- fullName
- photoUrl
- preferredSide
- declaredLevel
- city
- bio
- currentElo
- provisional
- matchesPlayed
- createdAt
- updatedAt

### Club
Fields expected:
- id
- name
- city
- address
- description
- isIntegrated
- createdAt
- updatedAt

These may evolve, but should remain aligned with the MVP direction.

---

## What To Avoid
Do not:
- convert this into microservices
- add Kafka, Redis, queues, or distributed infrastructure unless explicitly needed
- add complex DDD ceremony without real benefit
- expose entities directly
- use `ddl-auto=update`
- create huge service classes with many unrelated responsibilities
- create generic base repositories/services everywhere just for abstraction
- introduce unnecessary patterns too early
- build tournament logic before the user/player/club foundation is ready
- add frontend code in this repository

---

## Expected First Tasks for an Agent
When working on this repository, the most useful initial tasks are:

1. review and improve `pom.xml`
2. configure PostgreSQL and `application.yml`
3. configure Flyway
4. create first migration
5. create package structure
6. add shared base classes
7. create `User`, `PlayerProfile`, and `Club`
8. add repository layer
9. add health controller
10. add global exception handling

When completing a task:
- explain what files were created or modified
- explain why the change was made
- keep changes small and reviewable
- do not perform unnecessary unrelated refactors

---

## Definition of Done
A task is considered complete only if:
- the code builds
- the project structure remains consistent
- migrations are valid
- changes align with this document
- no unnecessary unrelated files were changed
- the result is understandable by a human developer continuing the project

---

## If Unsure
If requirements are ambiguous:
- preserve the modular monolith structure
- choose the simpler solution
- avoid overengineering
- favor future maintainability
- stay aligned with the MVP scope