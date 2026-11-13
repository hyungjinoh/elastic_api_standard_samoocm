package com.rayful.elastic.api.common.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
public class ApiResultVO {
	
	private Object apiResult;
	
	@SuppressWarnings("unchecked")
	@Builder
	public ApiResultVO(List<Map<String, Object>> searchSectionList, String kwd, List<String> prevKwd, int totalCnt, int sectionSize, String returnType, String reqTime) {
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
		
		// 리턴 타입 JSON
		if(returnType.toUpperCase().indexOf("JSON") > -1) {
			Map<String, Object> itemMap = null;
			List<Map<String, Object>> sectionList = new ArrayList<Map<String,Object>>();
			
			if(searchSectionList != null) {
				for (Map<String, Object> section : searchSectionList) {
					
					itemMap = new LinkedHashMap<String, Object>();
					itemMap.put("indexName", (String) section.get("indexName"));
					itemMap.put("sectionCnt", String.valueOf(section.get("sectionCnt")));
					itemMap.put("sectionCode", (String) section.get("sectionCode"));
					itemMap.put("sectionName", (String) section.get("sectionName"));
					itemMap.put("docList", (List<Map<String, String>>) section.get("docList"));
					itemMap.put("aggregations", (List<Map<String, String>>) section.get("aggregations"));
					
					sectionList.add(itemMap);
				}
			}
			
			resultMap.put("totalCnt", totalCnt);
			resultMap.put("kwd", kwd);
			resultMap.put("prevKwd", prevKwd);
			resultMap.put("sectionSize", sectionSize);
			resultMap.put("sectionList", sectionList);
			
			this.apiResult = resultMap;
		} 
	}
	
	@Override
	public String toString() {
		return "ApiResultVO [apiResult=" + apiResult + "]";
	}

}
