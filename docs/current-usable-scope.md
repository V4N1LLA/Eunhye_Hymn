# 현재 사용 가능 범위 정리

- 작성일: 2026-02-20
- 기준 브랜치: `develop`
- 기준 커밋: `6a00b68`
- 기준 문서: `README.md`, `docs/api-contract.md`, `docs/data-model.md`, `docs/changelog-dev.md`

## 1. 요약

현재 프로젝트는 로컬 개발/검증 기준으로 다음 범위를 즉시 사용할 수 있다.

- 백엔드 API: 인증, 찬양/에셋, 멤버 개인화, 운영 로그, AI 추천
- 관리자 웹: 찬양/에셋/사용자/초대코드/감사로그/개인정보 변경 요청/AI 추천 화면
- 모바일 앱: 인증 플로우(초대코드+SMS), 찬양 탐색/상세, AI 추천, 개인화, 오프라인 fallback
- 스테이징: AWS 배포 파이프라인/운영 스크립트 기준으로 점검 가능

## 2. 로컬에서 바로 검증 가능한 범위

### 2.1 관리자 웹 (Admin)
- 찬양 관리: 목록/생성/수정/삭제, 검색/필터
- 에셋: presign -> upload -> confirm, 삭제
- 사용자: 조회/생성/수정/삭제(soft delete)
- 초대코드: 생성/조회/비활성화
- 개인정보 변경 요청: 대기 목록 조회, 승인/반려
- 감사 로그: 필터/페이지네이션/요약, 동기 CSV + 비동기 CSV 작업/상태/다운로드/지표
- AI 추천: 상황 입력 기반 추천 확인

### 2.2 백엔드 API
- 인증:
  - `POST /auth/social`
  - `POST /auth/signup`
  - `POST /auth/login`
  - `POST /auth/admin/login`
  - `POST /auth/invite/validate`
  - `POST /auth/sms/request`
  - `POST /auth/sms/verify`
  - `POST /auth/refresh`
  - `POST /auth/logout`
  - `POST /auth/withdraw`
  - `DELETE /me/account`
- 멤버:
  - `GET/PUT /me/profile`
  - `POST /me/profile-change-requests`
  - `GET /me/profile-change-requests/latest`
  - 즐겨찾기/메모/히스토리
- 관리자:
  - `/admin/hymns`, `/admin/assets/*`, `/admin/users`, `/admin/invite-codes`
  - `/admin/events/*`
  - `/admin/profile-change-requests`
- AI:
  - `POST /ai/hymn-recommendations`

### 2.3 모바일 앱
- 로그인/검증: `Login -> InviteCode -> Phone -> SMS -> Home`
- 찬양: 목록/검색/태그필터, 상세(PNG+MIDI)
- AI 추천: 상황 기반 추천
- 개인화: 프로필, 즐겨찾기, 메모, 히스토리
- 오프라인: 캐시 fallback + 오프라인 변경 동기화

## 3. 스테이징 기준 상태

- 배포/검증 워크플로 구성 완료:
  - `.github/workflows/deploy-staging.yml`
  - `.github/workflows/api-ci.yml`
  - `.github/workflows/admin-ci.yml`
  - `.github/workflows/mobile-ci.yml`
  - `.github/workflows/mobile-release-check.yml`
  - `.github/workflows/mobile-store-release.yml`
- 운영 점검 스크립트:
  - `scripts/staging-preflight.ps1`
  - `scripts/staging-ops-cycle.ps1`
  - `scripts/staging-rehearsal.ps1`
  - `scripts/staging-latest-status.ps1`

## 4. 현재 우선 과제

- 스테이징 수동 스모크를 주기적으로 누적 기록
- 운영 시크릿 로테이션/권한 점검 정례화
- AI 추천 운영 지표(성공률/지연/비용) 관측 지표 확정

## 5. 빠른 실행 체크리스트

1. 로컬 스택
   - `.env` 준비: `cp .env.example .env` (PowerShell: `Copy-Item .env.example .env`)
   - `docker compose -f infra/docker/docker-compose.yml up -d`
   - `curl http://localhost:8080/api/v1/ping`
2. Admin
   - `cd apps/admin && npm install && npm run dev`
3. Mobile
   - `.\scripts\run-mobile-emulator.ps1 -Environment local -DeviceId emulator-5554`
