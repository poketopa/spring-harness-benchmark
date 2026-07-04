# Manager Authorization Hidden Oracle

## Feature

Store managers can manage only reservations for their own store.

## Required Behavior

- Unauthenticated requests fail as authentication failures.
- Authenticated non-manager users are rejected.
- Managers can access only reservations belonging to their own store.
- Other-store access is rejected without leaking technical exceptions.
- Store-manager relationship is modeled explicitly enough for tests and service code.
- Existing error response style is preserved.

## Required Tests

- Own-store manager success.
- Other-store manager rejection.
- Non-manager rejection.
- Unauthenticated rejection.
- At least one service or acceptance test proves the authorization boundary.

## Architecture Checks

- Role/store authorization uses a consistent service boundary.
- Controller does not own role/store policy.
- Authentication lookup is not duplicated with drifting error behavior.
