# issue/cdcpipe/numeric-as-double-rounding — 정리 (힌트)

태그: `data-semantics` · `precision`

## 전체 흐름

```
원장: fee numeric(18,4)  예) 1234.5650
cdcpipe decimal.handling.mode=double → 1234.5649999999999
분석 저장소: double 합계 후 원 단위 반올림 → 원장과 차이
가맹점 9만 개 × 일 합산 → 월 합계 37원 차이
```

## 핵심 문장

- `decimal.handling.mode=double`은 numeric을 **이진 부동소수**로 바꾼다 — 소수 넷째 자리 값이 정확히 표현되지 않는다.
- 건별로는 차이가 거의 없지만, 합산·반올림 경계(.5)에서 원 단위가 갈린다.
- 설정은 분석 도구 호환 때문에 넣은 것이었다.
- 대처: `precise`(decimal 바이트)나 `string`으로 내보내고 저장소는 decimal 타입으로.
