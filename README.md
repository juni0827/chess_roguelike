# Chess Roguelike

Android 우선 플레이어블을 기준으로 시작한 체스 로그라이크 프로젝트입니다. 현재 저장소는 공용 게임 코어, JSON 기반 콘텐츠/현지화 로더, Android 셸, Desktop 부트스트랩 셸로 나뉘어 있습니다.

## Modules

- `:core-game` - 체스 규칙, 상태 머신, 저장 모델, 메타 진행, AI 인터페이스
- `:content-io` - 콘텐츠 팩/모드 manifest, JSON 로더, locale catalog, mod resolver
- `:app` - Android UI, 모드 zip import, 로컬 저장, 현지화된 화면 셸
- `:desktop-app` - 공용 코어/콘텐츠를 읽는 JVM 부트스트랩 셸

## Local Setup

JDK 17이 필요합니다.

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew test
./gradlew assembleDebug
./gradlew lint
```

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
