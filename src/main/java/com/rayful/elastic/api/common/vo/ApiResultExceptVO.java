package com.rayful.elastic.api.common.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ApiResultExceptVO {
	
	private Object error;
	private String errorCode;
	
	@Builder
	public ApiResultExceptVO(String errorCode, String errorCodeSub, String errorMessage, List<String> errorMessageSubList
			, String url, String serviceType, String returnType, String xmlEncoding) {
		
		Map<String, Object> subErrorMap = null;
		List<Map<String, Object>> subErrorList = new ArrayList<Map<String,Object>>();
		
		if(errorMessageSubList != null) {
			boolean firstError = false;
			
			for (String errorMessageSub : errorMessageSubList) {
				
				subErrorMap = new HashMap<String, Object>();
				subErrorMap.put("name", serviceType);
				subErrorMap.put("code", errorMessageSub.substring(0, 5).replace("]", "").replace("[", ""));
				subErrorMap.put("message", errorMessageSub);
				subErrorList.add(subErrorMap);
				
				if(!firstError) {
					this.errorCode = (String) subErrorMap.get("code");
					firstError = true;
				}
			}
		
		} else {
			subErrorMap = new HashMap<String, Object>();
			subErrorMap.put("name", serviceType);
			subErrorMap.put("code", errorCodeSub);
			subErrorMap.put("message", errorMessage);
			subErrorList.add(subErrorMap);
			
			this.errorCode = (String) subErrorMap.get("code");
		}
		
		Map<String, Object> errorMap = new HashMap<String, Object>();
		errorMap.put("suberror", subErrorList);
		
		// JSON
		if(returnType.toUpperCase().indexOf("JSON") > -1) {
			this.error = errorMap;
		} 
	}

	public ApiResultExceptVO(int code, String msg, String returnType, String serviceType) {
		Map<String, Object> subErrorMap = null;
		List<Map<String, Object>> subErrorList = new ArrayList<Map<String,Object>>();
		
		subErrorMap = new HashMap<String, Object>();
		subErrorMap.put("name", serviceType);
		subErrorMap.put("code", code);
		subErrorMap.put("message", msg);
		subErrorList.add(subErrorMap);
		
		Map<String, Object> errorMap = new HashMap<String, Object>();
		errorMap.put("suberror", subErrorList);
		
		// JSON
		if(returnType.toUpperCase().indexOf("JSON") > -1) {
			this.error = errorMap;
			this.errorCode = String.valueOf(code);
		}
	}
}
