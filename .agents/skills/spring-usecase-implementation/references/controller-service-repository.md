# Controller, Service, Repository

## Controller

- Controller may pass a request DTO directly to Service for simple input forwarding.
- Controller must not create domain objects for core rules or judge domain policy.
- Introduce an application command record when request body, login user, path variable, query parameter, or other inputs must be composed.
- Do not introduce command/assembler for every endpoint by default.
- Choose one controller grouping rule per project. In resource-oriented projects, keep role-prefixed endpoints such as `/manager/reservations` inside the resource controller unless the whole project consistently uses role-based controllers.
- Do not expose compatibility aliases by default. Add them only when a requirement or existing client explicitly needs them, and document the compatibility status.

## HTTP Contract

- New APIs default to:
  - create: `201 Created`
  - delete: `204 No Content`
  - read: `200 OK`
  - update: `200 OK`
- Include `Location` for create responses when practical.
- If an existing project, assignment, or acceptance test fixes `200 OK` behavior, preserve that contract.
- Keep status code usage consistent within one project.
- Choose paths from the current project flow. Nested or grouped paths are acceptable when they match the user's mental model and existing resource flow.

## Swagger/OpenAPI

- Do not generate Swagger/OpenAPI annotations by default.
- Add them only when `springdoc`/OpenAPI dependencies already exist or API documentation is an explicit deliverable.
- In Controller, limit annotation to key `@Operation` and important success/failure `@ApiResponse`.
- In DTO, add `@Schema` only for unclear fields or useful examples.

## Request DTO

- Request DTO validates HTTP input only: required values, blank text, simple format, positive IDs.
- Use a separate request DTO when the use case or domain meaning differs, even if the current fields match another request.
- Prefer Bean Validation annotations such as `@NotNull`, `@NotBlank`, `@Positive` when validation is already used.
- For small projects without Bean Validation, record compact constructors may be used sparingly.
- Domain/usecase rules belong in Service or Domain: availability, duplication, past date, ownership, authorization, and reference existence.

## Response DTO

- Response DTO may have `from(domain)` static factories.
- Use `from...` for simple domain-to-response conversion from one source object.
- Use `of...` when the response factory needs additional calculated values or state, such as rank or reserved flags.
- Do not expose domain objects directly in JSON response fields.
- Convert to primitives, strings, booleans, numbers, enum names, nested response DTOs, or explicit API date/time representations.
- Controller may call `ReservationResponse::from`, but domain internals should not decide JSON shape.

## Service

- Service may receive request DTO directly for simple CRUD.
- At the Service entry, unpack request DTO/command into values or domain objects.
- Do not pass request DTO or command to DAO/Repository.
- Service may coordinate repository lookup, transactions, external input composition, current time, and multi-object flow.
- A Service may know another aggregate's repository for a small number of simple cross-aggregate reads.
- Split a new feature into its own Service when the user would naturally look for that feature by its domain/usecase name, even if its fields look similar to an existing feature.
- In reservation/waiting-style flows, keep reservation use cases in `ReservationService` and waiting use cases in `WaitingService`; use a small application read service/facade only when an API must combine both.
- If reservation and waiting must be converted/promoted in one transaction, keep the promotion orchestration in a small usecase service such as `WaitingPromotionService` instead of letting `ReservationService` call `WaitingService` while `WaitingService` writes reservations.
- If many use cases repeatedly coordinate several repositories or services, consider an application facade before scattering that orchestration.
- If a cross-aggregate merge or orchestration exists in only one place, keep it local when the method remains readable.
- Extract it when the same responsibility appears two or more times or the method stops reading as clear lookup, validation, save, and response flow.
- Service may handle not-found conversion, authority subject lookup, and storage-choice branching.
- In web authentication, argument resolvers should extract web input such as headers and delegate token/session/member validation to an authentication service. Avoid repository-backed authentication policy inside the resolver.
- If token authentication becomes server-stateful, such as one active session per member, serialize session renewal for the same authentication subject. Prefer a subject row pessimistic lock or an equivalent atomic update before inserting or replacing the session, and use `saveAndFlush` or explicit conflict handling so database uniqueness failures do not leak as 500 responses.
- When role authorization is needed across services, use a consistent authorization boundary. For example, if admin checks are in `AdminAuthorizationService`, manager checks should use the same style rather than private helpers inside one usecase service.
- When `LoginMember -> Member` lookup appears in several services, centralize it in a small authenticated-member lookup service so `UNAUTHORIZED` code and message do not drift.
- Move rules a single domain object can judge into domain methods: owner check, past reservation, state transition, cancellation, and invariants.
- If the same domain-rule `if` appears twice in Service, move it toward domain behavior, domain collection, or policy.

