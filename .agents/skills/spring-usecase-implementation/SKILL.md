---
name: spring-usecase-implementation
description: Java/Spring 요구사항 구현, Spring 기능 추가/수정, Controller-Service-Domain-Repository-Test 작성, Spring 테스트 실패 수정 요청 시 사용. 기존 Spring 프로젝트 구조를 읽고 실용적 중간 컨벤션에 따라 계층 경계, 도메인 책임, DTO, 예외, 트랜잭션, 테스트를 함께 설계하고 검증한다. 단순 Spring 개념 설명, 일반 코드 리뷰, 커밋 메시지 작성에는 사용하지 않는다.
---

# Spring Usecase Implementation

## Operating Goal

Implement Spring use cases so repeated prompts produce code close to the owner's conventions: simpler than the coach sample, but with stronger domain responsibility, clearer layer boundaries, and behavior-focused tests than the original personal sample.

Do not copy the coach architecture mechanically. Apply only the smallest structure that protects the current use case.
Do not introduce architecture or design that is not actually needed. Good code is simple while still assigning responsibilities to the right objects.

## Workflow

1. Inspect the existing project before designing.
   - Identify package structure, persistence style, test style, exception style, validation dependencies, and existing API contracts.
   - Preserve existing public API contracts and assignment/acceptance-test expectations.

2. Classify the use case.
   - Simple CRUD/input forwarding: keep the structure small.
   - Input composition, auth/path/query mixing, repeated domain rules, or multi-object policy: introduce the smallest extra structure needed.
   - If the requirement is ambiguous enough that multiple reasonable implementations would differ materially, ask the user for the decision criterion before implementing.

3. Read references only as needed.
   - Controller/Service/Repository or DTO work: read `references/controller-service-repository.md`.
   - Domain behavior, value object, policy, or exception decisions: read `references/domain-modeling.md`.
   - Test design or bug fix verification: read `references/testing-style.md`.
   - Java class or method organization work: read `references/java-class-ordering.md`.
   - Before finalizing: read `references/review-rubric.md`.
   - If output feels inconsistent or over/under-engineered: read `references/diagnostics-checklist.md`.
   - For the overall philosophy: read `references/architecture-principles.md`.

4. Implement in this order.
   - Domain behavior and invariants first when a domain rule exists.
   - Application/Service orchestration next: repository lookup, transaction, input composition, current time, and flow control.
   - Controller and DTO last: HTTP contract, validation, status codes, response mapping.
   - Repository/persistence changes with no web DTO leakage.
   - Tests covering the main user flow, important failure flow, and pure domain rules.
   - Class member ordering that keeps public flow followed by the private helpers it first calls.

5. Verify.
   - Run the narrowest relevant test first.
   - Then run broader project checks when practical.
   - If fixing a bug, reproduce it with a failing test/log/command before claiming it is fixed.
   - Do not delete or weaken tests unless the user changed the requirement.

6. Document only when it is part of the task or the project already requires it.
   - Do not update README by default.
   - Prefer ADRs for meaningful technical decisions.

## Hard Rules

- Controller must not create domain objects for core rules or decide domain policy.
- Request DTO or application command must not be passed through to DAO/Repository.
- Domain objects must not call `LocalDateTime.now()` or depend on Spring/web/persistence APIs.
- Repository/JdbcTemplate technical exceptions must not leak to Controller responses.
- Use a single project-wide error response style.
- Prefer behavior/result-state assertions over mock interaction assertions.
- Delete methods that are not used by the generated use case unless they are part of an existing public contract.

## Evolution Rule

Change this skill only when real usage shows a repeated failure, benchmark improvement, trigger issue, or review gap. Record meaningful project-level changes in the Obsidian project log and ADRs when they affect the skill direction.
