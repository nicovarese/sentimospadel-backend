# Progress Log

## 2026-03-31

### Summary
- Identified `AMERICANO` as the biggest remaining visible tournament slice still relying on frontend-local truth after `LEAGUE` and `ELIMINATION`
- Added a focused alignment note for this slice in `/docs/frontend-backend-tournament-americano-fixed-alignment.md`
- Implemented the first backend-driven `AMERICANO fijo` slice while intentionally leaving `AMERICANO dinámico` for the next pass
- Extended tournament persistence with `matches_per_participant` so the backend can own official fixed-americano launch and scheduling constraints
- Extended tournament launch so `AMERICANO` with `americanoType = FIXED` now generates official backend tournament matches instead of relying on local frontend generation
- Extended tournament standings so fixed americano now exposes an official flat table ordered by backend-owned points and games tiebreaks
- Extended tournament match result flow so fixed americano now supports submit / confirm / reject like the other operational tournament modes
- Extended pending actions so fixed-americano matches now appear in the backend-driven result tray once they become eligible by time
- Rewired the current frontend tournament data path so backend operational tournaments now include `AMERICANO fijo` in the existing competition/status flow without redesigning the UI
- Kept `AMERICANO dinámico` intentionally pending in this slice because its player-rotation rules are still more specialized than the fixed-team path

### Current AMERICANO Status
- `AMERICANO fijo` is now backend-driven for:
  - create
  - launch
  - generated matches
  - standings
  - submit / confirm / reject
  - pending actions
- `AMERICANO dinámico` still remains local/prototype and is the next tournament slice to implement

### Likely Next Steps
- Implement `AMERICANO dinámico` using backend-owned player rotation, generated matches, and individual standings
- Add QA seeds for fixed americano if manual testing needs a preloaded path similar to `LEAGUE` and `ELIMINATION`
- Continue cleaning the smaller premium/demo extras that still remain visible after the tournament modes are fully backend-driven

### Update Later In The Day
- Closed the pending `AMERICANO dinamico` slice instead of leaving it prototype-only
- Added `/docs/frontend-backend-tournament-americano-dynamic-alignment.md` to document the chosen model extension
- Added `entry_kind` to tournament entries so backend can keep:
  - official registered roster entries
  - generated per-match pair entries for dynamic americano
- Backend now owns dynamic americano:
  - launch
  - generated rotating pairings
  - generated matches
  - individual standings
  - submit / confirm / reject
  - pending actions
- Frontend now consumes backend dynamic-americano data in the existing tournament flow without redesigning the UI
- Added focused service tests for:
  - dynamic americano launch
  - dynamic americano result submit

### Final Update
- Closed the pending `club verification` workflow instead of leaving only onboarding flags
- Added `/docs/frontend-backend-club-verification-alignment.md` to document the chosen contract
- Added real backend verification-request persistence plus operational endpoints for:
  - player request flow
  - club-admin list
  - approve
  - reject
- Rewired the current frontend player profile and club shell to consume that workflow without inventing a second source of truth
- Kept the slice intentionally focused on official request / review state without opening document-upload or fraud-review scope

### Latest Update
- Closed the pending `club courts CRUD` slice instead of leaving `club_courts` as seed-only backend data
- Added `/docs/frontend-backend-club-courts-alignment.md` to document the chosen contract
- Added real backend club-court management endpoints for:
  - list
  - create
  - update
  - reorder
- Kept delete as soft deactivate so the official agenda remains backend-owned
- Added backend guardrails so:
  - a court with future reservations cannot be deactivated
  - a court with future real matches cannot be renamed in a way that would break location-based reconciliation
- Rewired the current frontend club shell to expose a real `Canchas` management view and replaced the dead `Torneos` shortcut in the dashboard with that backend-driven flow

### Newest Update
- Hardened auth so new accounts no longer become active before email confirmation
- Added `/docs/frontend-backend-auth-email-phone-search-alignment.md` to document the chosen contract
- Extended backend auth registration so both `PLAYER` and `CLUB` accounts must send a phone number and both `email` and `phone` are unique
- Added backend email-verification lifecycle:
  - `PENDING_EMAIL_VERIFICATION`
  - hashed verification token with expiration
  - public verify endpoint
  - resend endpoint
