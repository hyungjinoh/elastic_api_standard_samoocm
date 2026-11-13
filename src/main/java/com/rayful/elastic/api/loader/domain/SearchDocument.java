package com.rayful.elastic.api.loader.domain;

import java.util.List;

/**
 * Elasticsearch 에 색인되는 통합검색 문서 모델.
 * 프로젝트/지식정보/전사문서/부서문서 및 각 영역의 파일/폴더를 하나의 스키마로 표현한다.
 * (Lombok 미사용 - 명시적 getter/setter)
 *
 * 2026-10-20 "상세검색 필터항목 구성요청서" 반영: 프로젝트 탭 상세검색 26개 필터(사업16·시설5·기술4·자료1)를
 * 지원하기 위한 필드를 추가했다. region/constructionType 과 동일한 방식으로, 색인 시점에
 * 프로젝트 마스터 속성을 문서 쪽으로 비정규화하여 복사해 둔다(조인 없이 문서 검색 한 번으로 필터링).
 */
public class SearchDocument {

    private String id;

    /** 검색 영역(탭): PROJECT / KNOWLEDGE / COMPANY / DEPT */
    private String area;

    /**
     * [탭별 개별 인덱스 분리] 이 문서가 적재된 인덱스에 대응하는 elastic_api_standard collapseCd 값.
     * area 에서 1:1로 결정됨(PROJECT→90001, KNOWLEDGE→90002, COMPANY→90003, DEPT→90004) —
     * IndexingService.bulkIndex() 에서 색인 시점에 채워 넣는다. (IndexNames.collapseCdFor 참고)
     */
    private String collapsedCd;

    /** 문서 종류: ENTITY(프로젝트/게시글 본문) / FILE(첨부·프로젝트 파일) / FOLDER(폴더) */
    private String docType;

    private String title;
    /** 본문/미리보기 텍스트(색인용) */
    private String content;
    /**
     * 문서 경로: 예) 우장각>프로젝트>표준폴더>2020년>20204M 신축공사>01.수행(현장-CM)
     * [4차 리뷰 반영] "프로젝트" 탭(area=PROJECT) 문서는 "프로젝트"와 연도 사이에 "표준폴더"라는
     * 고정 폴더 1단계가 더 끼어 있다(실제 ECM 폴더 트리 구조와 맞추기 위함). 다른 탭(전사문서/
     * 부서문서/지식정보)은 이 규칙이 적용되지 않는다.
     */
    private String path;

    /**
     * [4차 리뷰 반영: pathLv3(3단계) → pathLv4(4단계)] path 를 앞 4단계까지만 잘라서 저장한
     * 파셋 전용 필드. LNB 표시 이름도 "폴더경로" → "표준폴더"로 바뀌었다.
     * 예) path 가 "우장각>프로젝트>표준폴더>2023년>AUTO-0028 충청남도 2023년 자동생성 프로젝트 028"이면
     *     pathLv4 는 "우장각>프로젝트>표준폴더>2023년".
     * "프로젝트" 탭(area=PROJECT) 문서는 "프로젝트"와 연도 사이에 "표준폴더"라는 고정 폴더 1단계가
     * 실제로 더 끼어 있어야 한다(ECM 폴더 트리 실구조와 일치시키기 위함) — SampleDataLoader 의
     * curated/자동생성 경로 모두 이 규칙을 따른다. 전사문서/부서문서/지식정보는 해당 없음(기존 구조 유지).
     * 검색 결과 LNB 파셋에서 "같은 4단계 폴더"끼리 묶어 집계하기 위한 keyword 전용 필드 —
     * path(text, 형태소 분석)로는 terms 집계를 걸어도 폴더명이 토큰 단위로 쪼개져 묶이지 않으므로,
     * 색인 시점에 미리 잘라 keyword 로 저장해 둔다(SampleDataLoader.truncatePathLevels() 참고).
     */
    private String pathLv4;

