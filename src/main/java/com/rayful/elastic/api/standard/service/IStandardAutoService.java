package com.rayful.elastic.api.standard.service;

import com.rayful.elastic.api.common.vo.SearchResultVO;
import com.rayful.elastic.api.standard.vo.RequestAutoParamVO;

public interface IStandardAutoService {
	
	/**
	 * 표준 자동완성
	 * @param paramVO
	 * @param traceId
	 * @return
	 */
	SearchResultVO getAutoList(RequestAutoParamVO paramVO, String traceId);
	
}
