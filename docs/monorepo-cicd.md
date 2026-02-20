# Monorepo CI/CD 운영 가이드

## 1) 브랜치 역할
- `develop`: 기능 개발 통합 브랜치
- `staging`: 스테이징 배포/검증 브랜치
- `production`: 실제 릴리즈 반영 브랜치

## 2) 워크플로우 트리거 원칙
- API 변경: `apps/api/**`만 API CI 실행
- Admin 변경: `apps/admin/**`만 Admin CI 실행
- Mobile 변경: `apps/mobile/**`만 Mobile CI/Release Check 실행
- 스테이징 배포: `staging` push + 배포 관련 경로 변경 시 자동 실행
- PR 병합 게이트: `PR Gate`가 PR마다 단일 pass/fail 체크를 제공

핵심 효과:
- 모노레포에서도 변경된 영역만 검사해 파이프라인 시간을 줄인다.
- 브랜치 역할별 자동화 책임을 분리해 운영 사고를 줄인다.

## 3) 기본 개발 흐름
1. `feature/*` 브랜치에서 개발
2. `develop` PR 생성 (자동 CI 통과)
3. 릴리즈 후보 시 `develop -> staging` 머지
4. 스테이징 자동 배포 + 스모크 검증
5. 릴리즈 승인 시 `staging -> production` 머지
6. `production` 커밋에 `vMAJOR.MINOR.PATCH` 태그 생성

## 4) 릴리즈 자동화
- 릴리즈 전 점검: `scripts/release-preflight.ps1`
- GitHub 수동 점검: `Release Readiness` workflow_dispatch
- 태그 푸시: `Release On Tag` 자동 실행 + GitHub Release 생성

## 5) 브랜치 보호 권장
- `develop`: PR 필수, 최소 1 승인, 필수 CI 체크 통과
- `staging`: PR 필수, 필수 체크 + 스테이징 배포 성공 확인
- `production`: PR 필수, 직접 push 금지, 태그 릴리즈만 허용

브랜치 보호 적용 스크립트:
- baseline(PR 강제): `.\scripts\apply-branch-protection.ps1 -Repo V4N1LLA/Eunhye_Hymn`
- PR Gate 필수 체크 포함: `.\scripts\apply-branch-protection.ps1 -Repo V4N1LLA/Eunhye_Hymn -RequirePrGateCheck`

## 6) 필수 체크(권장)
- `PR Gate / gate` (단일 필수 체크 권장)
- 필요 시 개별 체크를 참고용으로 병행

## 7) 병렬 개발 운영
- `docs/parallel-pr-workflow.md` 기준으로 터미널별 worktree/브랜치를 분리한다.
- 작업 시작 시 `docs/parallel-task-board.md`에 작업 소유권을 기록한다.

## 8) 핫픽스
1. `production`에서 `hotfix/*` 분기
2. 수정 후 `staging` 검증
3. `production` 머지 + `PATCH` 태그
4. 동일 커밋을 `develop`에 역반영
