# concurrent-login operational-policy Requirement Variant

## Variant Metadata

- Unit: concurrent-login
- Variant type: operational-policy
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/concurrent-login.md`
- Status: self-reviewed candidate

## Requirement

Define and implement an operational policy for concurrent login.

### Policy decision

For the same member account, decide how the service behaves when another login occurs while an earlier login may still be active.

The implementation must explicitly document one policy, such as:

- the newest login invalidates the previous login, or
- the previous login is preserved and the new login is rejected, or
- another clearly testable policy that limits concurrent login.

### Required behavior

- The service limits simultaneous login for the same account according to the selected policy.
- The selected policy determines how existing tokens or sessions are treated.
- The selected policy is enforced during authenticated requests, not only during login.
- The selected policy is covered by tests.

### Required explanation

- Explain whether the current authentication method is session-based or token-based.
- Explain why invalidating existing login state is easy or difficult with that method.
- Explain what additional server-side state is needed, if any.
- Explain whether the solution remains stateless.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: operations policy first.
- Risk of semantic drift: medium because examples must remain examples, not extra requirements.

