# roomescape-convention-correction-006

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: convention-correction
- Requirement: user feedback after concurrent login audit
- Baseline: benchmarks/roomescape-jpa-auth-concurrent-login-regen-v2
- Target project: benchmarks/roomescape-jpa-auth-concurrent-login-regen-v2
- Skill: spring-usecase-implementation
- Started at: not captured
- Finished at: 2026-07-03T17:39:38+09:00
- Duration minutes: not captured
- Result status: pass

## Prompt

```text
Review four findings after concurrent-login regeneration.
Decide whether each is an owner convention choice or something worth fixing.
Apply fixes for items that should definitely be improved.
```

## Expected Decision Rules

- [x] concurrent_session_race: simultaneous first login for the same member must not leak a DB unique violation as a 500 response.
- [x] real_concurrent_test: concurrent-login behavior needs an actual concurrent test, not only sequential token replacement.
- [x] token_provider_unit_test: custom token provider behavior should have direct unit coverage.
- [x] login_member_identity_assertion: `AuthService.authenticate(...)` tests should verify the returned member id, not only name.
- [x] verification: run targeted tests before full verification.

## Generated Result

### Main Files

- Production:
  - Added `MemberRepository.findByEmailForUpdate(...)` with `PESSIMISTIC_WRITE`.
  - Changed `AuthService.login(...)` to lock the member row while creating or renewing `AuthSession`.
  - Changed session persistence to `saveAndFlush(...)` so DB conflicts surface inside the service transaction.
- Tests:
  - Added `AuthTokenProviderTest`.
  - Updated `AuthServiceTest` to use id-bearing member fixtures and assert `LoginMember.id()`.
  - Added an actual simultaneous first-login test to `ConcurrentLoginAcceptanceTest`.
- Skill/reference:
  - Added stateful-auth session race guidance.
  - Added concurrent-test and token-provider-test guidance.
  - Added review checks for stateful auth races and custom token providers.

## Verification

- Narrow test: `./gradlew test --tests 'roomescape.auth.AuthTokenProviderTest' --tests 'roomescape.domain.AuthSessionTest' --tests 'roomescape.service.AuthServiceTest' --tests 'roomescape.repository.AuthSessionRepositoryIntegrationTest' --tests 'roomescape.ConcurrentLoginAcceptanceTest' --tests 'roomescape.MemberAuthenticationAcceptanceTest'`
- Narrow result: pass
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| concurrent_session_race | Same-member first login is serialized or conflicts are domain-handled | Login now locks the member row and flushes session renewal in service | pass | P1 | `AuthService`, `MemberRepository` |
| real_concurrent_test | Concurrent-login requirement has an actual concurrent test | `ConcurrentLoginAcceptanceTest` uses latches and verifies exactly one active token | pass | P1 | `ConcurrentLoginAcceptanceTest` |
| token_provider_unit_test | Token provider uniqueness parsing and invalid-token behavior are directly tested | `AuthTokenProviderTest` added | pass | P2 | `AuthTokenProviderTest` |
| login_member_identity_assertion | Auth service verifies returned login id and name | `AuthServiceTest` asserts `LoginMember.id()` and `name()` | pass | P2 | `AuthServiceTest` |
| verification | Targeted then full verification passes | Both targeted tests and `./gradlew clean test` passed | pass | P1 | this run note |

## Skill Changes

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/controller-service-repository.md`
- Category: stateful_auth_concurrency
- Change: Added rule to serialize same-subject session renewal and avoid leaking DB uniqueness conflicts.
- Validation target: pending next regeneration run.

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/testing-style.md`
- Category: concurrent_auth_testing
- Change: Added rule to use real concurrent tests for concurrency requirements and direct tests for custom token providers.
- Validation target: pending next regeneration run.

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/review-rubric.md`
- Category: review_rubric
- Change: Added review checks for stateful auth races, scoped login locks, real concurrent tests, and custom token provider tests.
- Validation target: pending next regeneration run.

## Follow-Up

- Keep the "new login wins" policy.
- Next run should verify that the skill naturally generates the member-row lock and actual concurrent login test without manual correction.
