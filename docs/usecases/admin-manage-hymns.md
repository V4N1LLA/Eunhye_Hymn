# 유스케이스: 관리자 찬송가 관리

## 목표
- 관리자가 찬송가를 생성, 수정하고 비활성 항목까지 포함해 전체 목록을 조회한다.

## 접근 조건
- 관리자(ADMIN) 권한 필요.

## 처리 흐름
1. 요청 토큰의 권한이 ADMIN인지 확인한다.
2. 생성 요청은 필수 필드(title)를 검증하고 신규 찬송가를 저장한다.
3. 수정 요청은 전달된 필드만 업데이트하고 기존 값은 유지한다.
4. 목록 조회는 활성/비활성을 구분하지 않고 모두 반환한다.

## API 요약
- `POST /admin/hymns`
- `PATCH /admin/hymns/{id}`
- `GET /admin/hymns`

## 응답 예시
```json
{
  "id": "hymn_001",
  "title": "Amazing Grace",
  "number": "123",
  "tags": "advent",
  "enabled": true
}
```
