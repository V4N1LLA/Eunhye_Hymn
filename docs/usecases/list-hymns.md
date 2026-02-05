# 유스케이스: 찬송가 목록 조회

## 목표
- 공개 목록에서 활성화된 찬송가만 노출한다.

## 접근 조건
- 인증 불필요.

## 처리 흐름
1. `enabled = true`인 찬송가만 조회한다.
2. 최소 필드만 반환한다(id, title, number, tags).

## 응답 예시
```json
[
  { "id": "hymn_001", "title": "Amazing Grace", "number": "123", "tags": "advent" }
]
```
