# Progress Log

## 2026-03-17

### Summary
- Started from a backend that already had auth, authenticated onboarding, and the first social-match slice with create/join/leave
- Finished the missing social-match lifecycle pieces before moving to rating: cancel, result submit, explicit teams, result confirm, result reject, and controlled result resubmission
- Kept ranking out of scope until the match result workflow was stable enough to become a trusted source of truth
- Implemented the first real rating/ELO slice on the official `1.00..7.00` scale
- Added `GET /api/rankings`
- Added rating history persistence and idempotent protection so confirmed results update rating exactly once
- Added player rating history read endpoints and DB-backed integration coverage for the confirmed-result rating flow
- Added a player-facing match history endpoint so the frontend can move naturally between player profile, matches, and rating history

### Key Technical Decisions
- Used only `CONFIRMED` social match results as input for rating updates
- Did not update rating on `PENDING` or `REJECTED` results
- Aligned `player_profiles` away from the old integer-style `current_elo` placeholder and onto `current_rating NUMERIC(4,2)`
- Kept onboarding initial rating and ongoing progression on the same official `1.00..7.00` scale
- Added `rated_matches_count` to support dynamic `K` by experience
- Persisted rating history in a dedicated `player_rating_history` table instead of overloading `player_profiles`
- Protected against double application with both `match_results.rating_applied` and a unique `(match_id, player_profile_id)` history constraint

### Current Backend Status
- Match lifecycle now includes:
  - create
  - join
  - leave
  - cancel
  - assign teams
  - result submit
  - result confirm
  - result reject
  - controlled resubmit after rejection
- Only confirmed results update player rating
- Player rating now lives on the official `1.00..7.00` scale
- Rating history now exists
- `GET /api/rankings` now exists
- `GET /api/players/me/rating-history` now exists
- `GET /api/players/{id}/rating-history` now exists
- `GET /api/players/me/matches` now exists
- DB-backed integration tests now cover Flyway loading, confirmed-result rating application, rating-history creation, and double-application protection
- Tournament logic is still pending

### Likely Next Steps
- Add optional filters for player match history such as `status` or `scope` once the frontend needs them
- Extend DB-backed integration coverage around match lifecycle edge cases and rating history reads if needed
- Revisit amendment/dispute refinement only if the current result workflow proves insufficient
- After lightweight match-history filters, start tournament foundations

## 2026-03-13

### Summary
- Confirmed and fixed Flyway runtime execution for the local PostgreSQL setup
- Isolated the backend against its own local database configuration
- Added practical project documentation in `README.md`
- Added the first thin auth slices for registration, login, and JWT-backed authenticated requests
- Implemented the authenticated onboarding initial survey flow with persisted answers and calculated result
- Corrected the onboarding verification interpretation so Primera/Segunda remain visible and only carry pending verification state
- Implemented the first thin social match slice with create, join, leave, list, and detail
- Extended the match lifecycle with cancellation and result submission, still without ranking/ELO updates
- Added explicit team assignment and a pending/confirmed result workflow before ranking
- Added result rejection and controlled resubmission so pending results can safely return to playable state
- Implemented the first real rating/ELO slice on the official `1.00..7.00` scale, driven only by confirmed social match results
- Kept the modular-monolith structure intact and did not add refresh tokens yet

### Key Technical Decisions
- Kept Flyway as the only schema evolution mechanism
- Kept `ddl-auto=validate` so Hibernate only validates schema state
- Used Spring Boot 4 Flyway starter integration instead of relying on raw Flyway libraries alone
- Kept security in temporary bootstrap mode while introducing a real `PasswordEncoder`
- Used `BCryptPasswordEncoder` for persisted password hashing
- Used Spring Security authentication infrastructure for real credential verification
- Added a stateless JWT access-token flow for authenticated requests
- Normalized registration email values before persistence
- Defaulted newly registered users to role `PLAYER` and status `ACTIVE`
- Kept onboarding survey submissions in a dedicated table and mirrored only the current result on `player_profiles`
- Implemented the agreed initial rating formula, Uruguay category mapping, and Primera/Segunda verification gating
- Removed the mistaken visibility flag from the active schema and code model through a follow-up Flyway migration
- Added a dedicated `match` module with `Match`, `MatchParticipant`, and minimal `OPEN/FULL/CANCELLED` status handling
- Extended match persistence with a dedicated `match_results` table and added `COMPLETED` to the match lifecycle
- Extended match participants with explicit team assignment and match results with confirmation metadata/status
- Extended match results with rejection metadata and a reversible `RESULT_PENDING -> FULL` transition
- Aligned `player_profiles` from the old integer-style placeholder rating to `current_rating` on the official numeric scale and added `rated_matches_count`
- Added dedicated `player_rating_history` persistence plus an idempotent apply guard on confirmed match results
- Reused the lazy player-profile resolution approach so authenticated users can create/join/leave matches without redesigning auth

