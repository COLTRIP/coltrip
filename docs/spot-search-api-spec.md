# 장소명 키워드 검색 API

## 요청

`GET /api/spots/search?keyword=해양&page=0&size=20`

비로그인 허용. 유효한 액세스 토큰이 있으면 본인의 isLiked를 반영한다. 기존 지도 bounding box 조회와 날짜별 예측 추천 API는 변경하지 않는다.

| 파라미터 | 필수 | 정책 |
|---|---|---|
| keyword | Y | 앞뒤 공백 제거 후 1~100자(Java 문자열 길이 기준) |
| page | N | 0부터 시작, 기본 0, 최대 10000 |
| size | N | 기본 20, 1~50 |

- DB에 등록된 장소명 부분 일치 검색. 영문 대소문자는 구분하지 않으며 세부 문자 비교/이름 정렬은 DB collation을 따른다.
- `%`, `_`, `!`는 와일드카드가 아닌 문자 그대로 검색한다. 검색어 내부 공백은 유지한다.
- 지도 영역, 사용자 위치, 날짜, 고요지수 및 감성모드 존재 여부로 검색을 제한하지 않는다.
- 이름 오름차순, 같은 이름이면 장소 ID 오름차순. 동명 장소는 ID와 주소로 구분한다.
- 카테고리/감성 필터, 자동완성, 오타 교정, 최근 검색어 저장은 이번 범위에 포함하지 않는다.
- AI를 호출하지 않고 등록된 관광지 DB만 조회한다.

## 응답

```json
{
  "spots": [
    {
      "id": 42,
      "name": "국립해양박물관",
      "address": "부산광역시 예시 주소",
      "category": "GALLERY",
      "modes": [],
      "imageUrl": null,
      "latitude": 35.1,
      "longitude": 129.1,
      "quietScore": null,
      "quietLevel": null,
      "quietScoreUpdatedAt": null,
      "isLiked": false
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```

장소와 좌표는 응답 형식 설명용이다. spots 항목은 기존 SpotSummaryResponse를 재사용한다. 모드 전체를 반환하며 이미지/점수가 없어도 장소를 제외하지 않는다. 이미지/점수/등급/갱신 시각은 null, 미분류 modes는 빈 배열일 수 있다.

검색 결과 없음은 200, spots=[], totalElements=0, totalPages=0, hasNext=false다. 마지막 페이지를 초과하면 spots=[]지만 전체 건수는 유지한다. page/size는 요청값을 반환한다. 개인화 응답이 공유 캐시에 저장되지 않도록 Cache-Control: no-store를 반환한다.

| HTTP | code | 조건 |
|---|---|---|
| 400 | MissingParameterException | keyword 누락 |
| 400 | InvalidSearchRequestException | 빈 검색어/100자 초과 또는 페이지 범위 오류 |
| 400 | InvalidParameterException | page/size 숫자 형식 오류 |
| 500 | InternalServerError | 예상하지 못한 DB/서버 오류 |

## 조회와 연동

ID를 DB에서 먼저 페이징한 뒤 해당 페이지의 장소와 모든 모드를 fetch join한다. 컬렉션 fetch join에 직접 페이지 제한을 적용하지 않는다. 좋아요는 페이지 ID 목록을 한 번에 조회한다. 이름 부분 일치는 일반 B-tree 인덱스만으로 충분히 최적화되지 않을 수 있어 데이터 증가 시 실행 계획/부하를 확인한다.

프론트는 검색어를 URL 인코딩해 전송하고, 검색어 변경 시 page=0으로 초기화한다. 빈 입력은 호출하지 않고 hasNext일 때만 다음 페이지를 요청한다. 입력 중 호출은 debounce하고 이전 검색의 늦은 응답이 새 결과를 덮어쓰지 않도록 처리한다. 선택한 id로 기존 GET /api/spots/{spotId}를 호출한다. 프론트 UI 구현/실기기 연동은 별도 작업이다.

SpotSearchIntegrationTest에서 실제 저장소/보안 필터를 사용해 부분 일치, 누락 데이터, 전체 모드, 동명 정렬, 페이지, 빈 결과, 특수문자, 검증, 로그인/익명 응답을 확인한다. 운영 DB로 테스트하지 않는다.
