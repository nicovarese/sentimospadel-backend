# Progress Log

## 2026-03-13

### Summary
- Confirmed and fixed Flyway runtime execution for the local PostgreSQL setup
- Isolated the backend against its own local database configuration
- Added practical project documentation in `README.md`
- Added the first thin auth slices for registration and login
- Kept the modular-monolith structure intact and did not add JWT yet

### Key Technical Decisions
- Kept Flyway as the only schema evolution mechanism
- Kept `ddl-auto=validate` so Hibernate only validates schema state
- Used Spring Boot 4 Flyway starter integration instead of relying on raw Flyway libraries alone
- Kept security in temporary bootstrap mode while introducing a real `PasswordEncoder`
- Used `BCryptPasswordEncoder` for persisted password hashing
- Used Spring Security authentication infrastructure for real credential verification
- Normalized registration email values before persistence
- Defaulted newly registered users to role `PLAYER` and status `ACTIVE`

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
  - Resolution: kept `permitAll`, added a real `AuthenticationManager`, and kept JWT deferred

### Current Backend Status
- PostgreSQL connection works against the isolated `sentimospadel` database
- Flyway migration runs before Hibernate validation
- Shared error handling and audit base entity are in place
- Modules currently implemented:
  - health
  - auth registration
  - auth login
  - user read endpoints
  - player read endpoints
  - club read/create endpoints
- Registration persists a new user with a BCrypt-hashed password
- Login authenticates against persisted users with Spring Security
- Duplicate email registration is rejected cleanly
- JWT is still pending
- Player onboarding, declared level, and ranking-related product decisions are intentionally deferred

### Pending Next Steps
- Add the next auth delivery step, likely JWT or an equivalent session/token strategy
- Add player profile creation/linking strategy for registered users
- Add integration tests around registration and repository behavior
- Add profile-specific local development configuration if needed

### Roadmap
- Foundation stabilization
- Auth completion
- User and player profile lifecycle
- Club management improvements
- Match and result modeling
- Ranking and ELO after match/result modeling is ready
- Tournament features after gameplay foundations are stable
