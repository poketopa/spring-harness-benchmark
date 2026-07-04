# Diagnostics Checklist

Use this when the output feels inconsistent, too complex, too simple, or unlike the intended convention.

## Under-Engineered Output

- Service contains repeated domain-rule `if` statements.
- Request DTO reaches DAO/Repository.
- Controller decides business policy.
- Domain rule cannot be tested without Spring/DB.
- JDBC/Spring technical exception reaches API handling.

Repair by moving only the repeated or object-owned rule into domain behavior, not by adding broad architecture.

## Over-Engineered Output

- Every endpoint receives command, assembler, facade, interface, and policy layers.
- Value objects are wrappers without behavior.
- Repository interface has one implementation and no persistence-boundary reason.
- Domain facade duplicates application service.
- Tests are mostly mocks and call-count checks.

Repair by deleting the abstraction unless a trigger from the references exists.

## Trigger Checks

This skill should trigger for:

- Spring feature/usecase implementation.
- Controller-Service-Domain-Repository-Test generation.
- Spring bug fix with reproducible failure.
- Refactoring Service-heavy domain logic while implementing behavior.

This skill should not trigger for:

- Concept explanation only.
- General code review only.
- Commit message generation.
- Non-Spring frontend or documentation-only edits.

## Change Rule

Do not add a new convention because it sounds useful. Add or modify a rule only when:

- a benchmark shows better results,
- the same failure repeats,
- trigger verification shows a false positive/negative,
- review finds a recurring layer/domain/test gap.
