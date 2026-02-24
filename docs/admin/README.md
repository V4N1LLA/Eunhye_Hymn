# 관리자 문서

관리자 웹의 정보 구조, 화면 흐름, 운영 가이드를 정리한다.

## 화면 인벤토리
- `/` 대시보드
- `/hymns`, `/hymns/new`, `/hymns/:id/edit` 찬양 관리
- `/assets/upload` 에셋 업로드
- `/users` 사용자 관리
- `/invite-codes` 초대코드 관리
- `/profile-change-requests` 개인정보 변경 요청 승인/반려
- `/events` 감사 로그/분석/CSV export
- `/ai/recommendations` AI 상황 기반 찬송 추천

## 에셋 업로드(프리사인)

관리자 웹에서 에셋(PNG, MIDI)을 업로드할 때는 프리사인 URL 발급 후 업로드하고,
마지막으로 확인(Confirm) 요청을 통해 서버에 등록한다.

### 관리자 업로드 절차
1. 찬송가 ID 입력
2. 파일 선택
3. Presign
4. Upload
5. Confirm

### 규칙
- part를 지정하지 않으면 ALL로 처리된다.
- objectKey 접두사는 hymns/{hymnId}/{type}/{part}/ 형식을 따른다.
- 동일한 hymnId/type/part 조합은 최신 업로드만 유지되며 Confirm 시 기존 항목이 교체된다.

### 트러블슈팅
- invalid_object_key 오류가 발생하면 찬송가 ID, 타입, 파트가 objectKey 경로와 일치하는지 확인한다.

## 개인정보 변경 요청 운영

- 경로: `/profile-change-requests`
- API:
  - `GET /api/v1/admin/profile-change-requests?status=PENDING|APPROVED|REJECTED`
  - `PATCH /api/v1/admin/profile-change-requests/{id}`
- 승인 요청 예시:
  ```json
  { "action": "APPROVE" }
  ```
- 반려 요청 예시:
  ```json
  { "action": "REJECT", "rejectReason": "이름 확인 필요" }
  ```

## 감사 로그/분석 화면

- 경로: `/events`
- API: `GET /api/v1/admin/events`
- 필터: `eventType`, `userId`, `hymnId`, `from`, `to`, `limit`
- 페이지네이션: `page`, `size`
- 분석: `summaryDays` 기준 최근 N일 이벤트 타입별 집계
- 내보내기:
  - `GET /api/v1/admin/events/export` (동기 CSV)
  - `POST /api/v1/admin/events/export-jobs` (비동기 작업 생성)
  - `GET /api/v1/admin/events/export-jobs/{jobId}` (상태)
  - `GET /api/v1/admin/events/export-jobs/{jobId}/download` (다운로드)
  - `GET /api/v1/admin/events/export-jobs/metrics` (운영 지표)

### 사용 포인트
1. 운영 이슈 시간대의 이벤트를 `from`/`to`로 좁혀 조회한다.
2. 특정 사용자/찬양 문제를 `userId`/`hymnId` 필터로 추적한다.
3. `summaryDays`를 1/7/30일로 바꿔 이벤트 흐름 변화를 비교한다.

## AI 추천 화면

- 경로: `/ai/recommendations`
- API: `POST /api/v1/ai/hymn-recommendations`
- 입력: 상황(`situation`), 추천 개수(`maxResults`)
- 출력: 추천 곡 목록 + 추천 사유 + 후보군 수(`candidateCount`)
