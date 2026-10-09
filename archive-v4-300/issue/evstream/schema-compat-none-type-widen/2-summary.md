# issue/evstream/schema-compat-none-type-widen — 정리 (힌트)

태그: `deploy` · `contract`

## 전체 흐름

```
pay.completed 스키마 v7: amount:int → v8: amount:long
레지스트리 subject 호환성 = NONE (예전 긴급 배포 때 꺼 둠)
생산자 v8 발행 → 소비자(v7 리더) → AvroTypeException → 재시도 → 정지
```

## 핵심 문장

- 레지스트리 호환성 검사가 **NONE**이면 어떤 스키마 변경도 등록된다 — 소비자 깨짐을 막는 장치가 없다.
- 1년 전 긴급 배포 때 끈 설정이 그대로였다.
- int→long은 Avro에서 리더 쪽 승격만 허용된다 — 옛 리더는 long을 읽지 못한다.
- 대처: 생산자 롤백, 호환성 BACKWARD 복구, 소비자 먼저 v8 리더로 배포한 뒤 생산자 배포.
