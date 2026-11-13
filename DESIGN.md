# 우장각 통합검색 — 백엔드 교체 설계 (wjg-backend → elastic_api_standard)

## 1. 왜 이런 구조인가

`elastic_api_standard`(RAY.Search)는 **조회 전용**, **설정(application.yml) 기반**의 라이선스 제품이다.
표준 컨트롤러/서비스 로직은 건드리지 않는 것이 이 제품의 설계 원칙이라, 커스터마이징은
① 설정파일 ② 범용 확장 지점(`filterOption`) ③ 프런트엔드 어댑터, 이 세 곳에 최소한으로만 넣었다.

## 2. collapseCd(탭) ↔ 인덱스 설계

wjg-backend 는 프로젝트/지식정보/전사문서/부서문서를 **하나의 인덱스**(`idx_wjg_documents`)에
`area` 필드로 구분해서 저장한다. 이 구조를 그대로 살려서, 5개 collapseCd 가 전부 같은 인덱스를
바라보고 `filterOption.area` 로 탭을 나눈다 (표준 제품의 "1 collapseCd = 1 인덱스" 전제와 호환되면서도
탭 추가/변경이 코드 변경 없이 설정만으로 가능).

| collapseCd | 탭 | area 필터 |
|---|---|---|
| 90000 | 통합검색(ALL) | 없음 |
| 90001 | 프로젝트 | PROJECT |
| 90002 | 지식정보 | KNOWLEDGE |
| 90003 | 전사문서 | COMPANY |
| 90004 | 부서문서 | DEPT |

기존 `99999`(통합검색)는 건드리지 않았다 — `StandardSearchController` 에 "메뉴 인덱스 병합"이
하드코딩되어 있어, 그 부작용을 피하려고 우리 통합검색은 새 코드(90000)를 썼다.

## 3. 변경 파일

| 파일 | 내용 |
|---|---|
| `application-local.yml` | `index-name`/`section-code`/`section-name`/`includes`/`fields`(AREA01~05)/`highlight`/`sort`/`aggs`/`condition` 에 `idx_wjg_documents`·90000~90004 항목 추가 |
| `StandardSearchServiceImpl.java` | `addAggsFilters()`에 `contains`(부분일치)·`range`(숫자/날짜 범위)·`exclude`(NOT) operator 3종 추가. 기존 and/or 로직은 그대로 — 완전히 분리된 분기라 표준 동작에 영향 없음 |

**`application-dev.yml.local`/`application-prod.yml`는 아직 반영 안 됨** — local과 인덱스 목록 구성이 달라
자동 패치 스킵. `application-local.yml`의 추가분(검색 → "90000" 부터)을 그대로 복사해 넣으면 된다.

## 4. 필터 매핑 — 뭐가 되고 뭐가 제약인지

| 우리 필터 | 처리 방식 | 비고 |
|---|---|---|
| 지역/사업분야/업무구분 등 다중선택(칩) | `filterOption.{field} = {operator:'or', values}` | 정상 동작 |
| 단일선택(계약여부/구조형식 등) | `filterOption.{field} = [value]` | 정상 동작 |
| 내부/외부 참여자, 프로젝트자재검색, 면적/규모 값 | `filterOption.{field} = {operator:'contains', value}` | **커스텀 확장 필요**(반영함) |
| 사업비(숫자범위) | `filterOption.{field} = {operator:'range', gte, lte}` | **커스텀 확장 필요**(반영함) |
| 결과에서 제외할 단어(NOT) | `filterOption.content = {operator:'exclude', value}` | **커스텀 확장**, content 필드 기준 근사치(제목/경로 등 다른 필드 단어는 못 거름) |
| 검색영역(파일명/폴더명/내용/작성자) | `srchArea: AREA02~05` | 정상 동작 |
| 기간(작성일 기준) | `regDtmYn/regStartDtm/regEndDtm` | 인덱스당 날짜필드 1개(`modDate`)만 설정 가능 |
| **프로젝트 일정(9개 날짜 필드 중 선택)** | 위와 동일 메커니즘 재사용 | ⚠️ **알려진 제약**: 어떤 날짜 필드를 골라도 실제로는 `modDate` 기준으로 필터링됨. 완전히 지원하려면 `condition` 을 날짜필드별로 다중화하는 추가 커스텀 필요(이번 범위에서는 생략) |
| 키워드(AND) | `kwd` (추가 입력 시 메인 키워드로 합산) | |
| 키워드 없이 필터만 검색 | `kwd: '*'` 로 대체 | ⚠️ **실제 서버/쿼리빌더에서 `*` 가 "전체"로 동작하는지 검증 필요** — 이 제품은 `kwd` 가 필수 파라미터라 우리 기존 "필터만 검색" UX 와 근본적으로 다름 |
| 자동완성(SearchBar) | 미반영 | `StandardAutoController` 별도 계약 — 다음 범위 |

