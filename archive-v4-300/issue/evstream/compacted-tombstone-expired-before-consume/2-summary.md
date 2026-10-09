# issue/evstream/compacted-tombstone-expired-before-consume — 정리 (힌트)

태그: `silent-failure` · `compaction`

## 전체 흐름

```
merchant.state (cleanup.policy=compact, delete.retention.ms=24h)
  M1: ACTIVE ... → 해지 시 tombstone(value=null)
search-cache 소비자: 인덱스 재구축 작업으로 3일 정지
  압축 실행: M1 의 옛 값 제거 → tombstone 만 남음 → 24h 후 tombstone 도 제거
재개: M1 관련 레코드 없음 → 캐시에 남은 ACTIVE 가 그대로
```

## 핵심 문장

- 압축 토픽에서 삭제 표지(tombstone)는 **`delete.retention.ms` 동안만** 남는다 — 그 뒤에는 키가 흔적 없이 사라진다.
- 그 시간보다 오래 멈춘 소비자는 "삭제됐다"는 사실을 받을 방법이 없다.
- 캐시를 처음부터 재구축하면 맞지만, 이어서 소비하는 증분 소비자는 옛 값을 계속 들고 있다.
- 대처: 정지가 보존 시간을 넘은 소비자는 증분 재개 대신 **전체 재구축**하도록 기동 검사 추가, 보존 시간 7일로 연장.
