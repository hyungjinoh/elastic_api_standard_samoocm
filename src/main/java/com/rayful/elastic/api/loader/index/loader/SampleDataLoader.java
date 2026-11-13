package com.rayful.elastic.api.loader.index.loader;

import com.rayful.elastic.api.loader.domain.ProjectDocument;
import com.rayful.elastic.api.loader.domain.SearchDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 데모용 샘플 데이터. 요구사항 PPT 화면(배치도.dwg, 감리보고서, 안전 전사문서 등)에 맞춰 구성.
 * 실제 색인 소스(RDBMS/Notes/SFTP)로 교체되는 지점.
 *
 * 목표 총 건수는 {@link #TOTAL_COUNT}(기본 500건)이며, 수기로 작성한 데모 문서(아래 curated 섹션) 외 나머지는
 * ProjectSampleDataLoader 의 프로젝트 마스터(100건)를 그대로 참조해 자동 생성한다(bulkGeneratedFiles).
 * region/constructionType 을 문서마다 손으로 맞추지 않고 마스터에서 그대로 읽어오므로,
 * "실제로는 색인 파이프라인이 프로젝트 마스터 값을 문서 쪽으로 비정규화하는 지점"이라는 설계 의도를
 * 코드로도 그대로 지키게 된다(마스터가 단일 진실 공급원).
 *
 * FILE_TITLE_TEMPLATES 중 상당수는 의도적으로 "배치도"를 포함시켜 두었다 — "배치도" 검색 시
 * 결과가 여러 페이지에 걸쳐 나오는지(페이지네이션 동작) 확인하기 위한 데모 목적이다.
 *
 *
 *
 * 프로젝트(project메서드), 파일(file메서드), 전사문서(companyDoc메서드), 지식관리(knowledge메서드) 메서드를 사용하여 검색용 문서생성함.
 */
public final class SampleDataLoader {

    private SampleDataLoader() { }

    /** 색인할 문서 총 목표 건수 (curated + 자동생성). */
    public static final int TOTAL_COUNT = 500;

    // 파일명 템플릿(확장자 페어). "배치도"가 들어간 항목이 전체의 약 1/3이 되도록 구성했다.
    private static final String[][] FILE_TITLE_TEMPLATES = {
            { "%s 배치도.dwg", "dwg" },              // 배치도
            { "%s 배치도(수정).dwg", "dwg" },          // 배치도
            { "%s 종합배치도.dwg", "dwg" },            // 배치도
            { "부지배치도_%s.dwg", "dwg" },            // 배치도
            { "%s 가설배치도.dwg", "dwg" },            // 배치도
            { "%s 배치평면도.dwg", "dwg" },            // 배치도(동의어 사전에 등록된 표현)
            { "%s 감리보고서.hwp", "hwp" },
            { "%s 착공계.hwp", "hwp" },
            { "%s 준공검사조서.hwp", "hwp" },
            { "%s 안전관리계획서.pdf", "pdf" },
            { "%s 품질관리계획서.pdf", "pdf" },
            { "%s 공정률보고서.xlsx", "xlsx" },
            { "%s 설계변경계약서.pdf", "pdf" },
            { "%s 현장사진대지.pptx", "pptx" },
            { "%s 구조계산서.pdf", "pdf" },
            { "%s 시공계획서.hwp", "hwp" },
            { "%s 하도급계약서.pdf", "pdf" },
            { "%s 산출내역서.xlsx", "xlsx" },
    };

    private static final String[] PERFORMER_POOL = {
            "김상무", "박전무", "신부장", "이부장", "최과장", "홍길동", "김전무", "김부장", "이과장", "정보관리자",
    };
    private static final String[] STANDARD_FOLDER_POOL = {
            "01. 수행(현장-CM)", "02. 수행(본사-CM)", "03. 설계관리", "05. 운영지원", "07. 정산관리", "11. 준공관리",
    };

    public static List<SearchDocument> sampleDocuments() {
        // 검색용 문서목록
        List<SearchDocument> list = new ArrayList<>();

        // 프로젝트 번호를 키로, 프로젝트문서를 값으로 갖는 문서맵 생성.
        // 문서 쪽 region/constructionType 은 프로젝트 마스터 값을 그대로 참조한다(단일 진실 공급원).
        List<ProjectDocument> projects = ProjectSampleDataLoader.sampleProjects();
        Map<String, ProjectDocument> byNo = new HashMap<>();
        for (ProjectDocument p : projects) {
            byNo.put(p.getProjectNo(), p);
        }

        // --- 프로젝트(ENTITY) --- (project no / region / constructionType 은 ProjectSampleDataLoader 와 동일하게 유지)
        list.add(project("P-20059D", "20059D", "양재 로데오 패션타운 신축 감리", 2002,
                false, "김상무", "CONTRACTED", "01. 수행(현장-CM)", "일반",
                "서울특별시", "민간"));
        list.add(project("P-23079S", "23079S", "23-25년 평택 Hook up 감리(삼성전자_평택시)", 2023,
                false, "박전무", "CONTRACTED", "01. 수행(현장-CM)", "일반",
                "경기도", "산업"));
        list.add(project("P-20204M", "20204M", "0000 신축공사", 2020,
                false, "신부장", "CONTRACTED", "01. 수행(현장-CM)", "일반",
                "경기도", "산업"));
        list.add(project("P-20204M-SUB", "20204M-1", "0000 신축공사 (종속-토목)", 2020,
                true, "신부장", "NOT_CONTRACTED", "01. 수행(현장-CM)", "일반",
                "경기도", "산업")); // 종속 프로젝트

        // --- 프로젝트 검색 팝업 다중선택 데모에 맞춘 신규 프로젝트(무주군/우성바움) ---
        list.add(project("P-23078M", "23078M", "무주군 노후하수관로 개량 통합 CM", 2023,
                false, "이부장", "CONTRACTED", "01. 수행(현장-CM)", "일반",
                "전라북도", "공공"));
        list.add(project("P-23077M", "23077M", "우성바움 하수도 증설 통합 CM(공주)", 2023,
                false, "최과장", "CONTRACTED", "01. 수행(현장-CM)", "일반",
                "충청남도", "공공"));

        //###########################################################
        // --- 파일(FILE)
        // [4차 리뷰 반영] F-BAECHI-1/2/4 는 원래 area="PROJECT"(프로젝트 탭 색인)로 되어 있었지만,
        // path 는 "우장각>전사문서>..."(CM업무안내서/법규정보 — 전사문서 라이브러리 참고자료)였다.
        // area 와 path(실제 보관 위치)가 서로 어긋나 있던 기존 샘플데이터 오류 — "프로젝트" 탭 LNB
        // "표준폴더" 집계에 전사문서 경로가 섞여 나오는 문제로 드러났다. 실제 보관 위치(전사문서
        // 라이브러리)에 맞춰 area 를 COMPANY 로 바로잡았다 — "전사문서" 탭에서 정상적으로 검색되고,
        // "프로젝트" 탭 결과/집계에서는 더 이상 나타나지 않는다.
        list.add(file("F-BAECHI-1", "COMPANY", "A01-005-007 배치도,지반층배치도,인동거리배치도.dwg",
                "dwg", 512_000L, "우장각>전사문서>2. 회사문서>03. CM업무안내서>3.3.3 프로젝트 사례>4. 공동주택(공공)",
                "20059D", "양재 로데오 패션타운", 2022, false, "김상무", "CONTRACTED", "01. 수행(현장-CM)",
                "배치도 지반층 인동거리 배치 도면 공동주택 사례", "서울특별시", "민간"));
        list.add(file("F-BAECHI-2", "COMPANY", "A-102, 103, 105 배치도,부분배치도,철거부분 배치도.dwg",
                "dwg", 640_000L, "우장각>전사문서>2. 회사문서>03. CM업무안내서>3.3.3 프로젝트 사례>5. 리모델링",
                "090020D", "법원공무원교육원 생활관", 2022, false, "이과장", "CONTRACTED", "02. 수행(본사-CM)",
                "배치도 부분배치도 철거부분 배치 리모델링 사례", "세종특별자치시", "공공"));
        list.add(file("F-BAECHI-3", "PROJECT", "배치계획서 및 산출내역서(ESC반영-배치변경후)-기성신청용(최종본).xlsx",
                "xlsx", 210_000L, "우장각>프로젝트>표준폴더>2020년>배치검토",
                "20204M", "0000 신축공사", 2020, false, "신부장", "CONTRACTED", "01. 수행(현장-CM)",
                "배치계획서 산출내역서 ESC 배치변경 기성신청 최종본", "경기도", "산업"));
        list.add(file("F-BAECHI-4", "COMPANY", "단열재 배치 관련 법규개정 참고.pdf",
                "pdf", 88_000L, "우장각>전사문서>4. 법규정보",
                null, null, 2020, false, "정보관리자", "NOT_CONTRACTED", "05. 운영지원",
                "단열재 배치 관련 법규개정 벽화 마감재료 방화지장", null, null));

        list.add(file("F-GAMLI-1", "PROJECT", "감리보고서건본.hwp", "hwp", 100_000L,
                "우장각>프로젝트>표준폴더>2020년>20011M 성균관대학교 제2경영관 신축공사 CM>01. 사업관리일반",
                "20011M", "성균관대 제2경영관 신축공사 CM", 2021, false, "홍길동", "CONTRACTED", "01. 수행(현장-CM)",
                "감리 감리보고서 건본 감리업무 종합하여 최종 감리보고서 제출", "서울특별시", "민간"));
        list.add(file("F-GAMLI-2", "PROJECT", "20130221-노량진수협 감리완료보고서.hwp", "hwp", 130_000L,
                "우장각>프로젝트>표준폴더>2008년>08044D 노량진 수산물도매시장 현대화사업 CM>10. 행정관리",
                "08044D", "노량진 수산물도매시장 현대화사업 CM", 2019, false, "김전무", "CONTRACTED", "01. 수행(현장-CM)",
                "감리 감리완료보고서 노량진 수산물 도매시장 현대화", "서울특별시", "공공"));
        list.add(file("F-TAB-1", "PROJECT", "TAB최종보고서(2012.3.12).hwp", "hwp", 260_000L,
                "우장각>프로젝트>표준폴더>2008년>08028D 충주의료원 이전신축 BTL CM>11. 준공관리",
                "08028D", "충주의료원 이전신축 BTL CM", 2020, false, "김부장", "CONTRACTED", "01. 수행(현장-CM)",
                "TAB 최종보고서 수행 중 발생 문제점 조치 결과 정리 건물 관리", "충청북도", "공공"));

        // --- 워크시트필터_프로젝트다중선택_검색_UI 목업(STEP 5-6)의 결과 화면과 맞춘 신규 파일 ---
        list.add(file("F-MUJU-1", "PROJECT", "배치도·지반층배치도.dwg", "dwg", 480_000L,
                "우장각>프로젝트>표준폴더>2023년>23078M 무주군 노후하수관로 개량 통합 CM>03. 설계관리",
                "23078M", "무주군 노후하수관로 개량 통합 CM", 2023, false, "이부장", "CONTRACTED", "01. 수행(현장-CM)",
                "무주군 노후하수관로 개량 배치도 지반층배치도 통합 CM", "전라북도", "공공"));
        list.add(file("F-WOOSEONG-1", "PROJECT", "산출내역서(최종본).xlsx", "xlsx", 190_000L,
                "우장각>프로젝트>표준폴더>2023년>23077M 우성바움 하수도 증설 통합 CM(공주)>07. 정산관리",
                "23077M", "우성바움 하수도 증설 통합 CM(공주)", 2023, false, "최과장", "CONTRACTED", "01. 수행(현장-CM)",
                "우성바움 하수도 증설 공주 산출내역서 최종본 정산", "충청남도", "공공"));
        list.add(file("F-PT-1", "PROJECT", "TAB 최종보고서.hwp", "hwp", 220_000L,
                "우장각>프로젝트>표준폴더>2023년>23079S 23-25년 평택 Hook up 감리>11. 준공관리",
                "23079S", "23-25년 평택 Hook up 감리(삼성전자_평택시)", 2023, false, "박전무", "CONTRACTED", "01. 수행(현장-CM)",
                "삼성 평택 Hook up TAB 최종보고서 준공", "경기도", "산업"));

//###########################################################
        // --- 전사문서(COMPANY / ENTITY) - 안전 관련 ---
        list.add(companyDoc("C-1", "[공지] 안전보건캠페인 - 무재해기 릴레이 실시",
                "안전보건캠페인 무재해기 릴레이 실시 무재해의 결의 안전깃발 릴레이 현장 직원 안전 책임",
                "우장각>전사문서>7. 기타>7.1 공지사항", "정보관리자", 2021));
        list.add(companyDoc("C-2", "[법규제정][전기분야] 전기안전관리법 신규 제정",
                "관리되던 안전관리 내용 일괄 전기안전관리법 분리 안전관리 내용 강화 상세화",
                "우장각>전사문서>5. 최신 기술뉴스", "이도훈", 2020));
        list.add(companyDoc("C-3", "[공지] 현장 안전관리 업무 표준화 지침",
                "코로나19 전염병 위험 지속 현장업무 노고 건설기술진흥법 안전관리 업무수행 지침 안전환경",
                "우장각>전사문서>7. 기타>7.1 공지사항", "정보관리자", 2021));
        list.add(companyDoc("C-4", "[법규개정][건축분야] 단열재 공사 전담감리 배치 관련",
                "방화벽 지붕 단열재 사용 현장 상주 전담감리 배치 창고 공장 내부 외벽 마감재료 방화 지장",
                "우장각>전사문서>4. 법규정보", "정보관리자", 2020));

        // --- 환경부 관련 전사문서 (E-1-2 예시) ---
        list.add(companyDoc("C-ENV-1", "[환경부]환경영향평가등의 대행비용 산정기준(환경부 일부개정안)_정리(2014.05.23).hwp",
                "환경부 환경영향평가 대행비용 산정기준 일부개정안 정리",
                "우장각>전사문서>08. 마케팅본부(민자실)>07. 조판>3. 참고자료", "석연철", 2023));
        list.add(companyDoc("C-ENV-2", "환경부 제4차 지속가능발전기본계획 (2021~2040)",
                "환경부 제4차 지속가능발전 기본계획 2021 2040",
                "우장각>전사문서>08. 마케팅본부", "정보관리자", 2023));

//###########################################################
        // --- 지식정보(KNOWLEDGE / ENTITY) ---
        list.add(knowledge("K-1", "월간 감리보고서 PPT(센트럴시티)",
                "센트럴시티 신세계백화점 강남점 근무 월간 감리보고서 PPT 작성",
                "우장각>지식정보", "이광동", 2014));

//###########################################################
        // --- 폴더(FOLDER) - E-1-5 폴더 검색 ---
        list.add(folder("D-1", "PROJECT", "배치검토",
                "우장각>프로젝트>표준폴더>2020년>20204M 0000 신축공사>배치검토",
                "20204M", "0000 신축공사", 2020, "01. 수행(현장-CM)", "경기도", "산업"));
        list.add(folder("D-2", "PROJECT", "김영길 작업내용",
                "우장각>프로젝트>표준폴더>2019년>192806M 우장각>김영길 작업내용",
                "192806M", "우장각", 2019, "01. 수행(현장-CM)", null, null));

        // --- 페이징 데모용 자동 생성: 목표 TOTAL_COUNT 건에 맞춰 나머지를 프로젝트 마스터(100건) 기반으로 채움 ---
        int bulkCount = Math.max(0, TOTAL_COUNT - list.size());
        list.addAll(generateBulkFiles(projects, bulkCount));

        // --- 프로젝트 탭 상세검색(26개 필터) 데모 속성 적용 ---
        // region/constructionType 과 동일하게, projectNo 를 키로 프로젝트 마스터 쪽 속성을 문서에 복사한다.
        Map<String, SearchDocument> detailByProjectNo = ProjectDetailSampleData.generate(projects);
        for (SearchDocument doc : list) {
            if (doc.getProjectNo() != null) {
                ProjectDetailSampleData.applyTo(doc, detailByProjectNo.get(doc.getProjectNo()));
            }
        }

        return list;
    }

    // ---------------------------------------------------------------- 자동 생성

    // [탭별 개별 인덱스 분리 — 2026-10-23] 원래 자동생성 문서가 전부 area="PROJECT" 로 고정되어 있어서
    // idx_wjg_documents2(지식정보)/3(전사문서)/4(부서문서) 에는 수기로 작성한 curated 문서 몇 건
    // (KNOWLEDGE 1건, COMPANY 6건, DEPT 0건!) 외엔 전혀 데이터가 안 들어갔다. 4개 탭 모두 고르게
    // 데모 데이터가 쌓이도록 area 를 라운드로빈으로 섞는다.
    private static final String[] AREAS = { "PROJECT", "KNOWLEDGE", "COMPANY", "DEPT" };
    private static final String[] AREA_LABELS = { "프로젝트", "지식정보", "전사문서", "부서문서" };

    /**
     * 프로젝트 마스터를 순환 참조하며 ***파일 문서를***** 자동 생성한다.
     * region/constructionType 은 마스터 값을 그대로 사용하므로 문서-프로젝트 간 정합성이 항상 보장된다.
     * area 는 4개 탭에 고르게 분배(라운드로빈)한다 — 어느 탭이든 샘플 데이터가 비어있지 않도록.
     */
    private static List<SearchDocument> generateBulkFiles(List<ProjectDocument> projects, int count) {
        List<SearchDocument> out = new ArrayList<>();
        if (projects.isEmpty() || count <= 0) {
            return out;
        }
        for (int i = 0; i < count; i++) {
            ProjectDocument proj = projects.get(i % projects.size());
            String[] template = FILE_TITLE_TEMPLATES[i % FILE_TITLE_TEMPLATES.length];
            String title = String.format(template[0], proj.getProjectName());
            String ext = template[1];
            String id = "GEN-F-" + String.format("%04d", i + 1);
            String area = AREAS[i % AREAS.length];
            String areaLabel = AREA_LABELS[i % AREAS.length];
            // [4차 리뷰 반영] "프로젝트" 탭(area=PROJECT)으로 돌아간 경우에만 "프로젝트"와 연도 사이에
            // "표준폴더" 1단계를 끼워 넣는다 — 전사문서/부서문서/지식정보는 기존 구조 그대로 둔다.
            String areaFolderSegment = "PROJECT".equals(area) ? areaLabel + ">표준폴더" : areaLabel;

            SearchDocument d = base(id, area, "FILE", title,
                    title.substring(0, title.lastIndexOf('.')) + " " + proj.getProjectName(),
                    "우장각>" + areaFolderSegment + ">" + proj.getProjectYear() + "년>" + proj.getProjectNo() + " " + proj.getProjectName());
            d.setFileType(ext);
            d.setFileSize(80_000L + (long) (i % 40) * 15_000L);
            d.setDownloadUrl("/api/files/" + id);
            d.setProjectNo(proj.getProjectNo());
            d.setProjectName(proj.getProjectName());
            d.setProjectYear(proj.getProjectYear());
            d.setSubordinate(proj.isSubordinate());
            d.setPerformer(PERFORMER_POOL[i % PERFORMER_POOL.length]);
            d.setContractStatus(i % 7 == 0 ? "NOT_CONTRACTED" : "CONTRACTED");
            d.setStandardFolder(STANDARD_FOLDER_POOL[i % STANDARD_FOLDER_POOL.length]);
            d.setRegion(proj.getRegion());
            d.setConstructionType(proj.getConstructionType());
            out.add(d);
        }
        return out;
    }

    // ---------------------------------------------------------------- helpers

    private static SearchDocument project(String id, String no, String name, int year,
                                          boolean subordinate, String performer, String contract,
                                          String folder, String security,
                                          String region, String constructionType) {
        // [4차 리뷰 반영] "프로젝트" 탭 경로는 "프로젝트"와 연도 사이에 "표준폴더" 1단계가 더 들어간다.
        SearchDocument d = base(id, "PROJECT", "ENTITY", name,
                name + " " + no, "우장각>프로젝트>표준폴더>" + year + "년>" + no + " " + name);
        d.setProjectNo(no);
        d.setProjectName(name);
        d.setProjectYear(year);
        d.setSubordinate(subordinate);
        d.setPerformer(performer);
        d.setContractStatus(contract);
        d.setStandardFolder(folder);
        d.setSecurityLevel(security);
        d.setRegion(region);
        d.setConstructionType(constructionType);
        return d;
    }

    private static SearchDocument file(String id, String area, String title, String fileType,
                                       long size, String path, String projectNo, String projectName,
                                       int year, boolean sub, String performer, String contract,
                                       String folder, String content, String region, String constructionType) {
        SearchDocument d = base(id, area, "FILE", title, content, path);
        d.setFileType(fileType);
        d.setFileSize(size);
        d.setDownloadUrl("/api/files/" + id);
        d.setProjectNo(projectNo);
        d.setProjectName(projectName);
        d.setProjectYear(year);
        d.setSubordinate(sub);
        d.setPerformer(performer);
        d.setContractStatus(contract);
        d.setStandardFolder(folder);
        d.setRegion(region);
        d.setConstructionType(constructionType);
        return d;
    }

    private static SearchDocument companyDoc(String id, String title, String content, String path,
                                             String author, int year) {
        SearchDocument d = base(id, "COMPANY", "ENTITY", title, content, path);
        d.setAuthor(author);
        d.setProjectYear(year);
        return d;
    }

    private static SearchDocument knowledge(String id, String title, String content, String path,
                                            String author, int year) {
        SearchDocument d = base(id, "KNOWLEDGE", "ENTITY", title, content, path);
        d.setAuthor(author);
        d.setProjectYear(year);
        return d;
    }

    private static SearchDocument folder(String id, String area, String title, String path,
                                         String projectNo, String projectName, int year, String folder,
                                         String region, String constructionType) {
        SearchDocument d = base(id, area, "FOLDER", title, title, path);
        d.setProjectNo(projectNo);
        d.setProjectName(projectName);
        d.setProjectYear(year);
        d.setStandardFolder(folder);
        d.setRegion(region);
        d.setConstructionType(constructionType);
        return d;
    }

    private static SearchDocument base(String id, String area, String docType,
                                       String title, String content, String path) {
        SearchDocument d = new SearchDocument();
        d.setId(id);
        d.setArea(area);
        d.setDocType(docType);
        d.setTitle(title);
        d.setContent(content);
        d.setPath(path);
        d.setPathLv4(truncatePathLevels(path, 4));
        d.setRegDate("2023-08-01T09:00:00");
        d.setModDate("2023-08-30T15:00:00");
        d.setSecurityLevel("일반");
        d.setDeleted(false);
        return d;
    }

    /**
     * [4차 리뷰 반영: 3단계 → 4단계] "우장각>프로젝트>표준폴더>2023년>AUTO-0028 ..." 같은 전체 경로를
     * 앞에서부터 levels 단계까지만 잘라 돌려준다("우장각>프로젝트>표준폴더>2023년"). 실제 색인
     * 파이프라인(ECM 등에서 데이터를 가져와 적재하는 쪽)에서도 path 를 설정하는 지점에 이 로직을
     * 똑같이 적용해서 pathLv4 를 채워야 한다 — 이 샘플 로더만 고치면 샘플 데이터 색인에만 반영되고
     * 실 데이터에는 반영되지 않는다. "프로젝트" 탭 문서는 path 자체에 "프로젝트"와 연도 사이에
     * "표준폴더"라는 고정 폴더 1단계가 실제로 더 들어 있어야 한다(ECM 폴더 트리 실구조와 일치).
     */
    private static String truncatePathLevels(String path, int levels) {
        if (path == null || path.isEmpty()) return path;
        String[] parts = path.split(">", -1);
        if (parts.length <= levels) return path;
        return String.join(">", java.util.Arrays.copyOfRange(parts, 0, levels));
    }
}
