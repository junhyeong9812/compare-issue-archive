# issue/cipipe/shallow-clone-version-unknown-overwrite — 정리 (힌트)

태그: `ci` · `deploy`

## 전체 흐름

```
clone --depth 1 → 태그 커밋 없음 → git describe 실패
빌드 스크립트: 실패 시 0.0.0-unknown (조용한 기본값)
아티팩트 저장소: settle-core:0.0.0-unknown 업로드 → 다음 빌드가 덮어씀
```

## 핵심 문장

- 얕은 클론은 **태그가 가리키는 과거 커밋이 없어** `describe`가 실패한다.
- 스크립트가 실패 시 기본값을 써서 빌드는 성공했다 — **조용한 기본값**이 문제를 숨겼다.
- 같은 버전 문자열로 여러 빌드가 올라가 덮어썼다.
- 태그를 가져오거나(`fetch --tags --depth`), 버전 계산 실패를 빌드 실패로 만든다.
