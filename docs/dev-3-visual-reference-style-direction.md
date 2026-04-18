# DEV-3 체스 로그라이크 비주얼 레퍼런스 수집과 스타일 방향 정리

## Goal

- 체스 보드의 **전술 가독성**과 로그라이크의 **런 진행 보상성**을 동시에 전달하는 비주얼 방향을 확정한다.
- DEV-5(핵심 화면 IA/와이어), DEV-6(HUD 우선순위), DEV-7(메타/보상 UX)이 공통으로 참조할 수 있는 **read-only 스타일 기준**을 제공한다.

## Inputs

### 상위/연관 이슈 기반 요구사항

- 상위 조율 이슈 CEO-13의 핸드오프 원칙 반영: 현재 결정/남은 결정/다음 액션 명시.
- 연관 이슈:
  - DEV-5: 핵심 화면 IA와 와이어프레임 정리
  - DEV-7: 메타 진행과 보상 선택 UX 설계
  - CEO-10: 플레이어 니즈와 코어 판타지 정의

### 참고 레퍼런스(2026-04-18 확인)

> 아래는 스타일 무드/정보 계층 구조 참고 목적이며, 특정 아트셋 복제를 금지한다.

1. Into the Breach UI 인터뷰 (가독성 우선 철학)
   - https://www.gamedeveloper.com/design/-i-into-the-breach-i-dev-on-ui-design-sacrifice-cool-ideas-for-the-sake-of-clarity-every-time-
2. Shotgun King Steam 페이지 (체스+로그라이크 장르 톤 참고)
   - https://store.steampowered.com/app/1972440/Shotgun_King_The_Final_Checkmate/
3. Shotgun King 스크린샷 (최소 UI에서 룰 전달 사례)
   - https://www.mobygames.com/game/183592/shotgun-king-the-final-checkmate/screenshots/windows/1139056/

## Visual Reference Synthesis

### A. 보드 가독성 축

- **가져올 요소**
  - 8x8 보드와 액션 표시 레이어를 분리해 시선 우선순위 명확화
  - 위협/가능 이동/선택 상태를 색상 + 모양(패턴)으로 이중 코딩
  - "한 턴 내 결과 예측"을 HUD가 아니라 보드에 직접 투영
- **피할 요소**
  - 저채도 배경 위 저대비 텍스트
  - 픽셀/장식 폰트를 본문 숫자 정보에 사용

### B. 로그라이크 보상 축

- **가져올 요소**
  - 전투 종료 직후 1-of-3 카드/유물 선택 패턴
  - 희귀도 색상과 효과 강도의 일치(희귀도-파워 인지 비용 절감)
- **피할 요소**
  - 전투 화면과 보상 화면의 완전히 다른 정보 문법(학습 단절)

### C. 톤 앤 무드 축

- **가져올 요소**
  - "절제된 다크 판타지"(과한 글로우보다 재질 대비)
  - 금속/석재/양피지의 촉감은 프레임에만 제한 적용
- **피할 요소**
  - 배경 장식이 보드 대비를 침식하는 과밀 텍스처

## Style Direction v0.2 (Read-only)

### 1) Color Tokens

- `bg/base`: #111318
- `bg/elev-1`: #1A1F29
- `line/subtle`: #2C3444
- `text/primary`: #E9EEF7
- `text/secondary`: #AAB4C5
- `accent/tactical`: #4DA3FF
- `accent/warn`: #FFB347
- `accent/danger`: #FF5D6C
- `accent/relic`: #B08D57

**Must**
- 위협/피해/실패는 `accent/danger` 계열로 일관 표기한다.
- 선택 상태는 색상 1개 의존 금지(외곽선/패턴 동시 사용).

**Should**
- 메타/상점/보상 화면은 `accent/relic`을 10~20% 포인트로 제한 사용.

### 2) Typography

- 제목: SemiBold 22/28
- 섹션: Medium 18/24
- 본문: Regular 15/22
- 숫자/전투값: SemiBold 16/20 (탭형 숫자 우선)

