package com.rayful.elastic.api.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SearchErrorCode {
	
	TIMEOUT_EXCPETION("1000", "408", "Search engine connection timeout")
	, SEARCH_EXCPETION("1000", "500", "An unknown error occurred during search")
	, DEFAULT_EXCEPTION("1000", "500", "An unknown error occurred");
	
	private String errorCode;
	private String errorCodeSub;
	private String errorMessage;
		
}
