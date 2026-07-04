# Requirement Variants

This directory contains requirement documents that preserve the same semantic requirements as the canonical mission documents while changing the writing style.

The goal is to test whether `spring-usecase-implementation` remains stable when prompts are realistic but not identical.

## Experiment Units

- `c1-waiting`: reservation waiting creation, cancellation, and my reservation/waiting read model
- `c2-combined`: reservation policy/error/change/cancel plus waiting approval behavior
- `manager-authz`: store manager authorization for own-store reservation management
- `concurrent-login`: preventing concurrent login by making the newest login authoritative

## Variant Types

- `narrative`: product-owner style prose
- `acceptance-criteria`: checklist or Given/When/Then style
- `api-contract`: endpoint, request, response, status, and error oriented
- `domain-rule`: domain invariant and state transition oriented
- `security-case`: threat/risk oriented authorization wording
- `operational-policy`: operations and session policy wording

## Semantic Control

Each variant must pass its unit checklist before it can be used in a benchmark run. A variant is invalid if it adds, removes, or weakens a required behavior.

The checklists live under `checklists/`.

## Output Targets

Generated implementations for this experiment should live under:

```text
benchmarks/robustness-runs/{target_name}
```

Run notes and CSV records still live under:

```text
benchmarks/benchmark-records/
```

