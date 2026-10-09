# issue/kdeploy/configmap-subpath-not-updated — 정답

태그: `environment-drift` · `stale-config`

## 정답
<!-- 질문 1:1 대응 -->

1. **바뀌지 않는다.** 바인드된 파일은 기동 시점 내용이다.

2. **변경 뒤 오토스케일로 새로 뜬 파드다.** 오후 피크에 늘어난 4개만 새 수수료였다.

3. **kubelet 동기화 뒤 바뀐다.** 다만 파드마다 시점이 달라 잠시 갈린다.

4. **ConfigMap 이름에 내용 해시(`fee-7c9d2`)를 붙이고 배포 템플릿이 참조하게 한다.** 변경 = 새 롤아웃.

## 문제 구조 (추상화 코드)

### 변형 A — subPath 마운트에 핫 리로드를 기대
① 문제 코드
```yaml
volumeMounts:
- { name: fee, mountPath: /app/conf/fee.yaml, subPath: fee.yaml }
volumes:
- { name: fee, configMap: { name: fee } }
```
② 고친 코드
```yaml
volumeMounts:
- { name: fee, mountPath: /app/conf/fee }
volumes:
- { name: fee, configMap: { name: fee-7c9d2 } }   # 내용 해시 → 변경 시 롤아웃
```
무엇이 깨졌나: 설정 반영 경로가 마운트 방식에 따라 다르다는 것을 몰랐다.

## 방안 비교

| 방안 | 전제 | 비용 | 실패 모드 | 맞는 조건 |
|---|---|---|---|---|
| 해시 이름 + 롤아웃 | 템플릿 도구 | 작음 | 옛 ConfigMap 정리 필요 | 기본 |
| 디렉터리 마운트 + 핫 리로드 | 앱 리로드 구현 | 작음 | 파드별 반영 시점 차이 | 잦은 변경 |
| 설정 서비스 조회 | 외부 의존 | 중간 | 설정 서비스 장애 | 선택하지 않은 방법 |

**결론**: 금액에 영향을 주는 설정은 롤아웃으로만 바꾼다. 차이 기간 거래 1,930건은 수수료를 재계산했다.

## 검증 기록
- 2026-10-09: 사건 기록 대조·추상화(Claude 초안, 가상 아카이브)
