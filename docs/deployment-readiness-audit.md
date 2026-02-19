# 배포 준비도 점검 리포트

- 작성일: 2026-02-19
- 점검 브랜치: `develop`
- 점검 커밋: `e877f2f`
- 점검 목적: 현재 코드/문서/배포 파이프라인이 실제 운영 전환 가능한 수준인지 확인

## 1. 결론 요약

- 로컬 개발/검증: **Go**
- 스테이징 자동 배포: **Go**
- 스테이징 운영 전환: **Conditional Go** (수동 스모크 정례화 필요)
- 모바일 스토어 실배포(Android/iOS): **No-Go** (워크플로우 준비 완료, 실 publish 미실행)
- 프로덕션 공개 배포: **No-Go**

## 2. 실행 검증 근거

### 2.1 최신 GitHub Actions 실행

- `deploy-staging.yml`: 성공
  - run `22161011643`
  - title: `Merge pull request #98 from V4N1LLA/feat/invite-code-case-compat`
  - time: `2026-02-18T22:56:34Z`
- `mobile-release-check.yml`: 성공
  - run `22158976340`
  - title: `Merge pull request #97 from V4N1LLA/feat/local-verification-flow`
  - time: `2026-02-18T21:48:28Z`
- `mobile-ci.yml`: 성공
  - run `22158976375`
- `api-ci.yml`: 성공
  - run `22161011631`

### 2.2 배포/릴리즈 파이프라인 구성 상태

- 스테이징 배포 파이프라인
  - API 테스트 -> Admin 빌드 -> ECR push -> EC2 배포/verify 흐름 구성 완료
  - 파일: `.github/workflows/deploy-staging.yml`
- 모바일 릴리즈 체크
  - Android release APK 빌드 검증 구성 완료
  - 파일: `.github/workflows/mobile-release-check.yml`
- 모바일 스토어 readiness
  - Android signed AAB + Google Play upload 모드
  - iOS no-codesign build + TestFlight upload 모드
  - 파일: `.github/workflows/mobile-store-release.yml`

### 2.3 로컬 재현성 보강 상태

- 신규 스크립트 기반 로컬 검증 플로우 추가
  - `scripts/local-bootstrap.ps1`
  - `scripts/local-verify.ps1`
  - `scripts/run-mobile-emulator.ps1`
- 관련 운영 가이드 추가
  - `docs/TEAM_LOCAL_DEVELOPMENT.md`

## 3. 리스크/미완료 항목

1. 스테이징 수동 스모크 정례화
- 배포 성공과 별개로 Admin/Mobile 런타임 검수 로그를 주기적으로 누적해야 함

2. 모바일 계정/온보딩 실서비스 경로
- 회원가입은 UI 안내 수준
- 모바일 ID/PW 로그인은 dev-login 검증 경로 의존
- 온보딩 정보는 서버가 아닌 로컬 저장 기반

3. 스토어 실배포 이력 부재
- `mobile-store-release.yml`는 준비되어 있으나 실제 publish run 근거가 아직 없음

## 4. 실배포 전 필수 체크리스트

- [ ] 스테이징 수동 스모크 정례 수행 및 `docs/staging-smoke-log.md` 누적
- [ ] 모바일 계정/회원가입/온보딩 정책 확정(서버 연동 포함)
- [ ] Android `play_upload`, iOS `testflight` 모드 리허설 실행
- [ ] 프로덕션 도메인/HTTPS/모바일 네트워크 정책 확정
- [ ] 운영 승인 절차(Go/No-Go) 문서화 고정

## 5. 현재 판단

- Staging 운영 검증: **Conditional Go**
- Production 배포: **No-Go**
- Mobile Store publish: **No-Go**

## 6. 관련 문서

- `current_update.md`
- `docs/current-usable-scope.md`
- `docs/changelog-dev.md`
- `docs/runbook.md`
- `docs/staging-smoke-checklist.md`
- `docs/staging-smoke-log.md`
- `docs/TEAM_LOCAL_DEVELOPMENT.md`
