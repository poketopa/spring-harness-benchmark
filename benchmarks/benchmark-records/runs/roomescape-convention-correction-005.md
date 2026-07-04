# roomescape-convention-correction-005

## Summary

- Date: 2026-07-03
- Mission: roomescape-reservation-waiting
- Cycle: convention-correction
- Requirement: user feedback after cycle2-regen-v2 convention review
- Baseline: benchmarks/roomescape-jpa-auth-cycle2-regen-v2
- Target project: benchmarks/roomescape-jpa-auth-cycle2-regen-v2
- Skill: spring-usecase-implementation
- Started at: not captured
- Finished at: 2026-07-03T16:44:36+09:00
- Duration minutes: not captured
- Result status: pass

## Prompt

```text
1. DomainValidator가 따로 존재하지 않고 각각의 도메인이 알아서 처리하는 방식이 직관적이다.
2. 서비스를 유스케이스 단위로 분리한 점은 좋다.
3. 서비스 테스트와 복잡한 인수 테스트에서 작업 단위 경계가 더 명확하면 좋겠다. given/when/then 주석을 넣는 것이 좋을까?
```

## Expected Decision Rules

- [x] domain_validation_ownership: simple domain invariant validation stays inside each domain class.
- [x] no_generic_domain_validator: no broad `DomainValidator` utility by default.
- [x] service_boundary: use-case based service split remains.
- [x] test_flow_clarity: service/mock tests and long acceptance tests show given/when/then boundaries.
- [x] verification: run targeted tests before full verification.

## Generated Result

### Main Files

- Domain:
  - Removed `DomainValidator`.
  - Moved invariant validation into `Member`, `Theme`, `ReservationTime`, `Reservation`, and `Waiting`.
- Tests:
  - Added `// given` and `// when & then` to mock-based service tests.
  - Added `// given`, `// when`, `// then` to longer acceptance flows.
  - Left short domain/repository tests compact.
- Skill/reference:
  - Updated domain modeling rules to avoid generic domain validation utility by default.
  - Updated testing style rules for conditional given/when/then comments.

### Verification

- Narrow test: `./gradlew test --tests 'roomescape.domain.*Test' --tests 'roomescape.service.*Test' --tests 'roomescape.ReservationAcceptanceTest' --tests 'roomescape.WaitingAcceptanceTest'`
- Narrow result: pass
- Full verification: `./gradlew clean test`
- Result: pass

## Convention Comparison

| Category | Expected | Observed | Status | Severity | Evidence |
| --- | --- | --- | --- | --- | --- |
| domain_validation_ownership | Domain objects own their own invariant checks | Each touched domain now has private validation methods | pass | P1 | `src/main/java/roomescape/domain` |
| no_generic_domain_validator | No broad `DomainValidator` utility by default | `DomainValidator` was deleted | pass | P1 | `src/main/java/roomescape/domain` |
| service_boundary | Use-case based service split remains | Reservation, waiting, promotion, rank, and my-reservation services remain separated | pass | P1 | `src/main/java/roomescape/service` |
| test_flow_clarity | Complex tests expose setup/action/assertion boundaries | Service mock tests and long acceptance tests use given/when/then comments | pass | P2 | `src/test/java/roomescape` |
| verification | Targeted then full verification passes | Both targeted tests and `./gradlew clean test` passed | pass | P1 | this run note |

## Skill Changes

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/domain-modeling.md`
- Category: domain_validation_ownership
- Change: Added rule to keep simple invariant validation inside the owning domain class and avoid generic `DomainValidator` by default.
- Reason: User found per-domain validation more intuitive than a shared utility.
- Validation target: pending next regeneration run.

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/testing-style.md`
- Category: test_flow_clarity
- Change: Added conditional rule to use given/when/then comments for service/mock tests and long acceptance flows, not short domain/repository tests.
- Reason: User wanted clearer work-unit boundaries in complex tests without adding noise everywhere.
- Validation target: pending next regeneration run.

- Skill/reference file: `.agents/skills/spring-usecase-implementation/references/review-rubric.md`
- Category: review_rubric
- Change: Added checks for domain-owned validation and clear test flow boundaries.
- Reason: These are now explicit owner conventions.
- Validation target: pending next regeneration run.

## Follow-Up

- Keep: use-case service split in `cycle2-regen-v2`.
- Fix before next run: none known.
- Add to skill/reference: completed in this run.
- Next benchmark: manager authorization add-on regeneration from `cycle2-regen-v2`, or a compact audit if the user wants one more manual review pass first.
