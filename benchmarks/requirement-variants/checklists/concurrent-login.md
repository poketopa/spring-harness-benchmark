# Semantic Checklist: concurrent-login

Canonical requirement:

- `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md`

## Required Behaviors

- The system limits simultaneous login for the same account.
- The implementation chooses and documents what happens to the old login when a new login occurs.
- The implementation chooses and documents whether the old login or new login wins.
- Existing tokens or sessions are handled according to the chosen policy.
- The implementation explains why the selected authentication style makes the requirement easy or difficult.
- The implementation records any additional server-side state required by the policy.

## Required Design Decisions

- The policy must be concrete enough to test.
- Token validation must account for stale tokens if newest-login-wins is selected.
- Session/token state storage must have a clear service and domain boundary.
- The README must explain the stateless-token trade-off if server-side state is introduced.

## Required Verification

- Tests cover first login success.
- Tests cover second login for the same account.
- Tests cover stale token rejection or the chosen old-login behavior.
- Tests cover latest token/session success.
- Tests include at least one focused unit or integration test for session/token state.

