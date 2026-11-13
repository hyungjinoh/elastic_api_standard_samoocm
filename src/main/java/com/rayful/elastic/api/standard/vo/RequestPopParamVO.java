package com.rayful.elastic.api.standard.vo;

import lombok.Data;

@Data
public class RequestPopParamVO {
	
	private String rqType;				// auto: 스케줄러 데이터, manul: 요청조건 데이터
										// rqType이 auto 인 경우 [ period, size ] 무시 - application.yml에 지정된 대로 제공
	
	private String sourceType;			// db: 데이터 베이스 통계 요청, es: 검색엔진 통계 요청, all: db, es둘다 요청  -> default all
	private int period;					// 인기검색어 기간 - 현재일 기준으로 가져올 기간 지정 예) 한달이면 30, 일주일이면 7 -> default 30
	private int size;					// 인기검색어 수 -> default 10

	@Override
	public String toString() {
		return "RequestPopParamVO [rqType=" + rqType 
				+ ", sourceType=" + sourceType 
				+ ", period=" + period 
				+ ", size=" + size 
				+ "]";
	}
	
	public String validateAndSetDefaults() {
       
		if (rqType == null || rqType.isEmpty()) {
			rqType = "auto"; // 기본값 설정
        }
        
        if (sourceType == null || sourceType.isEmpty()) {
        	sourceType = "all";
        }
        
        if (period == 0) {
        	period = 30; // 기본값 설정
        }
        
        if (size == 0) {
            size = 10; // 기본값 설정
        }
        
        return null;
    }
}
