# Semantic Checklist: c1-waiting

Canonical requirement:

- `missions/roomescape-reservation-waiting/requirements/base/02-cycle1-waiting.md`

## Required Behaviors

- A slot is the combination of date, time, and theme.
- A user can create a waiting entry for a slot already reserved by another user.
- Waiting entries for the same slot are ordered by request order.
- The same user cannot create duplicate waiting entries for the same slot.
- A user can cancel their own waiting entry.
- The user's own reservations and waiting entries are returned together.
- Reservation and waiting entries are distinguishable by status.
- Waiting entries include the user's current waiting rank.

## Required Design Decisions

- The relationship between reservation and waiting must be explicit in the domain model.
- Waiting rank can be computed or stored, but the decision must be documented.
- API decisions for waiting creation, cancellation, and my-reservation read model must be documented in the target README.

## Required Verification

- Requirement-level tests cover waiting creation for an occupied slot.
- Tests cover duplicate waiting rejection.
- Tests cover cancelling own waiting.
- Tests cover combined own reservation/waiting lookup and rank display.
- Tests include a useful mix of unit, integration, or acceptance coverage following the project testing convention.

