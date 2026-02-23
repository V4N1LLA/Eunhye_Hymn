# Eunhye Hymn 운영 Runbook (Staging)

## 1. 목적
- 스테이징 배포/장애 대응 절차를 표준화한다.
- 배포 승인(Go/No-Go) 근거를 동일한 형식으로 남긴다.

## 2. 전제 조건
- GitHub 저장소 접근 권한
- AWS 접근 권한 (ECR/EC2/CloudWatch)
- EC2 SSH 접속 가능
- `docs/admin/aws-free-tier-onboarding.md` 기준 인프라/시크릿 준비 완료

## 3. 참조 문서
- `docs/staging-smoke-checklist.md`
- `docs/staging-rehearsal-log.md`
- `docs/staging-smoke-log.md`
- `docs/deployment-readiness-audit.md`
- `docs/staging-admin-login.md`
- `infra/aws/README.md`
- `docs/SECRETS_MANAGEMENT.md`

## 4. 배포 체크리스트
- [ ] `staging` 최신 반영
- [ ] DB 마이그레이션 변경 유무 확인
- [ ] 환경 변수 파일 최신화 (`DB_*`, `JWT_*`, `INVITE_CODE`, `ADMIN_*`, `SMS_*`, `S3_*`, `AI_*`)
- [ ] 롤백 기준 버전(이전 이미지 태그) 확인
- [ ] 사전 점검 스크립트 통과
  - `.\scripts\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn [-AwsProfile <profile>] [-AutoLogin]`
  - AWS 세션 만료가 잦은 환경은 `-AutoLogin`을 기본으로 사용 (`aws sso login` 또는 `aws login` 자동 재시도)
- [ ] 배포 리허설 자동 실행(권장)
  - `.\scripts\staging-rehearsal.ps1 -Repo V4N1LLA/Eunhye_Hymn -Ref staging [-AwsProfile <profile>] [-AutoLogin]`
  - 로컬 확인만 필요하면 `-DryRun` 사용
- [ ] 운영 사이클 자동 점검(권장)
  - `.\scripts\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch staging -Owner <담당자> [-AutoLogin] [-WaitForCompletion]`
  - preflight + 최신 배포 게이트 + `docs/staging-smoke-log.md` 기록을 일괄 수행
- [ ] GitHub Actions 필수 Secrets 등록 확인
  - `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `ECR_REGISTRY`, `EC2_HOST`, `EC2_SSH_KEY`, `DEPLOY_ENV_FILE`
- [ ] GitHub Actions 배포 워크플로 최신 성공 이력 확인
  - `.\scripts\staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -RequireSuccess -RequireDeploySuccess -RequireVerifySuccess -MaxAgeMinutes 120`
  - 리허설 run 확인이 필요하면 `-Branch <브랜치>` 또는 `-Event workflow_dispatch` 옵션 사용
  - 문서/티켓 기록용 표 출력은 `-AsMarkdown` 옵션 사용
- [ ] 스모크 테스트 담당자/기기(Android/iOS/웹) 배정

## 5. 배포 절차
1. `develop` 변경을 `staging`에 머지
2. GitHub Actions 배포 워크플로 실행
   - 운영 반영: `staging` push 트리거
   - 리허설/선검증: `Deploy Staging` workflow_dispatch (`enable_awslogs=false` 권장)
   - 자동화 경로: `.\scripts\staging-rehearsal.ps1` 실행 시 preflight + workflow_dispatch + run 완료 대기 + 로그 기록을 일괄 수행
   - 최신 배포 상태 확인: `.\scripts\staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -RequireSuccess -RequireDeploySuccess -RequireVerifySuccess -MaxAgeMinutes 120`
3. EC2에서 컨테이너 상태 확인
   - `docker ps`
   - `docker logs <api_container> --tail 200`
4. 헬스체크
   - `curl http://<EC2_HOST>/api/v1/ping`
5. `docs/staging-smoke-checklist.md` 전 항목 수행
6. 결과 기록
   - 성공: 배포 시각, 커밋, 수행자, 체크 결과를 문서/티켓에 기록 (`docs/staging-rehearsal-log.md`, `docs/staging-smoke-log.md` 포함)
   - 실패: 즉시 롤백 후 장애 대응 절차로 전환

## 6. 롤백 절차
1. 최근 배포 커밋/이미지 태그 확인
   - `deploy-staging.yml`의 `workflow_dispatch`는 branch/tag ref만 지원한다. 커밋 SHA로 롤백 리허설이 필요하면 임시 브랜치를 만들어 실행한다.
   - 예시: `git branch tmp/staging-rollback-<sha12> <commit_sha>` → `git push origin tmp/staging-rollback-<sha12>`
2. 이전 안정 태그로 `deploy.sh` 재실행
3. `curl http://<EC2_HOST>/api/v1/ping` 및 핵심 화면 재확인
4. 원인/영향/복구 시각 기록 및 공유

## 7. 장애 대응
### 7.1 증상 확인
- API 5xx 지속
- 컨테이너 재시작 반복
- `/api/v1/ping` 실패
- 모바일 로그인/재생/동기화 실패 재현

### 7.2 1차 대응
1. 최근 배포 커밋/이미지 태그 확인
2. 롤백 기준 충족 시 즉시 롤백
3. CloudWatch/컨테이너 로그 수집
4. 영향 범위 및 사용자 공지 여부 판단

### 7.3 사후 조치
- RCA 문서화
- 재발 방지 항목 등록
- 관련 문서 업데이트
  - `docs/runbook.md`
  - `docs/staging-smoke-checklist.md`
  - `docs/changelog-dev.md`

## 8. 민감 정보 운영 원칙
- 민감 정보는 Git에 커밋하지 않는다.
- `.env`는 로컬 전용으로 사용하고, 실제 값은 Secrets Manager 또는 GitHub Actions Secrets로 관리한다.
- 로그/PR 본문에 토큰, 비밀번호, 키 전체값을 남기지 않는다.

## 9. 정기 점검 (권장)
- 주 1회 스테이징 배포 리허설 + 스모크 체크리스트 1회 수행
- 주 1회 `staging-ops-cycle.ps1 -WaitForCompletion -AutoLogin` 실행 결과를 기준으로 Go/Hold 근거를 `docs/staging-smoke-log.md`에 누적
- 월 1회 시크릿 교체 상태 점검
  - `.\scripts\staging-secret-rotation-audit.ps1 -Repo V4N1LLA/Eunhye_Hymn -MaxAgeDays 90 -IncludeMobileReleaseSecrets`
  - 결과가 `STALE` 또는 `MISSING`이면 즉시 교체/보완 후 `docs/changelog-dev.md`에 기록
- 월 1회 롤백 시나리오 점검
