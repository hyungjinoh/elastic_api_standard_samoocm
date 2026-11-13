package com.rayful.elastic.api.common.exception;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class SearchException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	private final int status;
	private final String errorCode;
	private final String errorCodeSub;
	private final String errorMessage;
	private final List<String> errorMessageSubList;
	
	public SearchException(int staus, String errorCode, String errorCodeSub, String errorMessage, List<String> errorMessageSubList) {
		super(errorMessage);
		this.status = staus;
		this.errorCode = errorCode + errorCodeSub;
		this.errorCodeSub = errorCodeSub;
		this.errorMessage = errorMessage;
		this.errorMessageSubList = errorMessageSubList;
	}
	
	public SearchException(int staus, String errorCode, String errorCodeSub, String errorMessage, String errorMessageSub) {
		super(errorMessage);
		List<String> errorMsgList = new ArrayList<String>();
		errorMsgList.add(errorMessageSub);
		
		this.status = staus;
		this.errorCode = errorCode + errorCodeSub;
		this.errorCodeSub = errorCodeSub;
		this.errorMessage = errorMessage;
		this.errorMessageSubList = errorMsgList;
	}
	
}
