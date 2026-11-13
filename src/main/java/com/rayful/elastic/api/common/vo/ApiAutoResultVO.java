package com.rayful.elastic.api.common.vo;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
public class ApiAutoResultVO {
	
	private Object apiResult;
	
	@Builder
	public ApiAutoResultVO(Map<String, Object> autoMap, String returnType, int totalCnt, int sectionSize, String reqTime) {
		
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
		
		// 리턴 타입 JSON
		if(returnType.toUpperCase().indexOf("JSON") > -1) {
			resultMap.put("totalCnt", totalCnt);
			resultMap.put("sectionSize", sectionSize);
			resultMap.put("sectionMap", autoMap);
			
			this.apiResult = resultMap;
		} 
	}
}
