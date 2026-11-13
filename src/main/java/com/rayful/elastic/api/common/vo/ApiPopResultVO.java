package com.rayful.elastic.api.common.vo;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
public class ApiPopResultVO {
	
	private Object apiResult;
	
	@Builder
	public ApiPopResultVO(Map<String, Object> popKeywordMap, String returnType, String reqTime, String filePath) {
		
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
		
		// 리턴 타입 JSON
		if(returnType.toUpperCase().indexOf("JSON") > -1) {
			resultMap.put("statis", popKeywordMap);
			
			this.apiResult = resultMap;
		} 
	}
}