- Added SMTP-backed delivery support with a local/dev `log-only` mode so the repo can run without a real mail provider
- Rewired frontend register/login so registration no longer auto-logins and the login screen can resend the verification email for pending accounts
- Created initial player profiles during player registration so display name stops depending on frontend-local storage immediately after signup
- Audited the visible `buscar jugador` flows and confirmed:
  - tournament player search works client-side over backend-loaded `/api/players`
  - club booking invite search is still mock-based and remains outside this auth slice

## 2026-03-30

### Summary
- Split auth/session flows between separate `PLAYER` and `CLUB` accounts instead of mixing both worlds in one frontend shell
- Extended backend auth registration so club accounts are real first-class users:
  - `POST /api/auth/register` now accepts `accountType`
  - `CLUB` registration creates a real `clubs` row plus an `ADMIN` user linked through `users.managed_club_id`
- Extended auth responses (`register`, `login`, `/api/auth/me`) to expose `managedClubId` and `managedClubName`
- Rewired frontend login/registration so the user explicitly chooses `Persona` or `Club`
- Rewired frontend session bootstrap so backend role decides the shell:
  - `PLAYER` -> player app
  - `ADMIN` -> club app
- Removed `Club View` from the official player shell and removed player tabs from the official club shell while preserving the current visual identity
- Added a minimal logout affordance inside the backend-driven club dashboard, because club sessions no longer navigate through the player profile area
- Implemented the first backend-driven `ELIMINATION` tournament slice on top of the existing tournament foundations
- Extended tournament launch so `ELIMINATION` now generates official group-stage matches, persists group assignment in `tournament_entries.group_label`, and moves the tournament to `IN_PROGRESS`
- Extended tournament standings so elimination tournaments now expose grouped standings through `GET /api/tournaments/{id}/standings`
- Extended tournament result flow so confirmed elimination results can automatically generate semifinals/final when the corresponding stage is complete
- Rewired the existing frontend elimination create/launch/status flow to backend contracts while preserving the current tournament UI
- Kept `AMERICANO` intentionally pending; backend still rejects it as operational in this slice
- Added dedicated QA seeds for `ELIMINATION` so manual testing no longer depends on hand-creating knockout tournaments from the UI:
  - `Eliminatoria QA Abierta`
  - `Eliminatoria QA En Curso`

### Current Session Split Status
- Player accounts now stay inside the player experience only:
  - home
  - reservas
  - competir
  - competición
  - perfil
- Club accounts now stay inside the club-admin experience only:
  - `Club View`
  - club users
  - club agenda
- The backend remains the source of truth for deciding which shell is allowed
- Frontend mode selection is now only the intended access context; a mismatched account type is rejected after login instead of being silently accepted

### Likely Next Steps
- Decide if the player shell also needs a first-class visible logout action
- Decide whether new club accounts should configure courts during signup or in a second-step club setup flow
- Continue the mock-removal plan in the remaining visible areas:
  - `AMERICANO`
  - premium/demo profile extras
  - any leftover local tournament/club data paths

## 2026-03-26

### Summary
- Added the first real club-admin context by linking `ADMIN` users to a single managed club through `users.managed_club_id`
- Added backend persistence for club operations:
  - `club_courts`
  - `club_agenda_slot_overrides`
  - `club_activity_logs`
- Added a seeded club admin user plus initial courts, agenda overrides, and recent club activity so `Club View` can be tested without hand-loading everything from the UI
- Implemented the first real club management endpoints:
  - `GET /api/clubs/me/management/dashboard`
  - `GET /api/clubs/me/management/users`
  - `GET /api/clubs/me/management/agenda?date=YYYY-MM-DD`
  - `POST /api/clubs/me/management/agenda/slot-action`
  - `POST /api/clubs/me/management/quick-actions`
- Rewired the current frontend `ClubDashboardView`, `ClubUsersView`, and `ClubAgendaView` to consume those backend endpoints while preserving the current visual language and navigation
- Kept the club dashboard slice intentionally scoped to one managed club per admin user, without opening multi-club admin or payments scope yet

### Current Club Management Status
- `Club View` is now backend-driven for:
  - dashboard summary cards
  - recent activity
  - club user metrics
  - top users of the month
  - per-court agenda
  - manual reserve / block / free actions
  - quick actions that persist operational activity
- The dashboard revenue-like metric is currently an operational proxy based on occupied 90-minute slots and court hourly rates, because there is still no payments domain
- Non-admin users or admins without a managed club now see a clean backend-driven no-access state instead of club mocks

