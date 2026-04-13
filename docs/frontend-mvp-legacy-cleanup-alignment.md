# Frontend MVP Legacy Cleanup Alignment

## Goal
- Leave the visible MVP path cleaner before mobile packaging.
- Remove frontend-only residues that no longer represent the official product path.
- Keep backend contracts unchanged.

## Scope
- Frontend cleanup only.
- No backend endpoint or business rule changes.

## Cleanup rules
- Remove legacy views and mock datasets that are no longer referenced by the active app flow.
- Keep the visible UX structure, but stop showing actions that still depend on fake local behavior.
- Prefer honest empty states over fallback static catalogs.

## Visible fixes in this slice
- Competition view should stop relying on local tournament mock fallback wording.
- Create tournament should stop copying a fake invitation link.
- The active MVP path should continue using backend-driven:
  - matches
  - tournaments
  - clubs
  - profile insights
  - club management

## Completed in this slice
- `CreateTournamentView` keeps the same surface area, but the share-link card is now an honest placeholder instead of copying a fake URL.
- `CompetitionView` no longer keeps a visible fallback to static tournament cards.
- The `Ver todos` affordance in active tournaments remains visible but no longer pretends to navigate anywhere.

## Deferred cleanup
- Large dead sections inside `frontend/App.tsx` still exist (`Legacy` views, old mock datasets, local-only fallbacks).
- Those leftovers are no longer part of the active MVP path, but they should be removed in a dedicated code-health pass before packaging.

## Code-health cleanup completed after the visible sweep
- Removed dead mock datasets that no longer initialized any active state:
  - old club cards
  - old club rankings dataset
  - old initial matches dataset
  - old initial agenda dataset
  - old friend-invite dataset
- Removed unused legacy views from `frontend/App.tsx`:
  - `LegacyClubDashboardView`
  - `NationalRankingView` mock version
  - `ClubsViewLegacy`

## Remaining local compatibility debt
- Some local-only fallback logic still exists around non-backend matches and tournaments.
- That logic was intentionally left in place in this pass because it still guards older in-memory items and should be removed only when that compatibility path is explicitly retired.

## Out of scope
- New tournament invite-link backend contracts.
- Payments.
- Premium entitlements.
- Classes.
