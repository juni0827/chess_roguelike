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

## 5) 운영 팁

- `fix` 모드는 기본적으로 사람이 트리거하게 유지 (무분별한 자동 수정 방지)
- `./gradlew test` 외에 `lint`/`detekt`를 추가해 품질 게이트 강화
- 대형 PR은 코멘트 명령으로 재실행 시점 제어

## 6) 참고 소스

- OpenAI Codex GitHub Action: https://github.com/openai/codex-action
- OpenAI Help (Codex GitHub 자동 리뷰 개요): https://help.openai.com/en/articles/11369540/

---

원하면 다음 단계로, 이 워크플로우에 `label gate`(예: `autofix-enabled`일 때만 fix 허용),
`CODEOWNERS` 기반 경로별 프롬프트 분기, 실패 로그 자동 첨부까지 붙여서 더 안전하게 고도화할 수 있습니다.
