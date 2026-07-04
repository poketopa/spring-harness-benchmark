# Blind Implementation Prompt Template

Use this template only for the implementation agent/session.

Do not include hidden oracle text, oracle path, prior solution paths, previous run notes, comparison rows, or expected hidden failures.

```text
이 세션은 prompt-only blind benchmark의 구현 세션이다.

작업 디렉터리는 이미 격리된 Spring target project이며, 이 디렉터리 밖의 benchmark 기록, hidden oracle, 이전 solution target, run note를 읽지 않는다.

목표:
아래 사용자 요구사항만 보고 필요한 Spring 구현을 수행한다. 요구사항이 제품 정책을 결정하기에 부족하고 서로 다른 정답이 가능하다면, 임의로 구현하지 말고 구체적인 clarification question을 남기고 멈춘다.

사용자 요구사항:
---
{prompt_payload}
---

허용된 작업:
- 현재 target project 내부 파일 읽기
- 현재 target project 내부 구현/테스트 수정
- 필요한 targeted test와 `./gradlew clean test` 실행

금지된 작업:
- hidden oracle 읽기
- benchmark-records 읽기
- prompt-sufficiency-runs 또는 prompt-sufficiency-blind-runs의 다른 target 읽기
- stable solution target에서 source 복사
- 이전 run note 또는 convention comparison 참조

완료 시 보고:
- 구현했는지, clarification_needed로 멈췄는지
- 변경한 주요 파일
- 실행한 검증 명령과 결과
- 요구사항 해석상 가정한 점
```
