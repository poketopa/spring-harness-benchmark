# Semantic Checklist: manager-authz

Canonical requirement:

- `missions/roomescape-reservation-waiting/requirements/add-ons/auth-authorization/03-store-manager-authorization.md`

## Required Behaviors

- The system distinguishes authentication from authorization.
- The system can determine whether the logged-in user is a store manager.
- The system can determine which store a manager is responsible for.
- Store managers can list, change, and delete only reservations that belong to their own store.
- Requests for another store's reservations are rejected.
- Unauthenticated requests are treated as authentication failures.
- Authenticated but unauthorized requests are treated as authorization failures.
- Authentication and authorization failures are not collapsed into one generic case.

## Required Design Decisions

- Authorization logic must not be scattered across controllers.
- The authorization boundary is explicitly chosen and explainable.
- The manager-store relationship is explicitly modeled.
- The response policy for forbidden or hidden resources is explicitly decided.
- The bridge from login identity to domain manager/store identity is explicit.

## Required Verification

- Tests cover own-store manager reservation access.
- Tests cover cross-store access rejection.
- Tests cover unauthenticated access rejection.
- Tests cover authenticated non-manager or wrong-manager rejection.
- Tests assert different handling for authentication and authorization failures.

