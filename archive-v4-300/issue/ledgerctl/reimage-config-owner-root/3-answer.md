# issue/ledgerctl/reimage-config-owner-root — 정답

태그: `environment-drift` · `deployment` · `permission`

## 정답
<!-- 질문 1:1 대응 -->

1. **설정 파일 소유자가 root로 바뀌었다.**\
백업을 root로 풀어 `client.conf`가 root:root 0600이 됐고, 배치 계정은 파일을 읽지 못해 자격증명 없이 접속했다.

2. **점검을 sudo로 실행했기 때문이다.**\
root는 파일을 읽을 수 있어 성공했다 — 크론은 batch 계정으로 돈다.

3. **2호기 담당자는 위키 최신 판 절차서(소유자 복원 단계 포함)를 썼다.**\
1호기 담당자는 공유 폴더의 옛 PDF 판을 썼다.

4. **절차서가 두 곳에 두 판으로 있었고, 옛 판에는 소유자 복원이 없었다.**\
복원 결과 확인 항목도 "파일 존재"뿐이었다.

5. **점검은 실행 계정으로, 확인은 소유자·권한까지.**\
재이미징 후 배치 계정으로 읽기 전용 점검 명령을 한 번 돌리는 단계를 절차서에 넣고, 옛 PDF 판은 폐기했다.

## 문제 구조 (추상화 코드)

### 변형 A — 백업 복원 후 소유자 미복원
① 문제 코드
```sh
sudo tar -xzf ledgerctl-conf.tgz -C /etc/ledgerctl
sudo ledgerctl ...        # 점검 — root 로 성공
```
② 고친 코드
```sh
sudo tar -xzf ledgerctl-conf.tgz -C /etc/ledgerctl
sudo chown -R batch:batch /etc/ledgerctl && sudo chmod 0600 /etc/ledgerctl/client.conf
sudo -u batch ledgerctl ...   # 실행 계정으로 점검
```
무엇이 깨졌나: 점검 환경(root)과 실행 환경(batch 계정)이 달라 복원 결함이 첫 배치에서야 드러났다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 복원 스크립트에 소유자·권한 고정 | 스크립트 단일 출처 | 작음 | 스크립트 사본 분기 | 기본 |
| 설정 관리 도구로 파일 배포 | 구성 관리 도입 | 중간 | 도구 장애 시 수동 복원 미숙 | 장기 |
| 배치 직전 사전 점검 잡 | 크론에 선행 잡 추가 | 작음 | 점검 잡 자체 실패 무시 | 병행 |

**결론**: 복원은 소유자까지, 점검은 실행 계정으로 — 절차서는 한 곳에만 둔다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
