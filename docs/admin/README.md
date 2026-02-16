# 관리자 문서

관리자 웹의 정보 구조, 화면 흐름, 운영 가이드를 정리한다.

## 추천 섹션
- 관리자 정보 구조
- 찬송가 관리 흐름
- 에셋 업로드(프리사인)
- 초대 코드/권한 관리
- 감사 로그 및 분석 화면

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

## 감사 로그/분석 화면

- 경로: `/events`
- API: `GET /api/v1/admin/events`
- 필터: `eventType`, `userId`, `hymnId`, `from`, `to`, `limit`
- 페이지네이션: `page`, `size`
- 분석: `summaryDays` 기준 최근 N일 이벤트 타입별 집계
- 내보내기: `GET /api/v1/admin/events/export` CSV 다운로드

### 사용 포인트
1. 운영 이슈 시간대의 이벤트를 `from`/`to`로 좁혀 조회한다.
2. 특정 사용자/찬양 문제를 `userId`/`hymnId` 필터로 추적한다.
3. `summaryDays`를 1/7/30일로 바꿔 이벤트 흐름 변화를 비교한다.
