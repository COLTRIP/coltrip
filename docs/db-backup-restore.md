# 운영 DB 백업 / 복원 절차

관련 이슈: #47

## 백업

- 위치: 운영 서버(`ubuntu@api.coltrip.co.kr`) `~/scripts/backup-db.sh`
- 방식: `mysqldump --single-transaction --quick --routines --triggers --no-tablespaces` 후 gzip 압축
- 저장 경로: `~/backups/mysql/coltrip_YYYYMMDD_HHMMSS.sql.gz`
- 자동 실행: cron, 매일 UTC 18:00(한국시간 새벽 3시) — `crontab -l`로 확인 가능
- 보관 기간: 14일. 그보다 오래된 백업 파일은 실행 시 자동 삭제
- 인증: `~/.my.cnf`(권한 600, `coltrip` 계정)에 접속정보 저장 — 스크립트/크론탭에 비밀번호 평문 노출 없음
- 로그: `~/backups/backup.log`

### 수동 백업

배포 직전에는 항상 수동으로 한 번 더 백업 실행:

```bash
ssh -i ~/.ssh/coltrip-key.pem ubuntu@52.78.79.67
~/scripts/backup-db.sh
```

`backup ok: ...` 메시지와 파일 크기가 출력되면 정상.

### 현재 한계

- 백업 파일이 **서버 로컬 디스크**에만 저장됨 (S3 등 외부 저장소 미연동). 인스턴스/볼륨 자체가 손상되면 백업도 함께 유실됨 — 캡스톤 규모에서는 당장 허용 가능한 리스크로 판단. 장기 운영 시 S3 업로드(`aws s3 cp`) 추가 필요
- 복제/실시간 백업(binlog 기반 PITR)은 없음 — 마지막 일일 백업 시점 이후 데이터는 복구 불가

## 복원

1. 서버 접속 후 최신(또는 복원하려는 시점의) 백업 파일 확인
   ```bash
   ls -lt ~/backups/mysql/ | head -5
   ```
2. **애플리케이션 중지** (복원 중 쓰기가 섞이면 안 됨)
   ```bash
   sudo systemctl stop coltrip-backend
   ```
3. 복원 전 현재 상태도 안전하게 한 번 백업(덮어쓰기 실수 대비)
   ```bash
   ~/scripts/backup-db.sh
   ```
4. 대상 파일 복원
   ```bash
   gunzip -c ~/backups/mysql/coltrip_20260913_055723.sql.gz | mysql --defaults-file=~/.my.cnf coltrip
   ```
5. 애플리케이션 재기동 및 확인
   ```bash
   sudo systemctl start coltrip-backend
   sleep 10
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/swagger-ui/index.html
   ```
6. 운영 주소로 실제 데이터 확인
   ```bash
   curl -s "https://api.coltrip.co.kr/api/spots?swLat=35.0&swLng=129.0&neLat=35.3&neLng=129.3" | head -c 300
   ```

## 배포 전후 상태 확인 절차

재배포(`git pull` → 빌드 → `systemctl restart`)할 때마다:

1. **배포 전**: `~/scripts/backup-db.sh` 수동 실행
2. 배포 전 테이블별 행 수 기록 (이상 유무 비교용)
   ```bash
   mysql --defaults-file=~/.my.cnf coltrip -e "
     SELECT 'user', COUNT(*) FROM user
     UNION ALL SELECT 'tourist_spot', COUNT(*) FROM tourist_spot
     UNION ALL SELECT 'visit', COUNT(*) FROM visit
     UNION ALL SELECT 'review', COUNT(*) FROM review;"
   ```
3. 배포 진행
4. **배포 후**: 위 카운트 다시 확인해 예상치 못한 행 손실이 없는지 비교, `/swagger-ui/index.html` 200 확인

## `ddl-auto` 운영 정책

- 현재 `spring.jpa.hibernate.ddl-auto: update`로 운영 중 (Hibernate가 엔티티 변경분을 자동으로 스키마에 반영)
- **결정 (2026-09-13)**: 캡스톤 프로젝트 규모·일정상 `update`를 계속 유지한다. 근거:
  - 정식 마이그레이션 도구(Flyway/Liquibase) 도입은 남은 일정 대비 과함
  - 위 자동 백업 체계로 스키마 변경 실수 시에도 전날 데이터로 복구 가능해 리스크 완화됨
  - `update`는 컬럼 삭제/타입 변경은 자동 반영하지 않음 — 그런 변경은 여전히 수동 `ALTER TABLE` 필요 (예: 과거 `quiet_feedback` 컬럼 정리 사례)
- 컬럼 삭제가 필요한 스키마 변경을 만들 때는 PR에 필요한 수동 `ALTER TABLE` 문을 반드시 명시하고, 배포 시 위 백업 절차를 거친 뒤 수동 실행한다
- 장기 운영으로 전환된다면 그때 Flyway 등 정식 마이그레이션 도구 도입을 재검토한다
