# 진짜 Codex를 GitHub PR 워크플로우에 자동화하는 방법

이 문서는 "유사 기능"이 아니라 **실제 `openai/codex-action`** 을 사용해 PR마다 Codex를 자동 실행하는 구성안입니다.

## 1) 무엇이 추가되었나

- 실제 동작 가능한 워크플로우 파일: `.github/workflows/codex-pr-automation.yml`
- PR 생성/업데이트 시 Codex 리뷰 자동 코멘트
- PR 코멘트 명령으로 Codex 재실행
  - `@codex_review` → 리뷰만
  - `@codex_review fix` → Codex가 수정 시도 + 테스트 + 별도 fix PR 생성

## 2) 사전 준비 (필수)

1. GitHub 저장소 `Settings > Secrets and variables > Actions`로 이동
2. `OPENAI_API_KEY` 시크릿 추가
3. 워크플로우 권한 확인
   - `contents: write`, `pull-requests: write`, `issues: write` (fix 모드 포함 시)

## 3) 자동 실행 시나리오

### A. PR 리뷰 자동화
- 트리거: `pull_request` (`opened`, `synchronize`, `reopened`, `ready_for_review`)
- 동작:
  1) PR merge ref checkout
  2) `openai/codex-action@v1` 실행 (read-only sandbox)
  3) 결과를 PR 코멘트로 게시

### B. 수동 재실행
- 트리거: PR 코멘트에 `@codex_review`
- 동작: 리뷰 job만 재실행

### C. 자동 수정 + 디버깅
- 트리거: PR 코멘트에 `@codex_review fix`
- 동작:
  1) PR head checkout
  2) Codex가 수정 수행 (workspace-write sandbox)
  3) `./gradlew test` 검증
  4) `peter-evans/create-pull-request`로 fix PR 생성

## 4) 현재 워크플로우의 안전 설계

- 리뷰 모드는 `sandbox: read-only`
- 수정 모드는 `sandbox: workspace-write` + 별도 브랜치/별도 PR
- `safety-strategy: drop-sudo` 사용 (권한 축소)
- `concurrency`로 중복 실행 취소
- `issue_comment` 트리거는 **base repo context** 에서 실행될 수 있으므로, `OPENAI_API_KEY` 등 시크릿이 보이는 상태로 동작할 수 있음
- 따라서 `@codex_review`, `@codex_review fix` 같은 코멘트 명령은 **아무나 실행 가능하게 두면 안 되고**, `author_association` 기준으로 `OWNER` / `MEMBER` / `COLLABORATOR` 등 **신뢰된 사용자만 허용**하는 gate가 필요
- 특히 `fix` 모드는 PR head checkout + 쓰기 권한이 결합되므로, **same-repo PR에서만 허용**하거나 최소한 fork PR에서는 차단하는 것을 권장
- `pull_request` 트리거는 fork에서 올라온 PR의 경우 기본적으로 저장소 시크릿에 접근하지 못하므로, `OPENAI_API_KEY`가 필요한 Codex 리뷰는 **fork PR에서는 실패하거나 skip되는 것이 정상 동작**임

## 5) 운영 팁

- `fix` 모드는 기본적으로 사람이 트리거하게 유지 (무분별한 자동 수정 방지)
- `issue_comment` 재실행은 멤버 전용으로 제한하고, 외부 기여자 코멘트로는 실행되지 않도록 명시적으로 gate 구성
- fork PR은 자동 리뷰가 바로 돌지 않을 수 있으므로, 필요 시 maintainer가 브랜치를 내부 저장소로 옮기거나 신뢰된 멤버가 코멘트로 재실행하는 운영 절차를 준비
- `./gradlew test` 외에 `lint`/`detekt`를 추가해 품질 게이트 강화
- 대형 PR은 코멘트 명령으로 재실행 시점 제어

## 6) 참고 소스

- OpenAI Codex GitHub Action: https://github.com/openai/codex-action
- OpenAI Help (Codex GitHub 자동 리뷰 개요): https://help.openai.com/en/articles/11369540/

---

원하면 다음 단계로, 이 워크플로우에 `label gate`(예: `autofix-enabled`일 때만 fix 허용),
`CODEOWNERS` 기반 경로별 프롬프트 분기, 실패 로그 자동 첨부까지 붙여서 더 안전하게 고도화할 수 있습니다.
