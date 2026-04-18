# DEV-10: 체스 로그라이크 데이터 구조/콘텐츠 확장 구현 지시서

이 문서는 **"구현 가능한 설계"를 실제 PR 단위로 분리**하기 위한 가이드입니다.
핵심 원칙은 다음과 같습니다.

- 공용 규약을 먼저 문서화하고(읽기 전용 기준)
- 인터페이스 고정 → 로더 확장 → 검증 강화 → 마이그레이션 대응 순서로 진행하며
- 한 PR에서 모델/로더/검증/마이그레이션을 동시에 크게 흔들지 않습니다.

---

## 1) 현재 기준선(Baseline) 고정

### 1-1. 현재 모델 경계

`GameContentFile`과 `ContentRegistry`를 기준으로 런타임 콘텐츠 경계는 아래 5개입니다.

- `pieces`
- `abilities`
- `upgrades`
- `rounds`
- `balance`

### 1-2. 현재 병합/우선순위 규약

`ResolvedContentRegistry.resolve(...)` 기준으로,

- 입력: `base + official + enabled user packs`
- 정렬: `sourcePriority` → `manifest.loadOrderHint` → `manifest.id`
- 충돌: 동일 키는 **나중에 병합된 pack이 덮어씀(last write wins)**

### 1-3. 현재 호환성 규약

- `manifest.compatibleGameVersions`는 게임 버전(`GAME_VERSION`)과 비교해 로딩 가능 여부를 결정
- 누락 dependency는 로딩 단계에서 즉시 실패

---

## 2) 구현 PR 분할 전략(충돌 방지용)

> 아래 단계는 반드시 순서대로 진행합니다.
> 각 단계는 **직전 단계의 public contract를 보존**해야 합니다.

### 단계 A — 인터페이스/규약 고정 (Read-only + 최소 코드)

### 목표

다른 이슈(DEV-8, DEV-9 등)가 공통으로 참조할 수 있는 규약을 먼저 고정합니다.

### 작업 범위

- 문서 정리(규약 원문)
- 필요 시 `typealias`, wrapper, KDoc 추가처럼 **비파괴적 변경만 허용**

### 수정 예상 위치

- `libraries/core-game/src/main/kotlin/com/chessroguelike/content/ContentModels.kt`
- `libraries/content-io/src/main/kotlin/com/chessroguelike/contentio/GameContentFile.kt`
- `docs/dev-10-data-structure-implementation-guide.md`

### 테스트 범위

- `:core-game:test` (기존 테스트 불변 확인)
- `:content-io:test` (직렬화/역직렬화 깨짐 여부 확인)

### 산출물 체크

- 모델 필드 명세/ID 규칙/참조 규칙이 문서에 고정됨
- 병합 우선순위 규약 문장화 완료

---

## 단계 B — 로더 확장 (Backward-compatible)

### 목표

기존 콘텐츠를 깨지 않으면서 pack 입력 경로/로딩 단계를 확장합니다.

### 작업 범위

- pack source 추가(예: 공식 DLC root 추가)
- 로더 내부 유틸 추가(경로 검증, locale 수집 보강)
- **파서 입력 스키마 변경 금지** (이 단계에서 schema bump 금지)

### 수정 예상 위치

- `libraries/content-io/src/main/kotlin/com/chessroguelike/contentio/PackSources.kt`
- `libraries/content-io/src/main/kotlin/com/chessroguelike/contentio/ModResolver.kt`

### 테스트 범위

- pack source 단위 테스트(디렉터리/리소스 로딩)
- enabledModIds 필터/의존성 누락 케이스 회귀 테스트

### 산출물 체크

- 기존 `base-game` 로딩 결과 hash/registry가 변하지 않음
- 확장 source를 켜도 순서 규약(`sourcePriority`, `loadOrderHint`, `id`) 유지

---

## 단계 C — Validator 강화 (Fail-fast)

### 목표

런타임에서 터지는 오류를 로딩 시점으로 당겨서 실패 원인을 명확히 합니다.

### 작업 범위

- 참조 무결성 검사
  - `UpgradeEffectDefinition.AddAbility.abilityId` 존재 확인
  - `RoundDefinition.enemyAbilityIds` 존재 확인
- ID 포맷 정책 검사(예: `namespace.type` 형태)
- locale key 누락 검사(필수 key set 대비)

### 수정 예상 위치

- `libraries/content-io/src/main/kotlin/com/chessroguelike/contentio/ResolvedContentRegistry.kt`
- (필요 시 신규) `libraries/content-io/src/main/kotlin/com/chessroguelike/contentio/ContentValidators.kt`

