# Testing Style

## Default Test Strategy

- Use E2E/acceptance tests and domain unit tests as the default axes.
- Cover key user flows and important failure flows with E2E/acceptance tests.
- Test domain objects, value objects, domain collections, and policies without Spring/DB when they contain rules.
- For core logic, include layered coverage: domain unit tests, integration tests around important controller/service/repository behavior, and acceptance tests.
- When a new feature has meaningful domain and service behavior, split tests by layer and by feature: domain unit test, focused repository integration test, focused service integration test, focused controller integration test when HTTP validation/status matters, and acceptance test for the user flow.
- Do not create exhaustive controller, service, and repository tests for every trivial CRUD path.
- Add focused layer tests when the boundary contains meaningful behavior: HTTP validation/contract, service orchestration, complex query/mapping, external integration, or fragile persistence behavior.
- Domain tests must construct objects without Spring and verify both successful behavior and constructor failures for single-object invariants.
- Repository query and database-constraint tests should use the narrow JPA slice when the project dependency set supports it. In Spring Boot 4, add the official test-only JPA slice starter before using `@DataJpaTest`.
- Controller slice tests should use a web slice and mocked services when only HTTP validation, status, path binding, or error body shape is under test. In Spring Boot 4, add the official test-only WebMvc slice starter before using `@WebMvcTest`.
- Full `RANDOM_PORT` tests are acceptance or HTTP integration tests, not controller slice tests. Name them `*AcceptanceTest` or `*HttpIntegrationTest` instead of `*ControllerIntegrationTest`.
- Add fast service unit tests with fakes or mocks for core orchestration failures that do not require Spring or the database; keep Spring integration tests for transaction, persistence, or wiring behavior.
- For requirements that are explicitly about concurrency or race-sensitive state, add at least one real concurrent integration or acceptance test using latches/barriers. A sequential replacement test is useful but does not prove the concurrent path.
- When a custom token provider adds parsing, random nonce, or invalid-token behavior, cover it with a focused unit test instead of relying only on indirect acceptance coverage.

## Test Doubles

- Prefer Fake over Mock when replacing collaborators.
- Verify result state and domain behavior before call counts.
- Use Mock mainly for external systems, mail/notification/payment side effects, or exception/timeout simulation.
- For time-sensitive service tests, inject and fix `Clock` instead of relying only on far-future dates.

## Naming and Structure

- Test names must reveal behavior and expected result.
- Use `@DisplayName` on tests.
- Prefer separate test classes by feature/use case when combining them would make the target behavior harder to find, such as `ReservationAcceptanceTest` and `WaitingAcceptanceTest`.
- Separate given/when/then flow when a test is long or setup/action/assertion are mixed.
- Use `// given`, `// when`, and `// then` comments for service tests with object creation plus mock/fake stubbing, and for long acceptance tests with several API calls. Do not force these comments into short domain or repository tests where the structure is already obvious.
- For simple domain unit tests, keep the body compact, but still add `@DisplayName`.

## Test Data and Fixtures

- Prepare test data directly when it does not hide the test intent.
- Extract helper/fixture when the same API request, domain object creation, or DB seed repeats two or more times.
- In E2E/acceptance tests, prefer public API helpers to create state.
- Use direct SQL only for repository/mapping tests, hard-to-create preconditions, or setup that would distract from the test purpose.
- Name helpers by domain action: `addReservation`, `createTheme`, `loginAsManager`.
- Do not let helpers hide important preconditions. Expose important differences as parameters.
