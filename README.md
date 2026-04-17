# Chess Roguelike

Android 우선 플레이어블을 기준으로 시작한 체스 로그라이크 프로젝트입니다. 현재 저장소는 공용 게임 코어, JSON 기반 콘텐츠/현지화 로더, Android 셸, Desktop 부트스트랩 셸로 나뉘어 있습니다.

## Modules

- `:core-game` - 체스 규칙, 상태 머신, 저장 모델, 메타 진행, AI 인터페이스
- `:content-io` - 콘텐츠 팩/모드 manifest, JSON 로더, locale catalog, mod resolver
- `:app` - Android UI, 모드 zip import, 로컬 저장, 현지화된 화면 셸
- `:desktop-app` - 공용 코어/콘텐츠를 읽는 JVM 부트스트랩 셸

## Repository Layout

```text
apps/android/src/main/java/com/chessroguelike/
  bootstrap/   # Application 시작점
  data/save/   # 로컬 저장소
  di/          # 앱 조립과 런타임 접근
  feature/
    game/      # 플레이 화면
    main/      # 메인 메뉴
    upgrade/   # 업그레이드 선택
  ui/board/    # 커스텀 보드 뷰

libraries/core-game/src/main/kotlin/com/chessroguelike/
  ai/
  content/
  engine/
  game/

libraries/content-io/src/main/kotlin/com/chessroguelike/contentio/
apps/desktop/src/main/kotlin/com/chessroguelike/desktop/
artifacts/      # APK, 빌드 출력, 프로젝트 캐시
docs/
```

## Local Setup

JDK 17이 필요합니다.

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew test
./gradlew assembleDebug
./gradlew lint
```

`assembleDebug`, `assembleRelease` 후 복사되는 APK는 저장소 루트에 보이게 복사되고,
Gradle/Kotlin 프로젝트 캐시는 `artifacts/project-cache/` 아래에 모입니다.

## Content Packs

기본 게임과 유저 모드는 같은 구조를 사용합니다.

```text
manifest.json
content/game-content.json
locales/en.json
locales/ko-KR.json
assets/...
```

유저 모드는 데스크톱에서 `~/.chess-roguelike/mods`, Android에서는 앱 내부 저장소 `files/mods` 아래에 풀립니다.

## Current Scope

- 공식 초기 언어: `ko-KR`, `en`
- 모딩 범위: 데이터 기반 정의, 텍스트, 아이콘/리소스 override
- 저장: 로컬 JSON snapshot
- 규칙 확장: 폰 자동 퀸 승격 포함