### Likely Next Steps
- Decide whether the `Club View` tab should remain visible for non-admin users or be gated once product wants stricter role-based navigation
- If club operations continue, add richer reservation semantics and real payment/revenue modeling instead of the current operational proxy
- Continue the mock-removal effort in the remaining visible product areas:
  - tournament modes beyond `LEAGUE`
  - profile/club extras that are still demo-only
  - any remaining legacy local data paths in `App.tsx`

## 2026-03-25

### Summary
- Added a dedicated historical seed slice for Felipe Rodriguez with older confirmed social matches, mixed wins/losses, and persisted rating-history rows so frontend QA can inspect recent results and rating movement without recreating the whole path from the UI
- Added backend-driven result eligibility based on real match end time (`scheduledAt + 90 minutes`) instead of surfacing result entry immediately when a social match becomes `FULL` or a tournament match is merely `SCHEDULED`
- Added `GET /api/players/me/pending-actions` so the frontend can read a unified pending tray for result submit/confirm across social matches and `LEAGUE` tournament matches
- Added persistent `player_notifications` storage with `UNREAD/READ` state plus references to social matches and tournament matches
- Added `GET /api/notifications` and `POST /api/notifications/{id}/read` as the first backend notification read path, still without mobile push
- Updated social/tournament result submission rules so backend rejects early result submission before the scheduled match has actually ended
- Rewired the frontend result-entry official path to use backend pending actions instead of treating `FULL` / `SCHEDULED` as immediate result-ready states
- Kept the current result UX intact by reusing the existing `ResultInputCard` modal/card flow instead of redesigning the app
- Fixed local development CORS for frontend/backend integration on `/api/**`
- Expanded the explicit local dev origin allowlist to cover `localhost` and `127.0.0.1` on ports `3000..3005`
- Confirmed that backend preflight `OPTIONS` requests now succeed for local frontend dev origins while keeping credentialed requests origin-scoped
- Fixed the backend auth wiring bug that prevented a freshly registered user from logging in immediately afterward
- Seeded the initial real club catalog through Flyway using the exact legacy frontend mock clubs so local/frontend testing no longer depends on a hardcoded club list
- Added a QA seed migration with pre-onboarded players plus social matches in useful test states so local manual testing no longer depends on creating every actor and match from the UI

