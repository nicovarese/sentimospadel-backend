# Frontend / Backend Match Invite Link Alignment

## Goal
- Let a player invite a friend to a social match through a shareable link.
- Keep backend as the source of truth for official match state.
- Avoid fake links or frontend-only tokens.

## Current mismatch
- The frontend still contains a fake copy-link action using a timestamp URL.
- There is no backend contract to:
  - generate an official invite link
  - resolve an invite link back to a match

## Decision
- Use a backend-signed invite token instead of storing invite rows in the database.
- Use a query-param URL instead of a path-based deep link.

## Why query param
- The frontend is a Vite SPA without router infrastructure.
- A path like `/t/invite/...` risks server-side 404 issues in static hosting.
- A URL like `https://app.example.com/?matchInvite=<token>` works with the current app entrypoint and mobile web wrappers.

## Backend contract
- `POST /api/matches/{id}/invite-link`
  - authenticated
  - only a participant of the match can request the link
  - returns:
    - `matchId`
    - `inviteToken`
    - `inviteUrl`
    - `expiresAt`
- `GET /api/matches/invite?token=...`
  - public
  - resolves the signed token
  - returns a lightweight preview of the invited match

## Match rules
- Invite links are convenience access, not a second source of permission.
- Official join eligibility still depends on live backend match state:
  - open/full/cancelled/completed
  - duplicate participant protection
- The link can resolve successfully while the match later becomes full or cancelled.

## Frontend behavior
- When a joined player opens the share action, frontend requests the official invite link from backend and copies/shares it.
- On app load, frontend checks `matchInvite` in the URL query string.
- If present, frontend resolves it through backend and shows a dedicated invite preview.
- If the visitor is not authenticated, the invite preview is shown alongside login/register.
- If the visitor is authenticated as a player, they can join directly from the invite preview.
- If the visitor is authenticated as a club/admin account, the app shows that player access is required.

## MVP scope
- Social match invite links only.
- No private-lobby access control yet.
- No tournament invite links in this slice.

## Implementation status
- Backend contract is now live.
- Frontend no longer generates fake invite URLs for social match sharing.
- The active MVP path now supports:
  - creating an official match invite link from a joined backend-driven social match
  - copying or sharing that link from the app
  - opening `?matchInvite=<token>` from a logged-out or logged-in session
  - resolving the invite preview from backend before join
  - joining the match through the official backend join endpoint
- Club-created matches now try to open the official invite link immediately after booking so the club can share it without leaving the club flow.

## Validation
- `.\mvnw.cmd test`
- `npm run lint`
- `npm run build`
