# Frontend / Backend Alignment: Club Booking Modes

## Goal

Support three official booking flows for clubs without losing the current frontend experience:

1. `DIRECT`
   User books from the app and the reservation is confirmed immediately.
2. `CONFIRMATION_REQUIRED`
   User books from the app, but the club must approve or reject the request.
3. `UNAVAILABLE`
   The club is listed in the app, but public booking is disabled.

The backend remains the source of truth for booking behavior, match lifecycle, slot occupancy, and approval decisions.

## Domain Model

### Club

Add `bookingMode` to `Club` as an enum:

- `DIRECT`
- `CONFIRMATION_REQUIRED`
- `UNAVAILABLE`

Keep `integrated` for existing compatibility and historical semantics, but the booking flow must be driven by `bookingMode`.

### Match

Add a new social match status:

- `PENDING_CLUB_CONFIRMATION`

This status represents a real reservation request that already exists in backend but is not yet approved by the club.

## Public Booking Flow

### DIRECT

- Public availability returns the real slot agenda.
- User selects an `AVAILABLE` slot.
- Backend creates the match immediately as `OPEN`.
- Slot becomes `RESERVED`.
- Frontend keeps the current confirmation UX.

### CONFIRMATION_REQUIRED

- Public availability returns the real slot agenda.
- User selects an `AVAILABLE` slot.
- Backend creates a real match as `PENDING_CLUB_CONFIRMATION`.
- Slot becomes `PENDING_CONFIRMATION`.
- Frontend keeps the booking screen but changes the final CTA/copy to a request flow:
  - `Solicitar reserva`
  - success copy: `Solicitud enviada. Esperando confirmacion del club.`
- No invite link should be generated until the club approves.

### UNAVAILABLE

- Club still appears in the clubs catalog.
- Public availability endpoint returns the club booking mode, but the frontend must not expose slot booking as actionable.
- Frontend copy should clearly explain that reservations are not yet available in the app for that club.

## Club Agenda

The club agenda must support a fourth effective slot state:

- `PENDING_CONFIRMATION`

This state is derived from a real `Match` in `PENDING_CLUB_CONFIRMATION`.

The club admin can:

- approve the request
- reject the request

These actions must operate on the real pending match, not on generic slot overrides.

## Approval / Rejection Rules

### Approve

- only the admin of the managed club can approve
- only matches in `PENDING_CLUB_CONFIRMATION` can be approved
- approval moves the match to:
  - `OPEN` if it only has the creator
  - `FULL` if it already has 4 participants
- the slot becomes `RESERVED`
- the requesting player receives an in-app event notification

### Reject

- only the admin of the managed club can reject
- only matches in `PENDING_CLUB_CONFIRMATION` can be rejected
- rejection moves the match to `CANCELLED`
- the slot becomes available again
- the requesting player receives an in-app event notification

## Frontend Mapping

### Club card / booking screen

- `DIRECT`: reserve immediately
- `CONFIRMATION_REQUIRED`: request reservation
- `UNAVAILABLE`: show the club, but no actionable booking CTA

### Match status

Map backend `PENDING_CLUB_CONFIRMATION` to frontend `pending_approval`.

That status must:

- not allow invite link generation
- not behave as `confirmed`
- show the pending approval state honestly in match cards and invite previews

## Notifications

Add event notifications for:

- booking approved
- booking rejected

These are event notifications, not pending-action notifications.

## Demo Seeds

Seed one representative club for each case:

- `Top Padel`: `DIRECT`
- `World Padel`: `CONFIRMATION_REQUIRED`
- non-integrated catalog clubs: `UNAVAILABLE`

This keeps the product demoable with all three flows visible from day one.
