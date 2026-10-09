# issue/tplsvc/locale-missing-key-empty-string — 정답

태그: `i18n` · `silent-failure`

## 정답
<!-- 질문 1:1 대응 -->

1. **"Payment of  completed"다.** 금액 자리가 비고 오류는 없다.

2. **렌더 테스트를 ko로만 했다.**

3. **기본 언어로 대체 렌더해 보내고 경보를 올린다.** 빈칸 메시지보다 한국어 메시지가 낫다고 판단했다.

4. **영향 고객의 언어 설정 분포를 본다.** 한 언어에 몰리면 번역 파일을 확인한다.

## 문제 구조 (추상화 코드)

### 변형 A — 누락 변수 빈 문자열 치환
① 문제 코드
```java
String out = engine.render(tpl(locale), vars);          // 누락 → ""
```
② 고친 코드
```java
try {
    return engine.strict().render(tpl(locale), vars);
} catch (MissingVariable e) {
    alert.warn("tpl missing " + e.name() + " " + locale);
    return engine.strict().render(tpl(DEFAULT_LOCALE), vars);
}
```
무엇이 깨졌나: 템플릿 엔진의 관대한 기본값이 번역 누락을 숨겼다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 엄격 렌더 + 기본 언어 대체 | 기본 언어 완전 | 작음 | 대체 언어 수신 | 기본 |
| CI 변수 집합 비교 | 번역 파일 저장소 | 작음 | 없음 | 함께 적용 |

**결론**: 템플릿은 엄격 모드로 렌더하고, 언어별 변수 집합을 CI에서 맞춘다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
