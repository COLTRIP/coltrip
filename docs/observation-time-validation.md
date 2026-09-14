# 관측 고요지수 시각 검증 (#86)

## 정책

- 대상: `POST /api/internal/quiet-index`의 `calculatedAt`.
- 기존 offset 없는 LocalDateTime 요청 형식을 유지하고 Asia/Seoul 로컬 시각으로 해석한다. AI가 UTC를 사용한다면 전송 전에 KST로 변환해야 한다.
- 서버가 인증을 확인한 뒤 검증 시각을 한 번 읽는다. JVM 기본 시간대와 무관하게 명시적인 KST로 비교한다.
- 허용 미래 오차는 0이다. 현재와 정확히 같은 시각 또는 과거는 허용하고 조금이라도 미래이면 400 `InvalidObservationTimeException`으로 거부한다. null은 기존 HTTP 필수값 검증으로 400이며 서비스 직접 호출도 방어한다.
- 이는 이번 구현의 보수적인 기본 정책이며 AI 서버 시계 동기화가 필요하다. 추후 오차 허용 합의 없이 미래 값을 조용히 보정하거나 허용하지 않는다.
- 인증 실패가 먼저 처리된다. 미래 시각 검증은 DB 조회/잠금/쓰기 전에 수행하므로 거부 시 이력·캐시·갱신 시각이 바뀌지 않는다.
- 과거 이력 수신과 동일 계산 시각 정정은 기존대로 허용하며 현재 캐시는 더 이전 시각으로 덮어쓰지 않는다.
- 예측 데이터는 기존 `/api/internal/quiet-index/forecasts` 계약으로 보낸다. 관측 API로 미래 예측을 받지 않는다.
- 지도 pull 동기화는 서버 시각으로 이력을 작성하는 별도 경로이며 이번 변경 대상이 아니다.

## 기존 미래 데이터 점검

코드 배포만으로 기존 잘못된 이력/캐시가 복구되지는 않는다. 운영에서 자동 삭제/보정하지 않는다.

1. DB 백업과 복원 가능 여부를 확인하고, 관측값 공급과 지도 동기화를 잠시 중단한다.
2. JVM·AI·DB 기록의 실제 시간대부터 확인한다. 기존 UTC 데이터를 단순히 미래 데이터로 판정하지 않는다.
3. KST로 변환한 현재 시각을 기준으로 아래 읽기 전용 SQL로 후보를 찾는다. `UTC_TIMESTAMP`가 실제 현재 UTC인지 DB 서버 시계도 확인한다.

```sql
SET @audit_now_kst = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 9 HOUR);
SELECT id, spot_id, quiet_score, calculated_at
FROM quiet_index WHERE calculated_at > @audit_now_kst;
SELECT id, tour_api_content_id, current_quiet_score, quiet_score_updated_at
FROM tourist_spot WHERE quiet_score_updated_at > @audit_now_kst;
```

4. 후보별 원본 데이터와 로그를 대조해 잘못된 행 ID 및 올바른 관측 시각을 확정한다. 점검 시점에는 더 이상 미래가 아닌 과거 오염 데이터는 이 SQL만으로 발견되지 않으므로 공급 로그도 확인한다.
5. 승인된 대상만 별도 보관 후 수정/제거한다. 시각 수정 시 `(spot_id, calculated_at)` 중복 및 정정 정책을 확인한다. FK 비활성화나 전체 테이블 삭제는 하지 않는다.
6. 같은 복구 트랜잭션에서 대상 장소를 잠그고, 검증된 과거/현재 이력 중 최신 행으로 `current_quiet_score`와 `quiet_score_updated_at`을 함께 복구한다. 유효 이력이 없으면 둘 다 null로 둔다.
7. 일반 `updateQuietScoreIfNewer`나 과거 값 재전송만으로는 미래 캐시를 되돌릴 수 없다. 승인된 복구 작업으로 해당 캐시를 정정해야 한다. 관측 시각을 현재로 위조하지 않는다.
8. 해당 값에서 파생된 방문 시작 점수·관측 시각과 제안 스냅샷도 영향 여부를 점검한다. 근거 없이 과거 방문 데이터를 일괄 재계산하지 않는다.
9. 커밋 전 수정 대상 건수와 최신 이력/캐시 일치를 확인한다. 공급 재개 후 정상 관측 갱신·목록/상세/타임라인·방문 제안을 확인한다.

운영 데이터 변경은 담당자의 대상 검토와 승인 후 수행한다. 이 이슈 구현에서 운영 DB 복구를 실행하지 않는다.
