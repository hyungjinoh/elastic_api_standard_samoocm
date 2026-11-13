package com.rayful.elastic.api.common.vo;

import java.util.Map;

import lombok.Data;

@Data
public class SearchResultVO {
	
	private String keyword;
	private String collapseCd;
	private int took;
	private int totalCnt;
	private Map<String, Object> result;
	
	@Override
	public String toString() {
		return "collapseCd=" + collapseCd
				+ ", keyword=" + keyword + ", took=" + took + ", totalCnt=" + totalCnt + ", result=" + result;
	}
}