## 5. 색인 데이터 — 로더를 elastic_api_standard 안으로 병합

`elastic_api_standard`는 적재 기능이 없다. 처음엔 별도 패키지(`wjg-loader`)로 분리했었는데,
**이번에 `elastic_api_standard` 프로젝트 안으로 병합**해서 하나의 배포 단위로 합쳤다.

- 위치: `src/main/java/com/rayful/elastic/api/loader/**` (완전히 새 하위패키지, 기존 `standard/*` 코드는 한 줄도 안 건드림)
- 내용물: wjg-backend 의 `IndexInitializer`/`IndexingService`/`SampleDataLoader`/`ProjectDetailSampleData`/
  `SearchDocument`/`ProjectDocument` 등을 패키지 경로만 바꿔 그대로 가져옴(로직 변경 없음)
- 추가 리소스: `src/main/resources/elastic/index-documents.json`, `index-projects.json` (ES 매핑)
- pom.xml: 공식 Elasticsearch Java 클라이언트(`co.elastic.clients:elasticsearch-java:8.15.3`) +
  `jackson-databind` 의존성 추가 — ⚠️ 이 클라이언트는 Jackson **2**(`com.fasterxml.jackson`) 기반인데
  이 프로젝트는 Spring Boot 4라 기본이 Jackson **3**(`tools.jackson`). springdoc(swagger-core)도 이미
  Jackson 2 에 의존해서 공존하고 있던 전례가 있어 호환될 것으로 보이지만, **실제 빌드/기동 검증 필수**
- application-local.yml: 기존 `elasticsearch.host`(표준 검색과 공유)는 그대로 두고,
  로더 전용 인증 정보(`username`/`password`/`trust-self-signed`)만 같은 블록에 추가
- 전역 예외 핸들러(`LoaderIndexException.Handler`)는 `@RestControllerAdvice(basePackages="com.rayful.elastic.api.loader")`
  로 범위를 한정해서 표준 검색 컨트롤러의 기존 에러 처리에 영향 주지 않게 했다

**API** (전부 `CommonInterceptor`의 `X-API-KEY` 헤더 검사 대상 — `search.api-key` 설정값 필요):

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/index/init` | 인덱스 없으면 생성 |
| POST | `/api/index/recreate` | 인덱스 재생성(매핑 변경 시) |
| POST | `/api/index/sample` | **샘플 데이터 적재**(문서 500여건 + 프로젝트 마스터 100건) |
| POST | `/api/index/projects/recreate` | 프로젝트 마스터만 재생성 |
| POST | `/api/index/projects/sample` | 프로젝트 마스터만 재적재 |

```bash
curl -X POST http://localhost:8002/api/index/sample -H "X-API-KEY: <search.api-key 설정값>"
```

~~(이전 버전들: ① 파이썬 스크립트+curl 수동 적재 → ② 별도 wjg-loader 패키지)~~ → 이번 버전은 단일 앱으로 통합.

## 6. 다음에 할 일 (이번 범위 밖)

1. `application-dev.yml.local` / `application-prod.yml` 에도 동일 설정 반영
2. `kwd: '*'` 가 실제로 "전체"로 동작하는지 실제 ES/쿼리빌더로 검증, 필요하면 쿼리빌더 추가 커스텀
3. "프로젝트 일정" 9개 날짜 필드 완전 지원(날짜필드별 range 커스텀)
4. 자동완성(StandardAutoController) 어댑팅
5. 실제 라이선스 키/ES 접속정보로 기동 테스트 (현재 코드는 전부 정적 분석만 거쳤고, 실제 서버에 붙여 돌려보지 못함)
