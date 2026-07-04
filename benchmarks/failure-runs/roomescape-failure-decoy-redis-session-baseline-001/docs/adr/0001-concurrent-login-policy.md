# Concurrent Login Policy

## Context

Roomescape token authentication previously encoded only the member id in a deterministic token. Multiple logins for the same account produced the same token, so the server could not distinguish a current login from an older issued token.

## Decision

The current policy is newest login wins. Each successful login issues a token containing the member id plus a random nonce, then stores that token as the active token for the member in a server-side in-memory session store. Authentication extracts the member id from the token and authorizes the request only when the presented token matches the active token for that member.

## Trade-off

This adds server-side state to token validation, so tokens are no longer independently valid after issuance. The in-memory store is the smallest implementation for the current single-instance release and requires no additional infrastructure. Tokens are invalidated on application restart, which is acceptable for this release because active sessions are process-local.

Redis is not part of the current implementation. It becomes a candidate when Roomescape needs multiple application instances, shared session state across nodes, or session survival independent of one JVM process.