### 테스트 범위

- invalid fixture 기반 실패 테스트(메시지까지 검증)
- valid fixture는 기존과 동일하게 resolve 성공

### 산출물 체크

- "왜 실패했는지"가 예외 메시지에 구체적으로 포함
- 검증 실패는 항상 resolve 단계에서 재현 가능

---

## 단계 D — schemaVersion 도입 (점진 적용)

### 목표

대규모 포맷 변경 전에 안전한 이행 레이어를 마련합니다.

### 도입 방식 (권장)

1. **D-1: Manifest에 선택 필드 추가**
   - `schemaVersion: Int? = null` (nullable)
   - null이면 `1`로 간주
2. **D-2: Loader에서 버전 분기만 추가**
   - v1: 기존 로직 그대로
   - v2+: 아직 미지원이면 명확한 에러(`Unsupported schemaVersion`)
3. **D-3: Migration 도입**
   - `ContentMigration` 인터페이스 추가
   - `v1 -> v2` 변환은 변환기에서 수행 후 공통 validator 재사용
4. **D-4: schemaVersion 필수화**
   - 모든 공식/기본 pack이 schemaVersion 명시한 뒤 null 허용 제거

### 수정 예상 위치

- `libraries/core-game/src/main/kotlin/com/chessroguelike/content/ContentModels.kt` (manifest)
- `libraries/content-io/src/main/kotlin/com/chessroguelike/contentio/PackSources.kt`
- `libraries/content-io/src/main/kotlin/com/chessroguelike/contentio/ResolvedContentRegistry.kt`
- (신규) `libraries/content-io/src/main/kotlin/com/chessroguelike/contentio/migration/*`

### 테스트 범위

- schemaVersion null/v1/v2 fixture
- 미지원 버전 실패 테스트
- migration 적용 후 registry 동등성 테스트

### 산출물 체크

- 버전별 동작이 테스트로 고정됨
- migration은 "입력 포맷 변환"까지만 담당, 비즈니스 규칙은 validator에서 일괄 처리

---

## 3) 책임 경계 (Owner 분리)

- `core-game` 책임
  - 콘텐츠 도메인 모델 타입
  - 런타임에서 필요한 최소 contract
- `content-io` 책임
  - 파일/리소스 로딩
  - pack 병합
  - 버전 호환성 검사
  - validator/migration 실행
- `app` 책임
  - 어떤 pack을 활성화할지 사용자 설정 전달
  - 로딩 실패 메시지 표시/복구 UX

---

## 4) PR 템플릿(DEV-10 하위 구현 공통)

각 PR 설명에 아래를 고정 포함합니다.

1. 어떤 단계(A/B/C/D)인지
2. Public contract 변경 여부 (있음/없음)
3. 영향 모듈(`core-game`, `content-io`, `app`) 
4. 실패 시 롤백 방법
5. 추가/수정 테스트 목록

---

## 5) 실패 조건 / 롤백 조건

아래 중 하나라도 발생하면 즉시 롤백 또는 hotfix 브랜치로 격리합니다.

- base-game만 로딩했을 때 기존 대비 `contentHash`가 변했는데 의도 설명 불가
- 기존 공식 pack이 새 validator에서 대량 실패
- schemaVersion 미도입 pack이 더 이상 로딩되지 않음(점진 적용 단계 위반)
- 앱에서 모드 OFF 상태에서도 로딩 실패가 발생

---

## 6) 다음 팀 핸드오프 스냅샷 (CEO-13 규칙 반영)

### 현재 결정 사항

- 구현 순서는 A→B→C→D로 고정
- D 단계 전까지는 대규모 schema 변경 금지
- 검증 실패는 resolve 시점 fail-fast 원칙 유지

### 남은 결정 사항

- ID 포맷 정규식 최종안(`namespace.type` vs `domain.entity.variant`)
- locale 필수 key의 최소 세트
- schemaVersion v2 목표 스펙(무엇이 바뀌는지)

### 다음 액션

1. 단계 A 전용 PR 작성(문서/KDoc 중심)
2. 단계 B에서 source 확장 PR 분리
3. 단계 C에서 validator 전용 PR 분리
4. 단계 D에서 migration PR 분리

### 막힌 점/필요 입력값

- CEO 팀: v2 스키마에서 변경할 기획 요구사항 우선순위 확정 필요
- DEV 팀: validator 에러 메시지의 사용자 노출 레벨(개발자용/사용자용) 결정 필요
