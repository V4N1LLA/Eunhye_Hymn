# AGENTS.md

Practical operating guide for agentic coding assistants in this repository.
Use this as the default execution and style baseline.

## 1) Repository Snapshot

- Monorepo with three apps:
  - `apps/api`: Spring Boot (Java 17, Gradle wrapper)
  - `apps/admin`: React + TypeScript + Vite
  - `apps/mobile`: Flutter/Dart
- Infra and ops:
  - `infra/aws`: Terraform + deploy scripts
  - `infra/docker`: local docker-compose stack
- API base path is always `/api/v1`.
- Architectural intent is Clean Architecture (domain -> application -> presentation/adapters -> infrastructure).

## 2) Rule Sources Checked

- Checked for Cursor rules: `.cursor/rules/` and `.cursorrules` -> **not present**.
- Checked for Copilot rules: `.github/copilot-instructions.md` -> **not present**.
- Project-specific operational guidance exists in `CLAUDE.md` and `docs/WORK_CYCLE.md`.

## 3) Setup and Toolchain

- Java: 17
- Node.js: 20+
- npm: 10+
- Flutter/Dart: Flutter stable, Dart >= 3.4
- Preferred wrappers/tools:
  - API: `./gradlew` (or `gradlew.bat` on Windows)
  - Mobile on Windows/local CI parity: `scripts/flutterw.ps1`

## 4) Build / Lint / Test Commands

Run commands from repo root unless noted.

### 4.1 API (`apps/api`)

- Build/test (CI parity):
  - `cd apps/api && ./gradlew test --no-daemon --stacktrace`
- Run app locally:
  - `cd apps/api && ./gradlew bootRun`
- Run a single test class:
  - `cd apps/api && ./gradlew test --tests "com.eunhyehymn.presentation.controllers.AdminEventApiTest" --no-daemon --stacktrace`
- Run a single test method:
  - `cd apps/api && ./gradlew test --tests "com.eunhyehymn.presentation.controllers.AdminEventApiTest.adminCanListEventsAndSummary" --no-daemon --stacktrace`
- Alternative pattern filter:
  - `cd apps/api && ./gradlew test --tests "*AdminEventApiTest*" --no-daemon --stacktrace`

### 4.2 Admin (`apps/admin`)

- Install deps:
  - `cd apps/admin && npm ci`
- Dev server:
  - `cd apps/admin && npm run dev`
- Type-check (used as lint gate in CI):
  - `cd apps/admin && npx tsc --noEmit`
- Production build:
  - `cd apps/admin && npm run build`
- Tests:
  - `cd apps/admin && npm run test`

### 4.3 Mobile (`apps/mobile`)

- Install deps:
  - `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 pub get`
- Analyze (lint):
  - `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 analyze`
- Run all tests:
  - `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 test`
- Run a single test file:
  - `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 test test/app_config_test.dart`
- Run one named test case:
  - `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 test test/app_config_test.dart --plain-name "app env defaults to local"`

### 4.4 Workflow/Script Validation

- GitHub workflow lint: `actionlint` in `.github/workflows/workflow-lint.yml`
- Bash syntax checks:
  - `bash -n infra/aws/deploy.sh`
  - `bash -n infra/aws/verify-staging.sh`
- PowerShell syntax checks are defined in `.github/workflows/workflow-lint.yml`.
- Doc sync check:
  - `pwsh -File scripts/check-doc-sync.ps1 -BaseRef origin/develop -HeadRef HEAD`

## 5) Coding Guidelines (Cross-cutting)

- Keep changes minimal, local, and consistent with existing code.
- Preserve Clean Architecture boundaries:
  - Domain layer must not depend on framework/infrastructure.
  - Use ports/interfaces for external dependencies.
- Avoid speculative abstractions (YAGNI); prefer simple, readable code.
- Do not hardcode secrets, credentials, or environment-specific private values.
- Keep API contracts stable; if changed, update docs:
  - `docs/api-contract.md`
  - `docs/data-model.md`
  - related README/docs as needed.

## 6) Java (API) Style

- Use constructor injection; avoid field injection.
- Use `record` for simple immutable DTO/domain carriers where already adopted.
- Package conventions:
  - `domain.model`, `domain.repository`
  - `application.usecases`, `application.ports`
  - `presentation.controllers`
  - `infrastructure.*`
- Naming:
  - UseCase classes end with `UseCase`.
  - Repository interfaces end with `Repository`; JPA adapters include `JpaRepository`/`RepositoryAdapter`.
  - Enums in `UPPER_SNAKE_CASE` values.
- Error handling:
  - Throw `ApiException` for expected API errors.
  - Return envelope via `ApiResponse.success(...)` / `ApiResponse.failure(...)`.
  - Prefer specific exception mapping in `GlobalExceptionHandler`.
- Validation:
  - Use Jakarta validation annotations (`@NotBlank`, etc.) on request DTOs/records.
- Transactions:
  - Mark multi-write use case operations with `@Transactional`.

## 7) TypeScript/React (Admin) Style

- TypeScript is `strict`; avoid `any`.
- Prefer explicit interfaces/types for API request and response payloads.
- Keep API access centralized in `src/api/*` using shared client helpers.
- Handle async errors as:
  - `catch (err) { ... err instanceof Error ? err.message : fallback ... }`
- Naming conventions:
  - Components/pages: `PascalCase` file + symbol names.
  - Hooks/utils/functions/vars: `camelCase`.
  - Shared type aliases/interfaces: `PascalCase`.
- Imports:
  - Group React/library imports first, then local imports.
  - Prefer `type` imports where helpful (already used in places).
- Formatting conventions seen in repo:
  - Double quotes, semicolons, trailing commas where formatter applies.
  - Keep JSX readable; avoid deeply nested conditional complexity.

## 8) Dart/Flutter (Mobile) Style

- Lints come from `flutter_lints`; explicit rule `avoid_print: true`.
- Use single quotes and trailing commas for multiline widget trees/args.
- Prefer named parameters and `required` for clarity.
- Keep async/network error handling user-safe:
  - Throw/propagate `ApiException` with actionable messages.
  - Provide graceful fallback where pattern already exists (cache/offline paths).
- Naming:
  - Classes/widgets: `PascalCase`
  - Members/functions/locals: `camelCase`
  - Private symbols: `_leadingUnderscore`
- Keep API URL handling consistent with `AppConfig` (`/api/v1` normalization).

## 9) Testing Conventions

- API tests use JUnit 5 + Spring Boot test + MockMvc + AssertJ.
- Prefer deterministic setup/teardown; isolate DB state in each test.
- Add tests near changed behavior (controller/use case/repository level as appropriate).
- For mobile, keep tests small and focused (`test/*_test.dart` pattern).
- For admin, if introducing test tooling, align with existing CI gates and add script(s).

## 10) Agent Workflow Expectations

- Before editing, inspect nearby code for local conventions and follow them.
- After edits, run the smallest meaningful validation first, then broader checks.
- If behavior/contracts changed, update docs in the same change.
- Keep commits/PRs scoped and explain why, not just what.

## 11) Quick Command Matrix

- API all tests: `cd apps/api && ./gradlew test --no-daemon --stacktrace`
- API single test: `cd apps/api && ./gradlew test --tests "*ClassName*" --no-daemon --stacktrace`
- Admin test+typecheck+build: `cd apps/admin && npm ci && npm run test && npx tsc --noEmit && npm run build`
- Mobile analyze+test: `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 analyze && ..\\..\\scripts\\flutterw.ps1 test`