    // 프로젝트 관련
    private String projectNo;
    private String projectName;
    private Integer projectYear;
    /** 종속 프로젝트 여부 (E-1-4: 종속 제외 필터) */
    private boolean subordinate;

    /**
     * 지역/공사구분 (워크시트필터_프로젝트다중선택_검색_UI E-2-2 확장).
     * 프로젝트 마스터(wjg-projects)의 속성을 색인 시점에 문서 쪽으로 비정규화하여 복사한 값.
     * ES 는 조인이 비싸므로, "지역이 서울인 프로젝트에 속한 문서" 같은 필터를 문서 검색 한 번에
     * 처리하려면 이렇게 부모(프로젝트) 속성을 자식(문서)에 미리 복제해 두는 편이 실무적이다.
     */
    private String region;
    private String constructionType;

    // 사람
    private String performer; // 수행자
    private String author;    // 작성자

    // 파일
    private String fileType;  // hwp/pdf/dwg/xlsx ...
    private Long fileSize;    // byte
    private String downloadUrl;

    // 날짜 (E-2-5: 등록일/수정일 구분 검색)
    private String regDate;   // yyyy-MM-dd'T'HH:mm:ss
    private String modDate;

    // 기타 조건
    private String contractStatus; // 계약여부: CONTRACTED / NOT_CONTRACTED (E-2-5, 상세검색 "계약여부" 재사용)
    private String standardFolder; // 표준폴더 분류 (사이드바 트리)
    private String securityLevel;  // 보안등급

    /** E-2-6 대응: 색인은 남아있으나 원본 삭제/이동된 문서 표시용 */
    private boolean deleted;

    // =====================================================================
    // 프로젝트 탭 상세검색(26개 필터) — "261020_우장각_상세검색_필터항목_구성요청서.xlsx" 반영
    // 그룹: 사업(16) · 시설(5) · 기술(4) · 자료(1) — 기존 "업무" 그룹은 삭제
    // =====================================================================

    // --- 사업 그룹 ---
    /** 진행상황(다중선택): 진행/종료/홀딩/사전이관완료/이관완료 */
    private List<String> progressStatus;
    /** 참여정보-사업분야(다중선택): 개발1/개발2/민간1 ... */
    private List<String> businessField;
    /** 참여정보-현장개설여부(단일선택): 개설/미개설 */
    private String siteOpened;
    /** 참여정보-업무구분(다중선택): 건설기술진흥법(CM) ... */
    private List<String> workType;
    /** 참여정보-수주형태(다중선택): PQ+입찰 ... */
    private List<String> orderType;
    /** 참여정보-용역구분(다중선택): PCM/PM/CM/감리/설계 ... */
    private List<String> serviceType;
    /** 참여정보-공동이행방식(단일선택): 단독/공동(주관)/공동(부관) */
    private String jointType;
    /** 참여정보-참여자(내부, 직접입력텍스트): 영업PM 홍길동 형태의 자유 텍스트 */
    private String internalParticipant;
    /** 참여정보-참여자(외부, 직접입력텍스트) */
    private String externalParticipant;
    /** 사업유형-건축행위(단일선택): 신축/증축/재축/개축/이전/대수선/개보수/용도변경/리모델링/기타 */
    private String constructionAction;

    // 공정 - 프로젝트 일정(날짜범위, 기준 필드 선택형). regDate(등록일자)는 기존 필드 재사용.
    private String contractDate;       // 계약일자
    private String preTransferDate;    // 사전이관일자
    private String transferDate;       // 이관일자
    private String endDate;            // 종료일자
    private String planApprovalDate;   // 사업계획승인일자
    private String buildingPermitDate; // 건축허가일자
    private String startDate;          // 착공일자
    private String useApprovalDate;    // 사용승인일자

