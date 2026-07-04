# Concurrent Login Hidden Oracle

## Feature

A member should have only one active login session according to the selected policy.

## Required Behavior

- Newest login invalidates older tokens for the same member.
- Different members can stay logged in independently.
- Old token use fails with intentional authentication error, not a 500.
- Token/session state is server-side enough to enforce active-session policy.
- Same-member concurrent logins do not leak database uniqueness failures.

## Required Tests

- New token works after re-login.
- Old token fails after re-login.
- Different member token remains valid.
- A real concurrent test exists for same-member login race using latches/barriers/executors.
- Token parsing/invalid token behavior is directly or adequately covered.

## Architecture Checks

- Web resolver delegates auth/session validation to service.
- Session renewal is atomic or serialized for the same member.
- No speculative Redis/dependency addition unless explicitly required.