### Current Local Dev Status
- Social/tournament result submission is now officially enabled only after the scheduled match end time
- Pending result actions now come from backend instead of frontend-local status assumptions
- Persistent notifications now exist for active result actions and can be marked as read
- Local frontend dev origins such as `http://127.0.0.1:3003` and `http://localhost:3004` are now allowed for `/api/**`
- Allowed methods include `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, and `OPTIONS`
- Allowed request headers include `Authorization` and `Content-Type`
- Security remains stateless and JWT-protected routes continue to behave as before
- Preflight requests are explicitly permitted for `/api/**`
- `register -> login -> /api/auth/me` now works end-to-end against the real backend
- The `/api/clubs` catalog now returns a deterministic DB-backed seed list in insertion order for local QA
- The local database now also includes seeded QA users, completed onboarding submissions, and seeded social matches for manual end-to-end testing

## 2026-03-24

### Summary
- Completed a focused frontend/backend e2e-completion audit based on the current visible frontend surface
- Added a practical manual QA checklist for the currently integrated frontend/backend flows
- Fixed the backend local startup blocker caused by a Hibernate/Flyway type mismatch on onboarding survey answer columns (`initial_survey_submissions.q1..q10`)
- Finished the remaining high-value frontend wiring gaps that already had backend support:
  - tournament `LEAGUE` explorer/list read path
  - tournament `LEAGUE` join/leave surfacing in the existing card CTA
  - club explorer/catalog read path for the reservation flow
  - social match leave/cancel surfacing in the existing match-card CTA
- Kept frontend visual structure intact and limited changes to adapters, state wiring, and existing button behavior

### Current Integration Status
- The frontend now uses real backend data for:
  - auth/session
  - onboarding
  - player profile hydration
  - social match create/join/leave/cancel/result flow
  - player match history with scopes
  - rankings
  - rating history
  - tournament `LEAGUE` explorer/list
  - tournament `LEAGUE` create/entry sync/launch/generated matches/result flow/standings/join/leave
  - club list used by the reservation flow
- The main frontend-visible areas still intentionally mock or only partially integrated are:
  - club rankings
  - top partners
  - top rivals
  - club dashboard operational data
  - coaches / premium helper flows
  - `ELIMINATION` / `AMERICANO`

### Likely Next Steps
- Decide whether to keep current mock-only profile/club extras as demo-only for QA or to open backend scope for them
- If staying within current product scope, focus next on small polish/read gaps rather than new feature slices
- If tournament work continues, choose between deeper `LEAGUE` read/detail polish and broader tournament-mode support
- If social-match UX continues, consider whether exposing additional backend actions or filters materially improves QA coverage

## 2026-03-23

### Summary
- Completed a frontend/backend integration audit across the local `backend/` and `frontend/` repositories
- Confirmed that the backend is already the effective source of truth for auth, onboarding, match lifecycle, result workflow, rating, and rankings
- Identified that the current frontend still behaves as a prototype with local mocks, local business logic, and legacy Supabase assumptions
- Added a typed frontend API client aligned to the current backend contracts as the first migration step away from mock-only state
- Corrected the frontend onboarding and top-category messaging so Primera/Segunda remain visible and only carry pending verification state
- Integrated frontend auth and onboarding against the real backend while preserving the existing UI flow
- Integrated the frontend social-match critical path against the real backend lifecycle without redesigning match screens
- Integrated the frontend national ranking and player rating-history views against backend ranking/history endpoints
- Added lightweight `scope` filters to `GET /api/players/me/matches` so the frontend can segment the authenticated player's match history more naturally
- Implemented the first tournament foundations slice with create/list/detail/join/leave
- Audited frontend/backend tournament mismatch in detail and documented the staged plan in `/docs/tournament-frontend-backend-alignment.md`
- Extended the backend tournament model to align with the current frontend expectations around formats, launch semantics, team entries, and tournament-specific match state
- Implemented the first operational tournament slice for `LEAGUE`:
  - creator entry sync
  - launch
  - generated league fixtures
  - tournament-match result submit/confirm/reject
  - live standings read

### Key Integration Findings
- Backend endpoints already exist for the main live slices:
  - auth
  - onboarding survey
  - rankings
  - player rating history
  - player match history
  - social match lifecycle
- The main gap is frontend wiring, not backend endpoint availability
- The frontend currently duplicates backend-owned logic in:
  - onboarding score calculation
  - category mapping
  - match rating progression
  - parts of match/result lifecycle state
- Frontend match/status assumptions still differ materially from backend contracts
- The old hidden-visibility interpretation for Primera/Segunda was still present in the frontend and was corrected in this pass

### Current Backend / Integration Status
- Backend contracts did not require redesign in this pass
- The frontend/backend audit report is available in `/docs/frontend-backend-integration-audit.md` at the workspace root
- A typed frontend backend client now exists as the first contract-alignment layer
- No backend business-rule changes were required specifically for this integration pass
- The frontend auth/onboarding integration report is available in `/docs/frontend-backend-auth-onboarding-integration.md`
- The frontend match integration report is available in `/docs/frontend-backend-matches-integration.md`
- The frontend ranking/history integration report is available in `/docs/frontend-backend-ranking-history-integration.md`
- The current frontend now uses backend as the official source of truth for:
  - auth session state
  - onboarding result
  - social match lifecycle state
  - ranking order
  - player rating history
- `GET /api/players/me/matches` now supports lightweight `scope` filters for `upcoming`, `completed`, `cancelled`, and `pending_result`
- Tournament audit report now exists at `/docs/tournament-frontend-backend-alignment.md`
- Tournament backend now understands:
  - `LEAGUE`
  - `ELIMINATION`
  - `AMERICANO`
- Tournament metadata now includes open vs closed enrollment intent, competitive/recreational intent, launch config, and standings tiebreak
- Tournament entries are no longer limited to single-player registrations
- Tournament entries now support team-style registration with:
  - primary player
  - optional secondary player
  - team name
  - optional time preferences
- Tournament creator can now synchronize entries through `PUT /api/tournaments/{id}/entries`
- Tournament creator can now launch through `POST /api/tournaments/{id}/launch`
- Tournament-generated matches now exist as a backend domain separate from social matches
- `GET /api/tournaments/{id}/matches` now exists
- `POST /api/tournaments/{id}/matches/{matchId}/result` now exists
- `POST /api/tournaments/{id}/matches/{matchId}/result/confirm` now exists
- `POST /api/tournaments/{id}/matches/{matchId}/result/reject` now exists
- `GET /api/tournaments/{id}/standings` now exists
- Tournament-generated matches do not affect official player rating
- Only `LEAGUE` is operational after launch in this pass
- League launch currently generates double round-robin fixtures only
- League standings are now calculated from confirmed tournament results with backend-owned `3-1-0` scoring
- Elimination bracket generation is still pending
- Americano operational pairing is still pending
- Advanced scheduling across courts/tournaments is still pending

### Likely Next Steps
- Consider wiring public player rating history to `GET /api/players/{id}/rating-history` only if the profile UX actually requires it
- Integrate the current frontend tournament screens against the new backend contracts through adapters instead of local tournament state
- Build the next tournament slice:
  - elimination bracket generation
  - americano operational engine
  - richer scheduling heuristics across available courts

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
- `GET /api/players/me/top-partners` is now available
- `GET /api/players/me/top-rivals` is now available
- `GET /api/players/me/club-rankings` is now available
- `GET /api/coaches` is now available
- Frontend/backend integration for social matches was completed against the existing backend contracts
- The current frontend now uses real backend reads for player match history and match discovery
- The current frontend now uses backend as the official source of truth for social match create/join/result submit/result confirm/result reject
- The current frontend no longer applies official social-match rating changes locally
- The current frontend profile analytics now use backend as the official source of truth for:
  - top partners
  - top rivals
  - club rankings
- The current frontend coaches screen now uses real backend data instead of local mock coaches
- Dedicated visible frontend controls for leave/cancel/manual team assignment are still pending and remain likely follow-up work
- DB-backed integration tests now validate Flyway migrations plus the confirmed result -> rating update -> rating history flow
- Added dedicated QA seed scenarios for manual testing:
  - `QA Social Join`
  - `QA Social Submit`
  - `QA Social Validacion`
  - `Liga QA Abierta`
  - `Liga QA En Curso`
- Club verification workflow itself is still not implemented
- Club operational dashboard / courts / reservations are still pending a real backend model and admin context
- Duplicate email registration is rejected cleanly
- Refresh tokens are still pending
- Tournament logic, reservations, payments, and club verification workflow are still pending

### Latest Updates
- Club verification workflow is now implemented end-to-end:
  - players can request verification from their profile
  - club admins can approve or reject from club management
  - the official player verification status is updated in backend
- Club courts CRUD is now backend-driven:
  - list
  - create
  - update
  - activate/deactivate
  - reorder
- Registration hardening is now in place:
  - phone is required
  - email uniqueness is enforced
  - phone uniqueness is enforced
  - email verification is required before login
- Legal consent is now backend-driven at registration:
  - terms and conditions are fetched from backend
  - privacy policy is fetched from backend
  - required acceptances are validated in backend before account creation
  - optional preferences for activity tracking and operational notifications are persisted
- Public club booking is now backend-driven for MVP:
  - `GET /api/clubs/{id}/booking-availability` exposes active courts plus official slot status
  - frontend club booking now consumes real slot availability
  - `POST /api/matches` now rejects blocked or already-reserved club slots
  - booking UI no longer exposes invitations, private-lobby, or payment options without backend support
- Tournament archive is now backend-driven:
  - `POST /api/tournaments/{id}/archive`
  - tournament reads now expose `archived` and `archivedAt`
- Automated validation status after these slices:
  - backend: `146` tests green
  - frontend: `npm run lint` green
  - frontend: `npm run build` green

### Pending Next Steps
- Remove remaining local tournament UI states:
  - launch preview
  - leftover legacy branches in frontend
- Decide whether public club booking needs:
  - real invitation workflow
  - player search/contacts
  - payment/billing
  or whether those stay out of MVP
- Expand in-app notifications coverage across the existing real flows
- Add push delivery after the notification event model is complete
- Add optional filter/query support on player-facing match history if product starts needing tabs like upcoming/completed/cancelled
- Extend persistence-backed integration tests around rating-history reads and match lifecycle edge cases

### Roadmap
- Foundation stabilization
- Auth completion
- Player onboarding completion
- User and player profile lifecycle
- Club management improvements
- Match and result modeling
- Ranking and ELO after match/result modeling is ready
- Tournament features after gameplay foundations are stable