    // 원가 - 사업비(숫자범위, 기준 필드 선택형)
    private Long budgetAmount;                    // 사업비
    private Long budgetPerPyeong;                  // 평당 사업비
    private Long designPrice;                      // 설계가격
    private Long designPricePerPyeong;              // 평당 설계가격
    private Long constructionCostInitial;           // 공사비(최초)
    private Long constructionCostInitialPerPyeong;  // 평당 공사비(최초)
    private Long constructionCostFinal;             // 공사비(최종)
    private Long constructionCostFinalPerPyeong;    // 평당 공사비(최종)

    // --- 시설 그룹 ---
    /** 용도시설구분(다중선택): 일반산업시설/데이터센터 ... */
    private List<String> facilityUseType;
    /** 법정용도-법정건축물용도(단일선택, Lv1/Lv2 계층): 첨부1.시설_법정건축물용도 시트 참고 */
    private String legalUseTypeLv1;
    private String legalUseTypeLv2;

    // 면적(직접입력텍스트, 기준 필드 선택형)
    private String siteAreaSqm;          // 대지면적(㎡)
    private String siteAreaPyeong;       // 대지면적(평)
    private String buildingAreaSqm;      // 건축면적(㎡)
    private String buildingAreaPyeong;   // 건축면적(평)
    private String totalFloorAreaSqm;    // 연면적(㎡)
    private String totalFloorAreaPyeong; // 연면적(평)

    // 규모(직접입력텍스트, 기준 필드 선택형)
    private String floorsAboveGround;    // 층(지상)
    private String floorsBelowGround;    // 층(지하)
    private String householdsApartment;  // 세대(공동주택)
    private String roomsOfficetel;       // 실(오피스텔/기숙사)
    private String roomsLodging;         // 객실(숙박시설)
    private String bedsHospital;         // 병상(병원)
    private String dataCenterCapacityMw; // MW(데이터센터 수전용량)

    /** 설계조건(단일선택): 도시계획변경/공공기여/지구단위계획수립변경/기부채납/DFS/BIM */
    private String designCondition;

    // --- 기술 그룹 ---
    /** 구조-구조형식(단일선택) */
    private String structureType;
    /** 재료-구조재료(단일선택) */
    private String structureMaterial;
    /** 공종-공종별 공법(단일선택) */
    private String workProcess;
    /** 프로젝트자재검색(직접입력텍스트) */
    private String materialKeywords;

