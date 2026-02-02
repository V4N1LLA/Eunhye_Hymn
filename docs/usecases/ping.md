# Use Case: Ping Health Check

## Goal
- Provide a lightweight endpoint to verify API availability.

## Primary Actor
- API client (admin web or mobile).

## Preconditions
- API service is running.

## Happy Path
1. Client sends `GET /ping`.
2. API returns a success envelope with `{ "ok": true }`.

## Alternate Flows
- If the server is unavailable, the client receives a network error.

## Data Touched
- None.

## API References
- `GET /ping`
