# Architecture Principles

## Goal

Generate Spring code that lands between the owner's current style and the coach sample.

- Keep personal/learning projects small and readable.
- Strengthen domain responsibility and layer boundaries where they affect repeated quality.
- Avoid copying complex architecture unless the use case needs it.
- Never add architecture or design that is not actually needed.
- Prefer code that is simple and still object-oriented: responsibilities should sit on the right object, not in a large procedural service.

## Decision Bias

Prefer Hybrid rules:

- Use simple request DTO to Service flow for simple CRUD.
- Introduce command, value object, domain collection, policy, repository interface, or extra test slice only when the trigger is present.
- Keep decisions repeatable and reviewable rather than maximizing theoretical purity.
- When in doubt, choose the smallest structure that preserves clear responsibility boundaries.
- If the same responsibility or orchestration appears two or more times, treat that as the main signal to extract it.

## Over-Engineering Signals

- Every endpoint has command/assembler/facade even when input is direct.
- Application service and domain facade do the same job.
- Value objects wrap simple fields with no validation or behavior.
- Repository interfaces exist only because interfaces feel cleaner.
- Tests mock internal calls instead of checking behavior.
- Response types split into deep hierarchies before state-specific fields actually create confusion.
- Convenience dependencies such as Lombok are added only because the project already uses them or the user explicitly wants that convention.

## Under-Engineering Signals

- Controller creates domain objects or decides business rules.
- Service passes request DTO or command to DAO/Repository.
- Service repeats the same domain rule `if` more than once.
- Domain object cannot be tested without Spring/DB.
- Technical persistence exceptions leak to API responses.
- One large Service owns rules that a domain object or simple domain collection can express clearly.

## Portfolio Note

This project is a harness project: the measurable outcome is not one perfect implementation, but repeated Spring outputs that converge on the same convention for the same requirement shape.
