# Manual End-to-End QA Checklist

This file mirrors the current cross-repo QA guide from `../../docs/manual-e2e-qa-checklist.md` so the backend repository keeps a committed QA-ready checklist alongside `docs/progress.md`.

Use the root-level checklist as the practical testing guide. The current integrated flows to prioritize are:

- auth
- onboarding
- social matches:
  - create
  - join
  - leave
  - cancel
  - result submit / confirm / reject / resubmit
- rankings
- rating history
- player match history
- LEAGUE tournaments:
  - create
  - join / leave
  - entry sync
  - launch
  - generated matches
  - standings
  - tournament result submit / confirm / reject
- club-backed reservation flow for real social match creation

Non-blocking demo/mock areas still outside the current core QA scope:

- club rankings
- top partners
- top rivals
- club dashboard operational data
- coaches / premium helper flows
- `ELIMINATION`
- `AMERICANO`
