# Release Management (SemVer + GitHub)

## 목적
- 스테이징 검증이 끝난 빌드만 릴리즈한다.
- 릴리즈 버전은 `vMAJOR.MINOR.PATCH` 태그로 GitHub에 영구 기록한다.
- 릴리즈 시점마다 GitHub Release를 생성해 변경 이력을 남긴다.

## 버전 규칙 (기본 SemVer)
- `MAJOR`: 하위 호환이 깨지는 변경
- `MINOR`: 하위 호환을 유지하는 기능 추가
- `PATCH`: 하위 호환을 유지하는 버그 수정

예시:
- `v1.0.0`
- `v1.1.0`
- `v1.1.1`

## 브랜치/배포 원칙
- 개발 통합: `develop`
- 스테이징 자동 배포: `staging` push 기반
- 릴리즈 기준 브랜치: `production`
- 모든 반영은 PR로만 진행한다 (직접 push 금지).

정책:
1. `develop`에서 개발/테스트를 완료한다.
2. `develop -> staging`으로 릴리즈 후보를 머지한다.
3. 스테이징 자동 배포 성공 + 스모크 테스트 완료를 확인한다.
4. `staging -> production`으로 반영 후 릴리즈 태그를 생성한다.
5. 태그 push 시 릴리즈 검증 워크플로우가 실행되고, 통과하면 GitHub Release가 생성된다.

## 릴리즈 전 점검
`scripts/release-preflight.ps1`를 사용한다.

```powershell
.\scripts\release-preflight.ps1 -Version 1.2.3 -Repo V4N1LLA/Eunhye_Hymn -Branch staging
```

GitHub에서 동일 점검을 실행하려면 `Release Readiness` 워크플로우를 수동 실행한다.

검증 항목:
- SemVer 형식(`1.2.3`)
- 태그 중복 여부(`v1.2.3`)
- 최신 스테이징 배포 성공 여부(배포/검증 단계 포함)
- 스테이징에 올라간 커밋 SHA 기준 API/Admin/Mobile CI 성공 여부

## 릴리즈 실행 절차
1. `production`에 릴리즈 대상 반영
2. 로컬에서 태그 생성

```powershell
git checkout production
git pull origin production
git tag -a v1.2.3 -m "Release v1.2.3"
git push origin v1.2.3
```

3. GitHub Actions `Release On Tag`가 자동 실행
4. 성공 시 GitHub Release 생성
5. `CHANGELOG.md`의 `Unreleased` 내용을 해당 버전 섹션으로 이동

## 권장 운영
- 각 릴리즈마다 반드시 태그 + GitHub Release를 함께 남긴다.
- 릴리즈 전에는 `docs/staging-smoke-checklist.md`와 `docs/staging-feedback-checklist.md`를 완료한다.
- hotfix도 동일하게 `PATCH` 버전으로 태깅한다.
