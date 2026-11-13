package com.rayful.elastic.api.common.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseVO {
	private Object result;
	private ApiResultExceptVO errorInfo;

	public ResponseVO(Object result) {
		this.result = result;
	}
	
	public ResponseVO(ApiResultExceptVO errorInfo) {
		this.errorInfo = errorInfo;
	}
	
}
