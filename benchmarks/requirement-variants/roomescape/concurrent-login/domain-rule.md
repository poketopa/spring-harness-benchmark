# concurrent-login domain-rule Requirement Variant

## Variant Metadata

- Unit: concurrent-login
- Variant type: domain-rule
- Canonical requirement: `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/04-prevent-concurrent-login.md`
- Semantic checklist: `benchmarks/requirement-variants/checklists/concurrent-login.md`
- Status: self-reviewed candidate

## Requirement

Model login state so that a single account cannot remain simultaneously active in multiple login contexts unless the chosen policy explicitly allows it.

### Login state rules

- A login belongs to a member account.
- When the same account logs in again, the system applies one documented policy.
- The policy decides whether the previous login remains valid or the new login becomes authoritative.
- Existing tokens or sessions must be validated according to that policy.

### Token/session rules

- If token authentication is used, explain how already issued tokens are handled.
- If server-side state is added, explain what state is stored and where it belongs.
- If every token validation requires server state lookup, document the stateless-token trade-off.

### Verification rules

- Tests must cover first login.
- Tests must cover a second login for the same account.
- Tests must cover the old login behavior after the second login.
- Tests must cover the valid login behavior after the second login.

## Semantic Equivalence Review

- [x] No required behavior was removed.
- [x] No required behavior was weakened.
- [x] No new required behavior was added.
- [x] Ambiguous wording was avoided or intentionally recorded.
- [x] API, test, error, and transaction expectations remain equivalent to the canonical requirement.

## Notes

- Intended wording difference: domain state and invariant oriented.
- Risk of semantic drift: low.

