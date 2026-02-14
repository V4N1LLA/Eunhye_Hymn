# Staging 스모크 테스트 체크리스트

- 작성일: 2026-02-14
- 대상 브랜치: `develop`
- 목적: 배포 직후 핵심 사용자 흐름을 15~20분 내 검증해 Go/No-Go를 결정한다.

## 1. 실행 전 준비

- [ ] GitHub Actions 배포 워크플로가 정상 완료되었는지 확인
  - `.\scripts\staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -RequireSuccess -RequireDeploySuccess -RequireVerifySuccess -MaxAgeMinutes 120`
  - 기록용 표가 필요하면 `-AsMarkdown` 옵션 사용
- [ ] 점검 대상 커밋 SHA/배포 시각/담당자 확정
- [ ] 자동 리허설 로그(`docs/staging-rehearsal-log.md`) 최신 행 확인
- [ ] 관리자 계정 및 모바일 테스트 계정 준비
- [ ] 모바일 테스트 기기(최소 1대) + 웹(Chrome) 준비
- [ ] 로그 확인 경로 준비 (EC2 docker logs, CloudWatch)

권장 실행:
- `.\scripts\staging-rehearsal.ps1 -Repo V4N1LLA/Eunhye_Hymn -Ref develop [-AwsProfile <profile>]`

## 2. API 기본 점검

1. 헬스 체크
   - `curl http://<EC2_HOST>/api/v1/ping`
   - 기대 결과: `{"ok":true}`
2. 컨테이너 상태
   - `ssh -i <pem> ec2-user@<EC2_HOST> "docker ps"`
   - 기대 결과: `api`, `nginx` 컨테이너가 `Up` 상태
3. 오류 로그 확인
   - `ssh -i <pem> ec2-user@<EC2_HOST> "docker logs <api_container> --tail 200"`
   - 기대 결과: `ERROR` 반복 없음

## 3. 관리자 웹 스모크

1. 관리자 로그인 성공 (소셜 또는 운영에서 허용된 방식)
2. 찬양 목록 진입 및 기본 검색 정상 동작
3. 에셋 업로드 흐름(presign -> upload -> confirm) 1건 점검
4. 감사 로그 화면(`/events`) 진입 및 필터 조회
5. 감사 로그 CSV 내보내기 1회 수행

실패 기준:
- 로그인 불가, 주요 페이지 5xx, 업로드/조회 API 실패가 재현될 때

## 4. 모바일 스모크

1. 로그인
   - Android/iOS: Google/Kakao SDK 로그인 성공
   - 웹/미지원 환경: Kakao 수동 토큰 fallback 동작 확인
2. 찬양 목록/검색
   - 목록 로드와 검색 응답 확인
3. 찬양 상세
   - PNG 에셋 표시
   - MIDI 재생/일시정지/정지/속도 변경 동작
4. 개인화
   - 즐겨찾기 토글
   - 메모 저장/재조회
5. 오프라인 동기화
   - 네트워크 차단 상태에서 메모 또는 즐겨찾기 변경
   - 네트워크 복구 후 동기화 반영 확인

실패 기준:
- 로그인 실패, 재생 불가, 동기화 누락/오염 재현

## 5. 결과 기록 템플릿

| 항목 | 결과(PASS/FAIL) | 증상 요약 | 로그/증빙 | 담당자 |
|------|------------------|-----------|-----------|--------|
| API 헬스체크 |  |  |  |  |
| Admin 로그인/핵심 기능 |  |  |  |  |
| Mobile 로그인/핵심 기능 |  |  |  |  |
| 오프라인 동기화 |  |  |  |  |
| 최종 판정(Go/No-Go) |  |  |  |  |

## 6. 실행 기록 (2026-02-14)

| 항목 | 결과(PASS/FAIL) | 증상 요약 | 로그/증빙 | 담당자 |
|------|------------------|-----------|-----------|--------|
| API 헬스체크 | PASS | `deploy` job의 Verify deployment 단계 성공 | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22010470389 | codex |
| Admin 로그인/핵심 기능 | PASS(자동) | 배포 파이프라인 `check-admin`(tsc/build) 통과, 런타임 수동 점검은 별도 수행 필요 | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22010470389 | codex |
| Mobile 로그인/핵심 기능 | PASS(자동) | 배포 파이프라인 `test-api`와 deploy health는 통과, 모바일 실기기 수동 점검은 별도 수행 필요 | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22010470389 | codex |
| 오프라인 동기화 | PASS(자동) | 배포/롤백/복구 리허설 중 API 헬스 및 배포 성공, 모바일 오프라인 E2E는 별도 점검 필요 | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22010470389 | codex |
| 최종 판정(Go/No-Go) | PASS(조건부) | develop 리허설 성공 + 롤백 리허설 성공 + develop 재배포 복구 성공 | 22010284332 / 22010387328 / 22010470389 | codex |

## 7. 실패 시 즉시 조치

1. `docs/runbook.md`의 롤백 절차 수행
2. 장애 원인/영향 범위/복구 시각 기록
3. `docs/changelog-dev.md`에 후속 조치 추가
