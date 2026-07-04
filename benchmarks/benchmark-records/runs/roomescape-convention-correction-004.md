# roomescape-convention-correction-004

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: convention-correction
- Requirement: user feedback after test convention audit
- Baseline: benchmarks/roomescape-jpa-auth-cycle2-regen
- Target project: benchmarks/roomescape-jpa-auth-cycle2-regen
- Skill: spring-usecase-implementation
- Started at: not captured
- Finished at: 2026-07-03T13:42:45+09:00
- Duration minutes: not captured
- Result status: pass

## Prompt

```text
테스트 코드에 대한 의견: 도메인 생성 자체 검증, mock/fake 서비스 테스트 층, repository slice, controller slice와 인수 테스트 분리, 패키지 구조, 누락 케이스를 확인하고 내 컨벤션은 남기되 생성 실수는 보완/반영.
```

## Expected Decision Rules

- [x] domain_invariants: single-object invalid state is rejected by domain construction or domain behavior.
- [x] persistence_rule_boundary: repository-state rules are not forced into constructors.
- [x] test_layering: domain, service, repository, controller slice, HTTP integration, acceptance tests are findable separately.
- [x] repository_slice: repository tests use the narrow JPA slice when possible.
- [x] controller_slice_naming: full HTTP tests are not named controller integration tests.
- [x] service_unit_coverage: core service failure flows have fast mock/fake tests when Spring/DB is not needed.
- [x] time_test_control: service tests with current time use fixed `Clock`.
- [x] verification: run targeted tests before full verification.

## Generated Result

### Main Files

- Domain: `DomainValidator`, `Member`, `Reservation`, `ReservationTime`, `Store`, `Theme`, `Waiting`
- Controller: no production controller behavior change
- Service: no production service behavior change
- Repository: no production repository behavior change
- DTO: no DTO change
- Tests:
  - Added domain invariant tests for member/time/reservation/waiting/store/theme.
  - Added service unit tests for reservation create validation, waiting create validation, store creation validation, and time deletion in-use validation.
  - Moved repository tests to `roomescape.repository` and changed them to `@DataJpaTest`.
  - Added `@WebMvcTest` controller slice tests for reservation and waiting request validation.
  - Renamed full HTTP tests from `*ControllerIntegrationTest` to `*HttpIntegrationTest`.
  - Added acceptance coverage for signup/login, admin setup, self-reservation waiting rejection, and manager cancellation.
- Docs: README test convention correction section.

### Verification

- Narrow test: `./gradlew test --tests 'roomescape.domain.*Test' --tests 'roomescape.service.*Test' --tests 'roomescape.repository.*Test' --tests 'roomescape.controller.*Test'`
- Narrow result: pass
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| domain_invariants | Domain construction validates single-object invariants | Constructors now reject blank/null core fields and non-manager store ownership | pass | P1 | `src/main/java/roomescape/domain` |
| persistence_rule_boundary | DB-state rules stay out of constructors | Duplicate reservation/waiting slot rules remain in service/repository/DB tests | pass | P1 | `ReservationService`, `WaitingService`, repository tests |
| test_layering | Domain/service/repository/controller/acceptance tests are separated | Added package-separated unit/slice tests and kept full HTTP acceptance tests | pass | P1 | `src/test/java/roomescape` |
| repository_slice | Repository tests use JPA slice | Repository tests now use Boot 4 `@DataJpaTest` with test slice starter | pass | P1 | `src/test/java/roomescape/repository` |
| controller_slice_naming | Full HTTP tests are not controller slice tests | Full HTTP tests renamed to `*HttpIntegrationTest`; `@WebMvcTest` slice tests added | pass | P1 | `src/test/java/roomescape/controller` |
| service_unit_coverage | Core service failures can be tested without Spring/DB | Added mock-based service unit tests with fixed `Clock` where needed | pass | P1 | `src/test/java/roomescape/service` |
| acceptance_missing_flows | Important user/admin/manager flows are visible in acceptance tests | Added signup-login, admin setup, self waiting rejection, manager cancel coverage | pass | P2 | acceptance tests |
| verification | Targeted then full verification passes | Both targeted test command and `./gradlew clean test` passed | pass | P1 | this run note |

## Skill Changes

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/domain-modeling.md`
- Category: domain_invariants
- Change: Added rule to validate single-object invariants at domain construction time and keep persistence-dependent rules outside constructors.
- Reason: The generated code previously left object construction mostly unguarded and tested only behavior after valid construction.
- Validation target: pending next regeneration run.

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/testing-style.md`
- Category: test_layering
- Change: Added rules for domain construction tests, JPA slice repository tests, WebMvc slice controller tests, full HTTP test naming, service unit tests, and fixed Clock.
- Reason: The generated test suite overused full Spring context tests and named full HTTP tests as controller integration tests.
- Validation target: pending next regeneration run.

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/review-rubric.md`
- Category: review_rubric
- Change: Added review checks for domain construction invariants, persistence-rule boundary, test slices, HTTP test naming, and service unit coverage.
- Reason: These gaps were found only through post-generation audit.
- Validation target: pending next regeneration run.

## Follow-Up

- Keep: acceptance/E2E tests for final user workflows and Spring integration tests for transaction/persistence behavior.
- Fix before next run: none currently known.
- Add to skill/reference: completed in this run.
- Next benchmark: rerun a generation benchmark and check whether the skill now creates the layered test structure without manual correction.
