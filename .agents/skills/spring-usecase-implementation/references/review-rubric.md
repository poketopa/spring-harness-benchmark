# Review Rubric

Use this before finalizing Spring implementation work.

## Architecture

- The solution is no more complex than the use case needs.
- Simplicity is not procedural dumping: responsibilities are still placed on the right object.
- Existing package style and API contracts are respected.
- API paths follow the existing project flow and resource mental model.
- Controller grouping follows one rule, such as resource-oriented or role-oriented, without mixing both accidentally.
- Materially ambiguous requirements were clarified with the user instead of silently choosing one of several plausible designs.
- New abstractions have a concrete trigger, not just aesthetic value.
- A feature with its own domain/usecase name has its own Service when that is where a maintainer would intuitively look for changes.
- A responsibility that appears only once can stay local when readable; two or more occurrences are a strong extraction signal.
- New convenience dependencies such as Lombok are not added unless already established or explicitly requested.
- If Lombok is used, JPA entities avoid broad annotations such as `@Data`, `@Setter`, generated equality, or generated `toString`.

## Controller and DTO

- Controller does not create domain objects for core rules.
- Request DTO handles HTTP input validation only.
- Different use cases do not share a request DTO just because their current fields match.
- Command record is introduced only when input composition justifies it.
- Response DTO converts domain data to API fields and does not expose domain objects directly.
- Response DTO factory naming is consistent: `from...` for one-source conversion and `of...` when additional calculated values are required.
- Unified list responses may use one DTO with a small number of nullable state-specific fields.
- Split response types when state-specific fields grow or their meanings diverge enough to confuse clients.
- Swagger/OpenAPI annotations are absent unless the project already uses them or documentation is required.

## Service and Repository

- Service unpacks request DTO/command before calling Repository/DAO.
- DAO/Repository receives domain objects, domain values, or needed primitive values, not web DTOs.
- Service transaction boundaries are explicit where DB access exists.
- Cross-aggregate repository knowledge is small and local, or otherwise moved behind an application facade.
- Cross-aggregate state transitions that create/delete different aggregate roots in one transaction have one clear usecase owner.
- Role authorization has one consistent boundary; similar roles are not split between dedicated authorization services and unrelated private helpers.
- Web authentication responsibility is split cleanly: argument resolver extracts web input and auth service handles token/session/member validation.
- Stateful authentication changes account for same-subject races, such as simultaneous first login, without leaking database uniqueness failures as 500 responses.
- Repeated `LoginMember -> Member` loading does not duplicate `UNAUTHORIZED` code/message policy across services.
- Simple rank/count/order queries stay readable; complex policy-heavy queries are not hidden in repository method names.
- List response mapping avoids avoidable per-row database checks when a single bulk query can provide the same state.
- Rank calculation does not load an entire ordered collection just to find one element when a direct count/query can express the same rule.
- Derived query method names are still easier to read than JPQL; otherwise explicit JPQL is preferred.
- Service methods are visually readable: lookup, validation, persistence, and mapping steps are separated or named.
- Validation helper extraction is consistent across sibling use cases in the same service.
- Lookup failure is converted to domain/usecase error, not leaked as technical exceptions.

## Domain

- Single-object rules are domain methods.
- Entity names match the domain language rather than mechanically combining adjacent aggregate names.
- Repeated collection rules are not duplicated in Service.
- Value objects have validation/behavior or clear semantic value.
- Value objects are introduced because the grouped value itself has an invariant or question to answer, not just to look more object-oriented.
- Domain does not depend on Spring, web, persistence, or global time calls.
- Domain constructors or factories reject single-object invalid state that the object itself can decide.
- Domain invariant validation stays in the owning domain class by default, not in a broad generic validator utility.
- Persistence-dependent rules are not forced into domain constructors.

## Persistence And Concurrency

- Concurrency-sensitive uniqueness and referential invariants are backed by database constraints.
- Service checks still provide friendly errors, but DB constraints are the final guard.
- Locks are absent unless DB constraints cannot protect the invariant and the race can cause material business harm.
- When a lock is used for login/session renewal, it is scoped to the smallest subject row or equivalent aggregate needed for the race.
- Harmless presentation races, such as a stale waiting rank, do not force locking.

## Exception and API Error

- Domain/usecase failures use a single code-based exception by default, not one exception class per failure.
- Domain/usecase error codes do not know HTTP status.
- HTTP status is mapped in the web adapter or global exception handler.
- The project uses one error response style consistently.
- `ProblemDetail`, `MessageSource`, and localized messages are present only when the project actually needs that API maturity.

## Tests

- Core user flow and important failure flow are covered.
- Domain rules are tested without Spring/DB when possible.
- Domain construction failures are tested for single-object invariants.
- Core logic has layered tests: domain unit tests, focused controller/service/repository integration tests, and acceptance tests.
- Repository behavior that only needs JPA uses a repository/JPA slice instead of a full application context when the project dependencies support it.
- Controller slice tests are separated from full HTTP/DB acceptance tests and full HTTP tests are not named `ControllerIntegrationTest`.
- Service orchestration failures have fast fake/mock unit coverage when they do not need Spring, DB, or transaction behavior.
- Explicit concurrency requirements have a real concurrent test, not only sequential before-and-after assertions.
- Custom token providers have direct tests for uniqueness, parsing, and invalid token behavior when those concerns are part of the implementation.
- Feature tests are separated enough that domain, controller, service, repository, and acceptance coverage can be found directly.
- Additional layer tests are focused on meaningful behavior, not exhaustive trivial CRUD duplication.
- Test methods have `@DisplayName` that shows behavior and expected result.
- Assertions check result state and domain behavior, not mostly implementation calls.
- Long service/mock tests and complex acceptance tests make given/when/then boundaries clear with comments or helpers, while short tests avoid unnecessary structure.
- Repeated setup is helper/fixture; important differences remain visible.

## Verification

- Run the smallest relevant test first.
- Run broader tests/build when the change touches shared contracts or multiple layers.
- If unable to run tests, report the exact gap and the next best evidence.

## Documentation

- README is updated only when requested, required by the assignment, or already part of the project's workflow.
- Meaningful technical decisions are captured as ADRs by default.
- ADRs stay concise: context, decision, alternatives considered, consequences, and follow-up triggers.
