# issue/amlscan/str-file-bom-reject — 정리 (힌트)

태그: `silent-failure` · `encoding` · `environment-drift`

## 전체 흐름

```
보고서 생성 도구 (리포팅 서버 4.0 업그레이드, 2025-03-04)
  내보내기 기본값 = UTF-8 + BOM          ← 이전 3.x 는 BOM 없음
        │ str_20250311.txt  (EF BB BF 'H' 'D' 'R' ...)
        ▼
amlscan submit ──SFTP 업로드──▶ FIU 보고망 수신함
  stdout "SUBMITTED S-77120", exit 0      ← 업로드 성공 = 제출 성공으로 표시

FIU 일괄 수집 (매일 02:00)
  첫 줄이 "HDR" 로 시작? ── 아니오 (BOM 3바이트가 앞에 있음) → 파일 전체 반려
  반려 결과 = 다음 날 결과 파일 RJT_<제출번호>.txt (사유 코드 F02 헤더 오류)

amlscan status : 결과 파일을 가져오기 전까지 SUBMITTED 유지
```

## 핵심 문장

- FIU 보고망은 파일 첫 줄이 `HDR`로 시작해야 한다 — 앞에 BOM 3바이트가 붙으면 **파일 전체**를 헤더 오류(F02)로 반려한다.
- `amlscan submit`의 `SUBMITTED`는 **업로드 성공**이다 — 접수 판정은 다음 날 결과 파일로 온다.
- 제출 전 BOM 검사, 제출 다음 날 결과 파일(ACC/RJT) 대조를 절차로 묶는다.
