# Domain Modeling

## Domain Responsibility

- Put rules a single domain object can decide into that domain object.
- Keep Service focused on orchestration: lookup, transaction, input composition, and saving.
- Validate single-object invariants at domain construction time when the object itself can decide them, such as required references, non-blank names, required timestamps, and role restrictions like a `Store` requiring a manager.
- Keep simple invariant validation inside the domain class, usually as private validation methods near the constructor or behavior that first calls them. Do not introduce a generic `DomainValidator` utility by default; it makes the rule feel less owned by the domain object.
- If the same value validation becomes meaningfully duplicated, prefer considering a value object with domain meaning over adding a broad validation utility.
- Do not move repository-state rules into constructors. Duplicate slots, existing reservations, rank, and other persistence-dependent rules stay in Service, domain service, repository queries, or database constraints.
- Name entities with the domain noun the user uses. Prefer `Waiting` over a mechanically qualified name such as `ReservationWaiting` when the feature is simply called waiting.
- Domain must not depend on Spring, web DTOs, persistence APIs, or current-time globals.
- Do not keep speculative domain methods. If a method is not used by the current use case or existing public contract, delete it.
- Do not add Lombok to a project unless it is already used or explicitly requested.
- If Lombok is explicitly used on JPA entities, limit it to safe boilerplate such as `@Getter` and `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- Do not use `@Data` on JPA entities.

## Value Objects

Do not create value objects by default. Introduce one when at least one signal exists:

- The same validation, parsing, or comparison repeats in two or more places.
- Primitive/String usage hides domain meaning.
- The value has behavior, not just storage.
- Multiple fields always move together, such as date + time forming a `Schedule`.
- The grouped value itself has something to validate or answer.

Value objects must carry meaningful responsibility such as validation, comparison, or domain transformation. Simple IDs, names, and one-off descriptions may stay as standard types.
Do not create a value object only to move validation away from the parent domain. Create it when the invariant belongs to the grouped value itself, so construction or domain methods make that invariant clear.

## Domain Collection, Policy, Domain Service

- Do not create domain facade/domain service by default.
- Keep simple CRUD and one-off composition in Service.
- Keep simple ranking/counting/order calculations in repository queries when they are naturally expressed by the database.
- Move ranking/counting/order rules toward domain/application logic when the query becomes complex or starts carrying business policy.
- Move a repeated collection rule to a domain collection.
- Move algorithmic or replaceable policy logic to a policy/domain service.
- Consider these only when:
  - multiple domain objects' collection rule repeats two or more times,
  - Service calculates domain policy more than it coordinates transactions,
  - policy replacement is likely.
- If application service and domain facade overlap, the facade is probably too early.

## Exceptions

- Use one code-based domain/usecase exception such as `RoomescapeException(ErrorCode, message)` by default.
- Do not create one exception class per failure case.
- Domain/usecase error codes must not depend on HTTP status or web APIs.
- Map domain/usecase error codes to HTTP status in a web adapter such as `GlobalExceptionHandler` or an error-status mapper.
- Handle centrally in `GlobalExceptionHandler` or existing equivalent.
- Separate exception types are allowed only for broad flows such as auth/authz, validation, or external system failure.

## Error Response

- Small personal/learning/portfolio projects default to `ErrorResponse(code, message)`.
- External public APIs, collaboration APIs, or Spring 6-standard projects may use `ProblemDetail`.
- Introduce `MessageSource`, localized error titles/details, or full `ProblemDetail` infrastructure only when public API, i18n, or collaboration documentation needs justify it.
- Do not mix `ErrorResponse` and `ProblemDetail` in one project.
- If using `ProblemDetail`, include a `code` property.
