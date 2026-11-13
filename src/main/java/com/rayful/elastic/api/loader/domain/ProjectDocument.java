package com.rayful.elastic.api.loader.domain;

/**
 * 프로젝트 마스터 색인(wjg-projects) 문서 모델.
 * "프로젝트 검색 팝업"(다중 선택)이 조회하는 대상이며, wjg-documents 의 projectNo 가 이 문서를 참조한다.
 * (Lombok 미사용 - 명시적 getter/setter, SearchDocument 와 동일한 스타일)
 */
public class ProjectDocument {

    private String projectNo;
    private String projectName;

    /** 발주처 (프로젝트 검색 팝업 목록에 표시) */
    private String clientName;

    /** 진행상태: 진행 / 완료 */
    private String projectStatus;

    /** 지역(시/도) - 워크시트 필터 '지역' 칩 및 문서 색인 비정규화 원본 */
    private String region;

    /** 공사구분 - 워크시트 필터 '공사구분' 칩 및 문서 색인 비정규화 원본 */
    private String constructionType;

    /** 발주처 구분코드 (E-2-2: 기업건/관공서(정부)/공기업/관공서(지자체)/기타) */
    private String clientTypeCode;

    private Integer projectYear;
    private boolean subordinate;
    private String regDate;

    public String getProjectNo() {
        return projectNo;
    }

    public void setProjectNo(String projectNo) {
        this.projectNo = projectNo;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getConstructionType() {
        return constructionType;
    }

    public void setConstructionType(String constructionType) {
        this.constructionType = constructionType;
    }

    public String getClientTypeCode() {
        return clientTypeCode;
    }

    public void setClientTypeCode(String clientTypeCode) {
        this.clientTypeCode = clientTypeCode;
    }

    public Integer getProjectYear() {
        return projectYear;
    }

    public void setProjectYear(Integer projectYear) {
        this.projectYear = projectYear;
    }

    public boolean isSubordinate() {
        return subordinate;
    }

    public void setSubordinate(boolean subordinate) {
        this.subordinate = subordinate;
    }

    public String getRegDate() {
        return regDate;
    }

    public void setRegDate(String regDate) {
        this.regDate = regDate;
    }
/*  public String getProjectNo() { return projectNo; }
    public void setProjectNo(String projectNo) { this.projectNo = projectNo; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getProjectStatus() { return projectStatus; }
    public void setProjectStatus(String projectStatus) { this.projectStatus = projectStatus; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getConstructionType() { return constructionType; }
    public void setConstructionType(String constructionType) { this.constructionType = constructionType; }

    public String getClientTypeCode() { return clientTypeCode; }
    public void setClientTypeCode(String clientTypeCode) { this.clientTypeCode = clientTypeCode; }

    public Integer getProjectYear() { return projectYear; }
    public void setProjectYear(Integer projectYear) { this.projectYear = projectYear; }

    public boolean isSubordinate() { return subordinate; }
    public void setSubordinate(boolean subordinate) { this.subordinate = subordinate; }

    public String getRegDate() { return regDate; }
    public void setRegDate(String regDate) { this.regDate = regDate; }*/
}
