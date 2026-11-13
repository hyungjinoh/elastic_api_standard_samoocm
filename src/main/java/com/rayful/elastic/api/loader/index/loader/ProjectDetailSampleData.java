package com.rayful.elastic.api.loader.index.loader;

import com.rayful.elastic.api.loader.domain.ProjectDocument;
import com.rayful.elastic.api.loader.domain.SearchDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 프로젝트 탭 상세검색 26개 필터(사업16·시설5·기술4·자료1)용 데모 데이터.
 * "261020_우장각_상세검색_필터항목_구성요청서.xlsx"의 '실제 사용 선택항목 목록'을 그대로 옮긴 값 풀에서
 * 프로젝트별로 결정적(deterministic)으로 값을 골라 배정한다 — region/constructionType 과 동일하게
 * "프로젝트 마스터 속성을 문서 쪽으로 비정규화"하는 패턴을 그대로 따른다(실제로는 색인 파이프라인이
 * 하는 일을 여기서는 고정 데이터 생성으로 대신한다).
 *
 * SampleDataLoader 가 문서(SearchDocument)를 만든 뒤, projectNo 로 이 클래스가 만든 맵을 찾아
 * {@link #applyTo(SearchDocument, int)} 로 복사해 넣는 방식으로 사용한다.
 */
public final class ProjectDetailSampleData {

    private ProjectDetailSampleData() { }

    // --- 사업 그룹 선택항목 풀 ---
    private static final String[] PROGRESS_STATUS = { "진행", "종료", "홀딩", "사전이관완료", "이관완료" };
    private static final String[] BUSINESS_FIELD = {
            "개발1", "개발2", "개발사업", "민간1", "민간2", "민간3", "정비사업", "공공", "공공주택", "민간주택",
            "하이테크", "설계1", "설계2", "설계3", "하이테크설계", "인프라", "해외", "친환경", "연구", "QS", "기타",
    };
    private static final String[] SITE_OPENED = { "개설", "미개설" };
    private static final String[] WORK_TYPE = {
            "건설기술진흥법(CM)", "건축법(상주)", "건축법(비상주)", "건축법(다중이용시설)",
            "주택법(건축감리)", "주택법(전기감리)", "주택법(소방감리)",
            "설계(기획)", "설계(계획)", "설계(기본)", "설계(실시)", "도시계획설계", "마스터플랜(M.P)",
            "사전검토", "개발기획", "자산실사(DD)", "독립기술자문(IE)", "기술제안", "타당성조사",
            "지하안전평가", "교통영향평가", "측량 및 지반조사", "친환경 인증", "기타",
    };
    private static final String[] ORDER_TYPE = {
            "PQ+입찰", "PQ+TP(SOQ)+입찰", "종심제", "기타", "수의계약", "가격제안", "현상설계", "기획제안", "투자참여",
    };
    private static final String[] SERVICE_TYPE = {
            "PCM", "PM", "CM", "감리", "설계", "개발사업", "계획·조사·평가·자문", "연구과제", "본사",
    };
    private static final String[] JOINT_TYPE = { "단독", "공동(주관)", "공동(부관)" };
    private static final String[] INTERNAL_PARTICIPANT_ROLE = { "영업PM", "운영PM", "단장" };
    private static final String[] INTERNAL_PARTICIPANT_NAME = {
            "김상무", "박전무", "신부장", "이부장", "최과장", "홍길동", "김전무", "김부장", "이과장",
    };
    private static final String[] EXTERNAL_PARTICIPANT_ROLE = {
            "수요자", "계약발주자", "원발주자", "허가권자", "감독자", "건설사업관리자", "공동수행사", "시행자", "설계자", "시공자", "컨설팅", "기타",
    };
    private static final String[] CONSTRUCTION_ACTION = {
            "신축", "증축", "재축", "개축", "이전", "대수선", "개보수", "용도변경", "리모델링", "기타",
    };

    // --- 시설 그룹 선택항목 풀 ---
    private static final String[] FACILITY_USE_TYPE = {
            "일반산업시설", "데이터센터", "물류창고시설", "일반업무시설", "상업시설", "대형복합시설", "공동주택",
            "오피스텔", "단독주택(단지)", "병원시설", "연구시설", "교육시설", "문화집회시설", "체육시설",
            "호텔·생활숙박시설", "관광·레저시설", "공원·단지", "인프라", "공공업무시설", "시니어하우징시설", "하이테크시설",
    };
    /** 첨부1.시설_법정건축물용도 시트의 Lv1(대분류)·Lv2(세부용도) 대표 샘플(데모용 축약본) */
    private static final String[][] LEGAL_USE_TYPE = {
            { "단독주택", "단독주택" }, { "단독주택", "다가구주택" },
            { "공동주택", "아파트" }, { "공동주택", "연립주택" }, { "공동주택", "다세대주택" }, { "공동주택", "기숙사" },
            { "제1종 근린생활시설", "소매점" }, { "제2종 근린생활시설", "일반음식점" },
            { "문화 및 집회시설", "공연장" }, { "문화 및 집회시설", "전시장" },
            { "판매시설", "상점" }, { "운수시설", "철도시설" },
            { "의료시설", "병원" }, { "교육연구시설", "학교" }, { "교육연구시설", "연구소" },
            { "업무시설", "공공업무시설" }, { "업무시설", "일반업무시설" },
            { "숙박시설", "관광숙박시설" }, { "공장", null }, { "창고시설", "창고" },
            { "자동차 관련시설", "주차장" }, { "방송통신시설", "데이터센터" },
            { "발전시설", "발전소" }, { "교정시설", "교정시설" },
    };
    private static final String[] AREA_SQM = { "1200.5", "3400.0", "8800.2", "15200.0", "22000.5", "48000.0" };
    private static final String[] SCALE_NUMBER = { "3", "5", "8", "12", "18", "25", "32" };
    private static final String[] DESIGN_CONDITION = {
            "도시계획변경", "공공기여", "지구단위계획수립변경", "기부채납", "DFS", "BIM",
    };

    // --- 기술 그룹 선택항목 풀 ---
    private static final String[] STRUCTURE_TYPE = {
            "철근콘크리트조", "철골철근콘크리트조", "철골조", "조적조", "보강블럭조", "경량철골조", "PC조", "기타",
    };
    private static final String[] STRUCTURE_MATERIAL = { "콘크리트", "철근", "강재", "기타" };
    private static final String[] WORK_PROCESS = {
            "가설공사", "지반·기초공사", "건축공사", "토목공사", "조경공사", "기계설비공사", "전기통신공사", "소방설비공사", "기타공사",
    };
    private static final String[] MATERIAL_KEYWORDS = { "철근", "레미콘", "거푸집", "커튼월", "단열재", "창호", "방수재" };

    // --- 자료 그룹 선택항목 풀 ---
    private static final String[] MAIN_DATA_TYPE = {
            "설계설명서", "설계도면", "내역서", "계산서", "산출서", "공정표", "시공계획서", "백서", "시공동영상", "시공사진", "현장완료보고서", "기타",
    };

    /** 프로젝트 마스터(100건) 전체에 대해 결정적으로 상세검색 속성을 생성한다. */
    public static Map<String, SearchDocument> generate(List<ProjectDocument> projects) {
        Map<String, SearchDocument> byProjectNo = new HashMap<>();
        int i = 0;
        for (ProjectDocument p : projects) {
            byProjectNo.put(p.getProjectNo(), buildOne(i));
            i++;
        }
        return byProjectNo;
    }

    private static SearchDocument buildOne(int i) {
        SearchDocument d = new SearchDocument();

        d.setProgressStatus(List.of(pick(PROGRESS_STATUS, i)));
        d.setBusinessField(List.of(pick(BUSINESS_FIELD, i), pick(BUSINESS_FIELD, i + 7)));
        d.setSiteOpened(pick(SITE_OPENED, i));
        d.setWorkType(List.of(pick(WORK_TYPE, i), pick(WORK_TYPE, i + 5)));
        d.setOrderType(List.of(pick(ORDER_TYPE, i)));
        d.setServiceType(List.of(pick(SERVICE_TYPE, i), pick(SERVICE_TYPE, i + 3)));
        d.setJointType(pick(JOINT_TYPE, i));
        d.setInternalParticipant(pick(INTERNAL_PARTICIPANT_ROLE, i) + " " + pick(INTERNAL_PARTICIPANT_NAME, i));
        d.setExternalParticipant(pick(EXTERNAL_PARTICIPANT_ROLE, i));
        d.setConstructionAction(pick(CONSTRUCTION_ACTION, i));

        d.setContractDate(isoDate(2015, i, 2));
        d.setPreTransferDate(isoDate(2016, i, 5));
        d.setTransferDate(isoDate(2016, i, 8));
        d.setEndDate(isoDate(2024, i, 11));
        d.setPlanApprovalDate(isoDate(2014, i, 1));
        d.setBuildingPermitDate(isoDate(2015, i, 3));
        d.setStartDate(isoDate(2015, i, 6));
        d.setUseApprovalDate(isoDate(2023, i, 9));

        long base = 1_000_000_000L + (long) (i % 50) * 230_000_000L;
        d.setBudgetAmount(base);
        d.setBudgetPerPyeong(base / 300);
        d.setDesignPrice(base / 20);
        d.setDesignPricePerPyeong(base / 6000);
        d.setConstructionCostInitial(base - base / 10);
        d.setConstructionCostInitialPerPyeong((base - base / 10) / 300);
        d.setConstructionCostFinal(base + base / 15);
        d.setConstructionCostFinalPerPyeong((base + base / 15) / 300);

        d.setFacilityUseType(List.of(pick(FACILITY_USE_TYPE, i)));
        String[] legal = LEGAL_USE_TYPE[i % LEGAL_USE_TYPE.length];
        d.setLegalUseTypeLv1(legal[0]);
        d.setLegalUseTypeLv2(legal[1]);

        d.setSiteAreaSqm(pick(AREA_SQM, i));
        d.setSiteAreaPyeong(pick(AREA_SQM, i + 1));
        d.setBuildingAreaSqm(pick(AREA_SQM, i + 2));
        d.setBuildingAreaPyeong(pick(AREA_SQM, i + 3));
        d.setTotalFloorAreaSqm(pick(AREA_SQM, i + 4));
        d.setTotalFloorAreaPyeong(pick(AREA_SQM, i + 5));

        d.setFloorsAboveGround(pick(SCALE_NUMBER, i));
        d.setFloorsBelowGround(pick(SCALE_NUMBER, i + 1));
        d.setHouseholdsApartment(pick(SCALE_NUMBER, i + 2));
        d.setRoomsOfficetel(pick(SCALE_NUMBER, i + 3));
        d.setRoomsLodging(pick(SCALE_NUMBER, i + 4));
        d.setBedsHospital(pick(SCALE_NUMBER, i + 5));
        d.setDataCenterCapacityMw(pick(SCALE_NUMBER, i + 6));

        d.setDesignCondition(pick(DESIGN_CONDITION, i));

        d.setStructureType(pick(STRUCTURE_TYPE, i));
        d.setStructureMaterial(pick(STRUCTURE_MATERIAL, i));
        d.setWorkProcess(pick(WORK_PROCESS, i));
        d.setMaterialKeywords(pick(MATERIAL_KEYWORDS, i) + ", " + pick(MATERIAL_KEYWORDS, i + 2));

        d.setMainDataType(pick(MAIN_DATA_TYPE, i));

        return d;
    }

    /** 대상 문서(doc)에 프로젝트 상세검색 속성(src)을 복사한다. SampleDataLoader 에서 문서 생성 후 호출. */
    public static void applyTo(SearchDocument doc, SearchDocument src) {
        if (doc == null || src == null) return;
        doc.setProgressStatus(src.getProgressStatus());
        doc.setBusinessField(src.getBusinessField());
        doc.setSiteOpened(src.getSiteOpened());
        doc.setWorkType(src.getWorkType());
        doc.setOrderType(src.getOrderType());
        doc.setServiceType(src.getServiceType());
        doc.setJointType(src.getJointType());
        doc.setInternalParticipant(src.getInternalParticipant());
        doc.setExternalParticipant(src.getExternalParticipant());
        doc.setConstructionAction(src.getConstructionAction());
        doc.setContractDate(src.getContractDate());
        doc.setPreTransferDate(src.getPreTransferDate());
        doc.setTransferDate(src.getTransferDate());
        doc.setEndDate(src.getEndDate());
        doc.setPlanApprovalDate(src.getPlanApprovalDate());
        doc.setBuildingPermitDate(src.getBuildingPermitDate());
        doc.setStartDate(src.getStartDate());
        doc.setUseApprovalDate(src.getUseApprovalDate());
        doc.setBudgetAmount(src.getBudgetAmount());
        doc.setBudgetPerPyeong(src.getBudgetPerPyeong());
        doc.setDesignPrice(src.getDesignPrice());
        doc.setDesignPricePerPyeong(src.getDesignPricePerPyeong());
        doc.setConstructionCostInitial(src.getConstructionCostInitial());
        doc.setConstructionCostInitialPerPyeong(src.getConstructionCostInitialPerPyeong());
        doc.setConstructionCostFinal(src.getConstructionCostFinal());
        doc.setConstructionCostFinalPerPyeong(src.getConstructionCostFinalPerPyeong());
        doc.setFacilityUseType(src.getFacilityUseType());
        doc.setLegalUseTypeLv1(src.getLegalUseTypeLv1());
        doc.setLegalUseTypeLv2(src.getLegalUseTypeLv2());
        doc.setSiteAreaSqm(src.getSiteAreaSqm());
        doc.setSiteAreaPyeong(src.getSiteAreaPyeong());
        doc.setBuildingAreaSqm(src.getBuildingAreaSqm());
        doc.setBuildingAreaPyeong(src.getBuildingAreaPyeong());
        doc.setTotalFloorAreaSqm(src.getTotalFloorAreaSqm());
        doc.setTotalFloorAreaPyeong(src.getTotalFloorAreaPyeong());
        doc.setFloorsAboveGround(src.getFloorsAboveGround());
        doc.setFloorsBelowGround(src.getFloorsBelowGround());
        doc.setHouseholdsApartment(src.getHouseholdsApartment());
        doc.setRoomsOfficetel(src.getRoomsOfficetel());
        doc.setRoomsLodging(src.getRoomsLodging());
        doc.setBedsHospital(src.getBedsHospital());
        doc.setDataCenterCapacityMw(src.getDataCenterCapacityMw());
        doc.setDesignCondition(src.getDesignCondition());
        doc.setStructureType(src.getStructureType());
        doc.setStructureMaterial(src.getStructureMaterial());
        doc.setWorkProcess(src.getWorkProcess());
        doc.setMaterialKeywords(src.getMaterialKeywords());
        doc.setMainDataType(src.getMainDataType());
    }

    private static String pick(String[] pool, int i) {
        return pool[Math.floorMod(i, pool.length)];
    }

    private static String isoDate(int baseYear, int i, int monthSeed) {
        int year = baseYear + (i % 9);
        int month = 1 + ((i + monthSeed) % 12);
        int day = 1 + ((i * 3 + monthSeed) % 27);
        return String.format("%04d-%02d-%02dT09:00:00", year, month, day);
    }
}