### Issues Encountered And Resolutions
- Flyway was not running at runtime
  - Cause: the project had Flyway libraries but not the Spring Boot 4 Flyway starter module
  - Resolution: added `spring-boot-starter-flyway`
- Runtime startup still failed after Flyway started
  - Cause: stale Jackson configuration no longer valid under Spring Boot 4
  - Resolution: removed the invalid Jackson serialization property
- Port `8080` was already in use locally
  - Resolution: documented `8081` as the safe local dev port when needed
- Local PostgreSQL admin credentials were not available from this environment
  - Resolution: documented the isolated database/user assumptions and kept repo config aligned with them
- Registration and login needed to coexist with bootstrap-mode security
  - Resolution: kept most endpoints usable, protected only `/api/auth/me`, and introduced Bearer-token validation
- Authenticated onboarding needed a player profile even though registration did not create one yet
  - Resolution: created the player profile lazily during initial survey submission with minimal defaults, without changing register/login flow
- Primera/Segunda visibility was initially interpreted incorrectly
  - Cause: verification requirement was first translated into a hidden/public category flag
  - Resolution: removed `publicCategoryVisible` from the model and kept only `requiresClubVerification` plus `clubVerificationStatus`
- Social matches needed authenticated player linkage even though player profile creation is still implicit
  - Resolution: extracted the lazy player-profile resolution into a reusable player service and used it from onboarding and matches

### Current Backend Status
- PostgreSQL connection works against the isolated `sentimospadel` database
- Flyway migration runs before Hibernate validation
- Shared error handling and audit base entity are in place
- Detailed chronological log for the day is available in `docs/bitacora-2026-03-13.md`
- Detailed onboarding/auth continuation log for the day is available in `docs/bitacora-2026-03-13-onboarding-auth.md`
- Modules currently implemented:
  - health
  - auth registration
  - auth login
  - auth current-user endpoint
  - onboarding initial survey submission
  - onboarding initial survey fetch
  - match create
  - match join
  - match leave
  - match cancel
  - match team assignment
  - match result submission
  - match result confirmation
  - match result rejection
  - match result read
  - rankings read
  - match list
  - match detail
  - user read endpoints
  - player read endpoints
  - club read/create endpoints
- Registration persists a new user with a BCrypt-hashed password
- Login authenticates against persisted users with Spring Security
- Login now returns a JWT access token
- `/api/auth/me` resolves the authenticated user from the JWT-backed security context
- The onboarding survey is now implemented as a post-registration authenticated flow
- The backend now calculates and persists the initial rating in the `1.00..7.00` range
- The backend now maps that rating to Uruguay categories from `PRIMERA` through `SEPTIMA`
- Onboarding initial rating and ongoing match-based progression now share the same official `1.00..7.00` scale
- Rating is always visible
- Estimated category is always visible
- Primera and Segunda require club verification and are represented by `requiresClubVerification=true` plus `clubVerificationStatus=PENDING`
- Tercera through Septima use `requiresClubVerification=false` plus `clubVerificationStatus=NOT_REQUIRED`
- No hidden visibility flag remains in the backend model
- `publicCategoryVisible` was removed from the schema and code after the product clarification
- The backend now supports the first social-match flow:
  - authenticated create
  - authenticated join
  - authenticated leave
  - authenticated cancel
  - authenticated team assignment
  - authenticated result submission
  - authenticated result confirmation
  - public list
  - public detail
- Match result reads are available
- Match participants are persisted explicitly and now carry explicit team assignment
- Match status now supports `RESULT_PENDING` and `COMPLETED`
- Matches can now return from `RESULT_PENDING` to `FULL` after rejection while preserving team assignment
- Confirmed social match results now update all 4 player ratings exactly once
- Pending and rejected results do not update rating
- Rating history is now persisted
- `GET /api/rankings` is now available
- `GET /api/players/me/rating-history` is now available
- `GET /api/players/{id}/rating-history` is now available
- `GET /api/players/me/matches` is now available
- DB-backed integration tests now validate Flyway migrations plus the confirmed result -> rating update -> rating history flow
- Club verification workflow itself is still not implemented
- Duplicate email registration is rejected cleanly
- Refresh tokens are still pending
- Tournament logic, reservations, payments, and club verification workflow are still pending

### Pending Next Steps
- Add the next player-profile lifecycle step so profile creation is explicit before other player features depend on it
- Add persistence-backed integration tests for onboarding once database-backed test wiring is introduced
- Add the actual club verification workflow for Primera/Segunda status transitions
- Add the next stability slice around result amendment/dispute refinement if product still needs it
- Add optional filter/query support on player-facing match history if product starts needing tabs like upcoming/completed/cancelled
- Extend persistence-backed integration tests around rating-history reads and match lifecycle edge cases
- After lightweight filters on `/api/players/me/matches`, start tournament foundations

### Roadmap
- Foundation stabilization
- Auth completion
- Player onboarding completion
- User and player profile lifecycle
- Club management improvements
- Match and result modeling
- Ranking and ELO after match/result modeling is ready
- Tournament features after gameplay foundations are stable
