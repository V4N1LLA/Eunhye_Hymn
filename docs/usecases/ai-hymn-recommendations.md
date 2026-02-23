# AI Hymn Recommendations

- Primary user: member
- Endpoint: `POST /api/v1/ai/hymn-recommendations`
- Auth: required (`Bearer access token`)

## Request

```json
{
  "situation": "For early morning prayer with a calm mood",
  "maxResults": 3
}
```

- `situation`: required, natural language context
- `maxResults`: optional, `1..5` (default follows server config)

## Success response

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "f94b982f-f030-4440-b173-992bf07885fb",
        "number": "101",
        "title": "Grace Song",
        "tags": "grace,comfort",
        "reason": "Fits a calm prayer flow."
      }
    ],
    "requestedMaxResults": 3,
    "candidateCount": 25
  },
  "error": null
}
```

## Failure cases

- `401 unauthorized`: missing or invalid token
- `429 too_many_requests`: rate limiter blocks request
- `503 ai_unavailable`: Gemini key disabled/missing or upstream error

## Cost controls

- Candidate hymn list is capped (`AI_RECOMMEND_MAX_CANDIDATE_HYMNS`)
- Max output tokens are capped (`AI_GEMINI_MAX_OUTPUT_TOKENS`)
- Request timeout is short (`AI_GEMINI_READ_TIMEOUT_SECONDS`)

## Operational metrics

The API records Micrometer metrics for recommendation monitoring.

- `ai_recommend_requests_total{result=*}`
  - success/failure volume (`validation_error`, `too_many_requests`, `ai_unavailable`, `error`, `unexpected_error`)
- `ai_recommend_latency_seconds{result=*}`
  - end-to-end latency from controller entry to response/error
- `ai_recommend_fallback_total{result=*}`
  - count of requests where AI output was unusable and fallback recommendations were served
- `ai_recommend_candidate_count{result=*}`
  - candidate hymn count distribution per request
- `ai_recommend_response_items{result=*}`
  - actual response item count distribution

Recommended SLO baseline:

- success rate >= `99%` (excluding `validation_error`)
- p95 latency <= `2.0s`
- fallback ratio <= `5%`
