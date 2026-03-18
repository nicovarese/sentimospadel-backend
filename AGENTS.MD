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
- authenticated onboarding survey
- clubs
- social matches
- tournaments
- rankings and ELO
- reservations and payments in later phases

This backend must be developed as a **modular monolith**, not as microservices.

---

## Current Goal
The current priority is no longer backend bootstrap.

The current priorities are:
1. keep auth, JWT, onboarding, and social matches stable
2. make player profile lifecycle explicit and maintainable
3. complete the social match lifecycle before implementing ranking/ELO
4. continue evolving the backend in small, reviewable vertical slices
5. avoid product assumptions that are not yet defined

At this stage, focus on:
- auth stability
- JWT-authenticated flows
- onboarding stability
- player profile lifecycle
- social match lifecycle
- clean persistence and migrations
- testable business logic
- documentation that reflects current product behavior

Do not jump into advanced ranking or tournament logic until match results are stable.

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
- `onboarding`
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
- `com.sentimospadel.backend.player.controller`
- `com.sentimospadel.backend.player.service`
- `com.sentimospadel.backend.player.entity`

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
- `initial_survey_submissions`

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
- answer option
- Uruguay category
- club verification status

Persist enums in a readable way, preferably as strings.

---

## API Rules

### Style
Use REST APIs.

### Conventions
- use nouns, not verbs, in route naming when possible
- use plural resource names when appropriate
- keep routes predictable and consistent

Examples:
- `/api/health`
- `/api/auth/register`
- `/api/auth/login`
- `/api/auth/me`
- `/api/users`
- `/api/players`
- `/api/clubs`
- `/api/onboarding/initial-survey`
- `/api/matches`

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

### Current security state
Security is already partially implemented.

Current backend behavior includes:
- email/password registration
- real login using Spring Security authentication
- JWT access token support
- `/api/auth/me`
- onboarding endpoints protected with JWT
- selected endpoints still intentionally left open in bootstrap mode

### Current phase
For now:
- keep register/login/JWT stable
- keep `/api/auth/me` stable
- protect authenticated user flows where appropriate
- do not add refresh tokens yet unless explicitly requested
- do not add OAuth login yet unless explicitly requested
- do not lock down the entire app at once without clear product need

### Future expectation
The project may later support:
- refresh tokens
- Google login
- Apple login
- stricter authorization rules by endpoint/role
- club-admin-specific permissions

But those are not the current implementation target unless requested.

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
2. service tests for application rules
3. controller tests for endpoint behavior
4. integration tests for repositories and DB-backed flows
5. minimal smoke tests for app startup and health endpoint

Do not write fake or useless tests only to increase numbers.

If adding tests:
- keep them readable
- keep them focused
- prefer meaningful coverage over volume

When product rules are mathematically or logically precise, prefer isolated unit tests for those rules.

---

## Configuration Rules

Use:
- `application.yml`
- profile-specific files when needed:
  - `application-dev.yml`
  - `application-prod.yml`

Do not hardcode secrets unsafely.

Use environment variables or external configuration for:
- database credentials
- JWT secrets
- API keys
- storage credentials
- payment provider credentials

Local fallback values are acceptable only for development and must be clearly temporary/documented.

---

## Current Domain Priorities

### Priority 1
Auth and player identity foundation:
- register
- login
- JWT
- current user endpoint
- explicit player profile lifecycle

### Priority 2
Onboarding and player initialization:
- onboarding survey
- persisted answers and result
- initial rating
- Uruguay category mapping
- club verification flags/status
- keep onboarding separate from register

### Priority 3
Social match lifecycle:
- create
- join
- leave
- cancel
- result submission
- match history and read models

### Priority 4
Ranking / ELO:
- only after match results are stable
- do not implement ranking logic on top of incomplete match modeling

### Priority 5
Tournaments:
- only after match and ranking foundations are stable

### Priority 6
Later modules:
- reservations
- payments
- notifications
- admin/reporting

Do not skip directly to later modules unless explicitly asked.

---

## Current Data Model Guidance

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
Fields currently expected to support:
- id
- user
- fullName
- photoUrl
- preferredSide
- declaredLevel (only if product rules later require it)
- city
- bio
- currentElo
- provisional
- matchesPlayed
- surveyCompleted
- surveyCompletedAt
- initialRating
- estimatedCategory
- requiresClubVerification
- clubVerificationStatus
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

### InitialSurveySubmission
Fields should remain conceptually close to:
- id
- playerProfile
- surveyVersion
- q1..q10
- weightedScore
- normalizedScore
- initialRating
- estimatedCategory
- requiresClubVerification
- createdAt
- updatedAt

### Match
Fields should remain conceptually close to:
- id
- createdBy
- status
- scheduledAt
- club
- locationText
- notes
- maxPlayers
- createdAt
- updatedAt

### MatchParticipant
Fields should remain conceptually close to:
- id
- match
- playerProfile
- joinedAt

These may evolve, but should remain aligned with the MVP direction.

---

## Product-Aligned Backend Rules

These rules are important and should be respected when implementing backend logic:

### Onboarding
- onboarding is a post-registration authenticated step
- onboarding must remain separate from register unless product explicitly changes that decision
- raw survey answers should be persisted
- calculated onboarding result should be persisted
- current onboarding result may be mirrored onto `player_profiles`

### Rating and category visibility
- initial rating is visible
- estimated category is visible
- if category is `PRIMERA` or `SEGUNDA`, verification is required
- verification requirement does **not** hide rating or category
- represent this with:
  - `requiresClubVerification`
  - `clubVerificationStatus`

### Club verification
- actual club verification workflow is not implemented yet
- prepare the model/state for it
- do not invent full verification processes unless explicitly requested

### Ranking / ELO
- do not implement ranking/ELO engine before match results are stable
- initial onboarding rating is not automatically the same thing as the future ranking engine unless explicitly defined
- avoid embedding unresolved ranking assumptions into onboarding or auth flows

### Player profile lifecycle
- the project should move toward an explicit player profile lifecycle
- temporary lazy creation is acceptable only when clearly documented and justified
- prefer explicit player-owned flows once product/API shape is clear

### Match lifecycle
- create/join/leave are only the first step
- ranking should be built on top of real match completion and results, not only open match records

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
- implement ranking before match result foundations are stable
- embed unresolved product assumptions into register/auth flows
- add frontend code in this repository

---

## Expected Current Tasks for an Agent
When working on this repository now, the most useful tasks are things like:
- keep auth and JWT flows stable
- evolve onboarding safely when product rules are clarified
- make player profile lifecycle explicit
- extend social match lifecycle in small steps
- prepare ranking on top of real match results, not before
- update docs when product rules change
- keep migrations small and reviewable
- add meaningful tests around business rules
- preserve consistency between docs and actual implementation

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
- docs stay aligned when product/backend behavior changes materially
- the result is understandable by a human developer continuing the project

---

## If Unsure
If requirements are ambiguous:
- preserve the modular monolith structure
- choose the simpler solution
- avoid overengineering
- favor future maintainability
- stay aligned with the MVP scope
- do not invent product rules that have not been decided yet