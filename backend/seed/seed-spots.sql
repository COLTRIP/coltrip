-- 개발용 시드 데이터 (프론트 지도 화면 개발용)
--
-- ⚠️ 주의
-- - tour_api_content_id를 'SEED-xxx'로 넣은 이유: 실제 TourAPI contentId를 모르는 상태라
--   임의값을 넣으면 나중에 AI가 실제 데이터를 적재할 때 충돌/중복이 생길 수 있음.
--   'SEED-' 접두어를 두면 실제 데이터와 구분되고, 나중에 한 줄로 정리 가능:
--   DELETE FROM tourist_spot WHERE tour_api_content_id LIKE 'SEED-%';
-- - 좌표는 대략적인 값. 실제 서비스 데이터는 TourAPI/Geocoding으로 정확한 값이 들어옴.
-- - quiet_score는 QuietLevel 3구간(0~40 CROWDED / 41~70 NORMAL / 71~100 QUIET)이
--   프론트에서 다 보이도록 일부러 흩어놓은 값. AI 배치가 돌면 실제 값으로 덮어씀.
--
-- 실행: mysql -h 127.0.0.1 -u root -p coltrip < backend/seed/seed-spots.sql

INSERT INTO tourist_spot
    (tour_api_content_id, name, address, latitude, longitude, category,
     description, recommend_reason, current_quiet_score, quiet_score_updated_at, created_at, updated_at)
VALUES
    ('SEED-001', '보수동 책방골목', '부산광역시 중구 책방골목길', 35.1020000, 129.0250000, 'BOOKSTORE',
     '헌책방이 늘어선 부산의 오래된 골목.', '책장 넘기는 소리만 남는 조용한 골목으로, 사유하며 걷기 좋습니다.',
     78, NOW(), NOW(), NOW()),

    ('SEED-002', '흰여울문화마을', '부산광역시 영도구 흰여울길', 35.0785000, 129.0435000, 'ALLEY',
     '영도 바닷가 절벽을 따라 이어진 마을.', '바다를 바라보며 천천히 걷기 좋지만 주말에는 붐빕니다.',
     32, NOW(), NOW(), NOW()),

    ('SEED-003', '감천문화마을', '부산광역시 사하구 감내2로', 35.0975000, 129.0107000, 'ALLEY',
     '산비탈을 따라 색색의 집이 늘어선 마을.', '골목이 많아 사람을 피해 조용한 길을 찾기 좋습니다.',
     45, NOW(), NOW(), NOW()),

    ('SEED-004', '해운대해수욕장', '부산광역시 해운대구 우동', 35.1587000, 129.1604000, 'BEACH',
     '부산을 대표하는 해수욕장.', '파도 소리를 들으며 물멍하기 좋으나 성수기에는 매우 혼잡합니다.',
     18, NOW(), NOW(), NOW()),

    ('SEED-005', '광안리해수욕장', '부산광역시 수영구 광안해변로', 35.1532000, 129.1187000, 'BEACH',
     '광안대교 야경으로 유명한 해변.', '밤바다를 바라보기 좋은 곳입니다.',
     37, NOW(), NOW(), NOW()),

    ('SEED-006', '부산시민공원', '부산광역시 부산진구 시민공원로', 35.1697000, 129.0578000, 'PARK',
     '도심 속 대규모 공원.', '넓은 산책로가 여러 갈래로 나뉘어 있어 한적하게 걷기 좋습니다.',
     72, NOW(), NOW(), NOW()),

    ('SEED-007', '용두산공원', '부산광역시 중구 용두산길', 35.1010000, 129.0324000, 'PARK',
     '부산타워가 있는 도심 공원.', '계단을 조금 오르면 도심 소음에서 벗어난 조용한 벤치가 있습니다.',
     64, NOW(), NOW(), NOW()),

    ('SEED-008', '범어사', '부산광역시 금정구 범어사로', 35.2851000, 129.0689000, 'TEMPLE',
     '금정산 자락의 천년 고찰.', '산속에 자리해 소음이 적고 명상하기에 적합합니다.',
     88, NOW(), NOW(), NOW()),

    ('SEED-009', '해동용궁사', '부산광역시 기장군 기장읍 용궁길', 35.1885000, 129.2233000, 'TEMPLE',
     '바다를 마주한 사찰.', '파도 소리와 함께 사유할 수 있는 공간입니다.',
     41, NOW(), NOW(), NOW()),

    ('SEED-010', '부산시립미술관', '부산광역시 해운대구 APEC로', 35.1690000, 129.1310000, 'GALLERY',
     '부산의 대표적인 시립 미술관.', '실내라 날씨 영향이 적고, 평일에는 관람객이 적어 여유롭습니다.',
     81, NOW(), NOW(), NOW()),

    ('SEED-011', '부산광역시립시민도서관', '부산광역시 부산진구 월드컵대로', 35.1730000, 129.0570000, 'LIBRARY',
     '부산에서 가장 오래된 공공도서관.', '정숙이 기본인 공간으로 독서와 사유에 적합합니다.',
     92, NOW(), NOW(), NOW()),

    ('SEED-012', '전포 카페거리', '부산광역시 부산진구 전포대로', 35.1520000, 129.0620000, 'CAFE',
     '개성 있는 카페가 모인 거리.', '골목 안쪽으로 들어가면 사람이 적은 조용한 카페를 찾을 수 있습니다.',
     55, NOW(), NOW(), NOW());

-- 감성모드 매핑 (한 장소가 여러 모드에 해당 가능)
INSERT INTO spot_mode (spot_id, mode)
SELECT s.id, m.mode FROM tourist_spot s
JOIN (
    SELECT 'SEED-001' AS cid, 'CONTEMPLATION' AS mode UNION ALL
    SELECT 'SEED-001', 'CULTURE' UNION ALL
    SELECT 'SEED-002', 'WALK' UNION ALL
    SELECT 'SEED-002', 'SCENERY' UNION ALL
    SELECT 'SEED-002', 'WATER_GAZING' UNION ALL
    SELECT 'SEED-003', 'WALK' UNION ALL
    SELECT 'SEED-003', 'SCENERY' UNION ALL
    SELECT 'SEED-004', 'WATER_GAZING' UNION ALL
    SELECT 'SEED-004', 'WALK' UNION ALL
    SELECT 'SEED-005', 'WATER_GAZING' UNION ALL
    SELECT 'SEED-005', 'SCENERY' UNION ALL
    SELECT 'SEED-006', 'WALK' UNION ALL
    SELECT 'SEED-006', 'CONTEMPLATION' UNION ALL
    SELECT 'SEED-007', 'SCENERY' UNION ALL
    SELECT 'SEED-007', 'WALK' UNION ALL
    SELECT 'SEED-008', 'CONTEMPLATION' UNION ALL
    SELECT 'SEED-008', 'WALK' UNION ALL
    SELECT 'SEED-009', 'CONTEMPLATION' UNION ALL
    SELECT 'SEED-009', 'WATER_GAZING' UNION ALL
    SELECT 'SEED-010', 'CULTURE' UNION ALL
    SELECT 'SEED-010', 'CONTEMPLATION' UNION ALL
    SELECT 'SEED-011', 'CONTEMPLATION' UNION ALL
    SELECT 'SEED-011', 'CULTURE' UNION ALL
    SELECT 'SEED-012', 'CONTEMPLATION'
) m ON s.tour_api_content_id = m.cid;