    // --- 자료 그룹 ---
    /** 주요자료(단일선택) */
    private String mainDataType;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getCollapsedCd() { return collapsedCd; }
    public void setCollapsedCd(String collapsedCd) { this.collapsedCd = collapsedCd; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getPathLv4() { return pathLv4; }
    public void setPathLv4(String pathLv4) { this.pathLv4 = pathLv4; }

    public String getProjectNo() { return projectNo; }
    public void setProjectNo(String projectNo) { this.projectNo = projectNo; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Integer getProjectYear() { return projectYear; }
    public void setProjectYear(Integer projectYear) { this.projectYear = projectYear; }

    public boolean isSubordinate() { return subordinate; }
    public void setSubordinate(boolean subordinate) { this.subordinate = subordinate; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getConstructionType() { return constructionType; }
    public void setConstructionType(String constructionType) { this.constructionType = constructionType; }

    public String getPerformer() { return performer; }
    public void setPerformer(String performer) { this.performer = performer; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getRegDate() { return regDate; }
    public void setRegDate(String regDate) { this.regDate = regDate; }

    public String getModDate() { return modDate; }
    public void setModDate(String modDate) { this.modDate = modDate; }

    public String getContractStatus() { return contractStatus; }
    public void setContractStatus(String contractStatus) { this.contractStatus = contractStatus; }

    public String getStandardFolder() { return standardFolder; }
    public void setStandardFolder(String standardFolder) { this.standardFolder = standardFolder; }

    public String getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public List<String> getProgressStatus() { return progressStatus; }
    public void setProgressStatus(List<String> progressStatus) { this.progressStatus = progressStatus; }

    public List<String> getBusinessField() { return businessField; }
    public void setBusinessField(List<String> businessField) { this.businessField = businessField; }

    public String getSiteOpened() { return siteOpened; }
    public void setSiteOpened(String siteOpened) { this.siteOpened = siteOpened; }

    public List<String> getWorkType() { return workType; }
    public void setWorkType(List<String> workType) { this.workType = workType; }

    public List<String> getOrderType() { return orderType; }
    public void setOrderType(List<String> orderType) { this.orderType = orderType; }

    public List<String> getServiceType() { return serviceType; }
    public void setServiceType(List<String> serviceType) { this.serviceType = serviceType; }

    public String getJointType() { return jointType; }
    public void setJointType(String jointType) { this.jointType = jointType; }

    public String getInternalParticipant() { return internalParticipant; }
    public void setInternalParticipant(String internalParticipant) { this.internalParticipant = internalParticipant; }

    public String getExternalParticipant() { return externalParticipant; }
    public void setExternalParticipant(String externalParticipant) { this.externalParticipant = externalParticipant; }

    public String getConstructionAction() { return constructionAction; }
    public void setConstructionAction(String constructionAction) { this.constructionAction = constructionAction; }

    public String getContractDate() { return contractDate; }
    public void setContractDate(String contractDate) { this.contractDate = contractDate; }

    public String getPreTransferDate() { return preTransferDate; }
    public void setPreTransferDate(String preTransferDate) { this.preTransferDate = preTransferDate; }

    public String getTransferDate() { return transferDate; }
    public void setTransferDate(String transferDate) { this.transferDate = transferDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getPlanApprovalDate() { return planApprovalDate; }
    public void setPlanApprovalDate(String planApprovalDate) { this.planApprovalDate = planApprovalDate; }

    public String getBuildingPermitDate() { return buildingPermitDate; }
    public void setBuildingPermitDate(String buildingPermitDate) { this.buildingPermitDate = buildingPermitDate; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getUseApprovalDate() { return useApprovalDate; }
    public void setUseApprovalDate(String useApprovalDate) { this.useApprovalDate = useApprovalDate; }

    public Long getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(Long budgetAmount) { this.budgetAmount = budgetAmount; }

    public Long getBudgetPerPyeong() { return budgetPerPyeong; }
    public void setBudgetPerPyeong(Long budgetPerPyeong) { this.budgetPerPyeong = budgetPerPyeong; }

    public Long getDesignPrice() { return designPrice; }
    public void setDesignPrice(Long designPrice) { this.designPrice = designPrice; }

    public Long getDesignPricePerPyeong() { return designPricePerPyeong; }
    public void setDesignPricePerPyeong(Long designPricePerPyeong) { this.designPricePerPyeong = designPricePerPyeong; }

    public Long getConstructionCostInitial() { return constructionCostInitial; }
    public void setConstructionCostInitial(Long constructionCostInitial) { this.constructionCostInitial = constructionCostInitial; }

    public Long getConstructionCostInitialPerPyeong() { return constructionCostInitialPerPyeong; }
    public void setConstructionCostInitialPerPyeong(Long constructionCostInitialPerPyeong) { this.constructionCostInitialPerPyeong = constructionCostInitialPerPyeong; }

    public Long getConstructionCostFinal() { return constructionCostFinal; }
    public void setConstructionCostFinal(Long constructionCostFinal) { this.constructionCostFinal = constructionCostFinal; }

    public Long getConstructionCostFinalPerPyeong() { return constructionCostFinalPerPyeong; }
    public void setConstructionCostFinalPerPyeong(Long constructionCostFinalPerPyeong) { this.constructionCostFinalPerPyeong = constructionCostFinalPerPyeong; }

    public List<String> getFacilityUseType() { return facilityUseType; }
    public void setFacilityUseType(List<String> facilityUseType) { this.facilityUseType = facilityUseType; }

    public String getLegalUseTypeLv1() { return legalUseTypeLv1; }
    public void setLegalUseTypeLv1(String legalUseTypeLv1) { this.legalUseTypeLv1 = legalUseTypeLv1; }

    public String getLegalUseTypeLv2() { return legalUseTypeLv2; }
    public void setLegalUseTypeLv2(String legalUseTypeLv2) { this.legalUseTypeLv2 = legalUseTypeLv2; }

    public String getSiteAreaSqm() { return siteAreaSqm; }
    public void setSiteAreaSqm(String siteAreaSqm) { this.siteAreaSqm = siteAreaSqm; }

    public String getSiteAreaPyeong() { return siteAreaPyeong; }
    public void setSiteAreaPyeong(String siteAreaPyeong) { this.siteAreaPyeong = siteAreaPyeong; }

    public String getBuildingAreaSqm() { return buildingAreaSqm; }
    public void setBuildingAreaSqm(String buildingAreaSqm) { this.buildingAreaSqm = buildingAreaSqm; }

    public String getBuildingAreaPyeong() { return buildingAreaPyeong; }
    public void setBuildingAreaPyeong(String buildingAreaPyeong) { this.buildingAreaPyeong = buildingAreaPyeong; }

    public String getTotalFloorAreaSqm() { return totalFloorAreaSqm; }
    public void setTotalFloorAreaSqm(String totalFloorAreaSqm) { this.totalFloorAreaSqm = totalFloorAreaSqm; }

    public String getTotalFloorAreaPyeong() { return totalFloorAreaPyeong; }
    public void setTotalFloorAreaPyeong(String totalFloorAreaPyeong) { this.totalFloorAreaPyeong = totalFloorAreaPyeong; }

    public String getFloorsAboveGround() { return floorsAboveGround; }
    public void setFloorsAboveGround(String floorsAboveGround) { this.floorsAboveGround = floorsAboveGround; }

    public String getFloorsBelowGround() { return floorsBelowGround; }
    public void setFloorsBelowGround(String floorsBelowGround) { this.floorsBelowGround = floorsBelowGround; }

    public String getHouseholdsApartment() { return householdsApartment; }
    public void setHouseholdsApartment(String householdsApartment) { this.householdsApartment = householdsApartment; }

    public String getRoomsOfficetel() { return roomsOfficetel; }
    public void setRoomsOfficetel(String roomsOfficetel) { this.roomsOfficetel = roomsOfficetel; }

    public String getRoomsLodging() { return roomsLodging; }
    public void setRoomsLodging(String roomsLodging) { this.roomsLodging = roomsLodging; }

    public String getBedsHospital() { return bedsHospital; }
    public void setBedsHospital(String bedsHospital) { this.bedsHospital = bedsHospital; }

    public String getDataCenterCapacityMw() { return dataCenterCapacityMw; }
    public void setDataCenterCapacityMw(String dataCenterCapacityMw) { this.dataCenterCapacityMw = dataCenterCapacityMw; }

    public String getDesignCondition() { return designCondition; }
    public void setDesignCondition(String designCondition) { this.designCondition = designCondition; }

    public String getStructureType() { return structureType; }
    public void setStructureType(String structureType) { this.structureType = structureType; }

    public String getStructureMaterial() { return structureMaterial; }
    public void setStructureMaterial(String structureMaterial) { this.structureMaterial = structureMaterial; }

    public String getWorkProcess() { return workProcess; }
    public void setWorkProcess(String workProcess) { this.workProcess = workProcess; }

    public String getMaterialKeywords() { return materialKeywords; }
    public void setMaterialKeywords(String materialKeywords) { this.materialKeywords = materialKeywords; }

    public String getMainDataType() { return mainDataType; }
    public void setMainDataType(String mainDataType) { this.mainDataType = mainDataType; }
}