**Must**
- 본문 가독성 폰트는 산세리프 계열 고정.
- 12sp 미만 텍스트 금지.

**Should**
- 분위기용 장식 폰트는 타이틀/로고에만 사용.

### 3) Icon & Shape

- 버프/디버프/상태 아이콘은 20dp 그리드 기준.
- 턴 관련 핵심 액션은 채움형, 정보성 아이콘은 라인형.

**Must**
- 동일 의미 아이콘의 스타일 혼용 금지(라인/채움 섞지 않기).

### 4) Frame & Surface

- 카드/패널 코너 반경: 10dp
- 클릭 가능한 타일 최소 터치 영역: 40dp
- 계층 그림자: 2단계까지만 허용

### 5) Motion

- 전술 피드백(선택/타격): 120~180ms
- 전환(화면/모달): 180~240ms
- 보상 강조: 260~320ms, 1회성 스케일 업

**Must**
- 승패/치명 상태 외에는 과도한 화면 흔들림 금지.

## Screens (대상 화면)

1. Main
   - 플레이 시작, 진행 재개, 메타 진입 허브
2. Battle Board
   - 8x8 보드, 턴 정보, 위험 예고, 스킬/행동
3. Upgrade Reward (1-of-3)
   - 전투 보상 선택, 희귀도/시너지 표시
4. Meta Progression
   - 잠금 해제, 영구 강화, 빌드 이력
5. Result/Run Summary
   - 패배/승리 결과, 획득 리소스, 다음 런 진입

## States (빈/로딩/오류/성공)

### Main
- Empty: 세이브 없음 + 신규 런 CTA 노출
- Loading: 세이브/콘텐츠 인덱스 로드 스피너
- Error: 세이브 파싱 실패 + "복구/초기화" CTA
- Success: 최근 런 스냅샷 카드 표시

### Battle Board
- Empty: 튜토리얼 진입 전 오버레이 안내
- Loading: 스테이지 진입/적 배치 중 인터랙션 잠금
- Error: 턴 계산 실패 시 "재시도/로그 내보내기"
- Success: 선택/이동/공격/결과 예측이 즉시 반영

### Upgrade Reward
- Empty: 보상 없음(이벤트 분기) 시 스킵 안내
- Loading: 카드 생성 애니메이션
- Error: 카드 풀 로드 실패 시 기본 카드 폴백
- Success: 1개 선택 후 즉시 덱/빌드 반영 피드백

## Handoff Criteria

### DEV 전달 완료 조건

- [ ] DEV-5에서 본 문서의 Screen 우선순위를 IA에 반영
- [ ] DEV-6에서 Battle HUD 정보 우선순위를 토큰 기준으로 정렬
- [ ] DEV-7에서 Upgrade 1-of-3 컴포넌트에 색/타이포/모션 규칙 적용
- [ ] Android 실제 기기 기준 명암 대비(텍스트/배경) 점검
- [ ] "색상 외 상태 구분" 접근성 체크(패턴/아이콘 동시 표기)

### 현재 결정 사항

- 스타일 축: **Occult Tactics on Clean Grid**
- 보드 우선 원칙: 장식보다 전술 정보 우선
- 보상 UX 원칙: 1-of-3 즉시 비교 + 선택 후 즉시 강화 체감

### 남은 결정 사항

1. 배경 텍스처 강도(저/중/고) 최종 선택
2. 희귀도 4단계 vs 5단계 체계 고정
3. 치명타/체크메이트 연출 강도 상한값

### 다음 액션

1. DEV-5: Main/Battle/Upgrade 와이어프레임에 토큰 주석 삽입
2. DEV-6: Battle HUD 정보 밀도 A/B 시안 제작
3. DEV-7: Reward 카드 컴포넌트 프로토타입(모바일 터치 영역 검증)
4. 공통: 1차 구현물 리뷰 후 v0.3 토큰 조정
