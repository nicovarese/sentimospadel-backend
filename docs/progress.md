# Progress Log

## 2026-03-13

### Summary
- Confirmed and fixed Flyway runtime execution for the local PostgreSQL setup
- Isolated the backend against its own local database configuration
- Added practical project documentation in `README.md`
- Added the first thin auth slices for registration, login, and JWT-backed authenticated requests
- Implemented the authenticated onboarding initial survey flow with persisted answers and calculated result
- Corrected the onboarding verification interpretation so Primera/Segunda remain visible and only carry pending verification state
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
- Rating is always visible
- Estimated category is always visible
- Primera and Segunda require club verification and are represented by `requiresClubVerification=true` plus `clubVerificationStatus=PENDING`
- Tercera through Septima use `requiresClubVerification=false` plus `clubVerificationStatus=NOT_REQUIRED`
- No hidden visibility flag remains in the backend model
- `publicCategoryVisible` was removed from the schema and code after the product clarification
- Club verification workflow itself is still not implemented
- Duplicate email registration is rejected cleanly
- Refresh tokens are still pending
- Match logic, result ingestion, ranking engine, tournament logic, and club verification workflow are still pending

### Pending Next Steps
- Add the next player-profile lifecycle step so profile creation is explicit before other player features depend on it
- Add persistence-backed integration tests for onboarding once database-backed test wiring is introduced
- Add the actual club verification workflow for Primera/Segunda status transitions
- Add match and result modeling before starting ranking engine work

### Roadmap
- Foundation stabilization
- Auth completion
- Player onboarding completion
- User and player profile lifecycle
- Club management improvements
- Match and result modeling
- Ranking and ELO after match/result modeling is ready
- Tournament features after gameplay foundations are stable