## Readability

- Separate lookup, domain construction, validation, persistence, and response mapping with small visual gaps or focused private methods.
- If one use case uses a named validation helper for multiple checks, use the same style for sibling use cases in the same service, such as `validateCreateAllowed` and `validateChangeAllowed`.
- Do not extract every line into a method; extract when it names a real step or removes repeated lookup/validation noise.
- Avoid adding Lombok or other convenience dependencies unless the project already uses them or the user explicitly chooses that convention.
- If Lombok is explicitly chosen, keep DTOs as records where practical and use Lombok mainly for constructor/getter boilerplate.

## Transaction

- DB-access Service classes should default to `@Transactional(readOnly = true)`.
- Create/update/delete methods should use write `@Transactional`.
- Apply this by default in JPA projects.
- In JDBC/JdbcTemplate projects, apply it when multiple DB operations form one use case.
- Omit for services without DB access or very small learning examples.
- Add database constraints for invariants that can be violated by concurrent requests, such as unique reservations or duplicate waiting.
- Use service validation for user-friendly errors, but do not rely on service validation alone for concurrency-sensitive invariants.
- Introduce locks only when database constraints cannot protect the invariant and the race can cause material business harm.
- If a race only causes harmless or easily corrected presentation drift, such as a temporarily stale waiting rank, avoid locks.

## Time

- Controller does not create the current time.
- Service injects `Clock` when current time matters.
- Use `LocalDateTime.now(clock)` or `LocalDate.now(clock)` in Service.
- Pass `now` into domain methods for time-dependent rules.
- Domain objects must not call `LocalDateTime.now()` directly.

## Repository

- Do not create repository interfaces by default.
- For simple CRUD with one implementation, concrete DAO or Spring Data `JpaRepository` may be injected directly into Service.
- Introduce a domain-facing repository interface when persistence implementation may change, in-memory fake tests are useful, a domain service depends on a repository, or persistence technology leaks into application/domain code.
- Define repository interface methods in domain language, not SQL/technology language.
- DAO/JpaRepository must not receive request/web DTOs.
- Prefer a simple repository query for straightforward ranking/counting/order lookups that the database can answer cleanly.
- Prefer existing bulk repository reads over per-row `exists` checks when mapping list responses, such as loading all reservations for a theme/date once and mapping reserved time ids in memory.
- Rank calculation should prefer a direct repository count/query over loading the full ordered collection and using `indexOf`, unless the collection is already needed in memory for the same use case.
- Choose rank/order implementations by this priority: intuitive domain meaning, usable with many users, then readable code.
- Long Spring Data derived query method names are acceptable while they remain readable.
- Introduce JPQL or explicit query methods when the method name becomes harder to read than the query itself.
- If the query becomes hard to read, hard to test, or starts encoding policy beyond filtering/counting/order, move the rule toward application/domain logic or a dedicated policy.

## Lookup Failure

- Use `Optional<T> findById(id)` for possibly missing lookup.
- Add `getById(id)` when missing ID always has the same domain not-found meaning.
- Implement `getById(id)` by calling `findById(id)` and throwing a domain-specific error.
- If message/meaning differs by use case, use `findById(id)` and convert in Service.
- For user-owned resources, return the same not-found response when the resource is missing or belongs to another user, unless the product explicitly needs a forbidden response.
- Convert JDBC `queryForObject` empty result to `Optional` or domain exception before it reaches Controller.
