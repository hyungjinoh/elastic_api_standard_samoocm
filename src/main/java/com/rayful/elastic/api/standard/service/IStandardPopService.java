package com.rayful.elastic.api.standard.service;

import java.io.File;

import com.rayful.elastic.api.standard.vo.RequestPopParamVO;

public interface IStandardPopService {
	
	/**
	 * 인기 검색어 파일 - single
	 * @param paramVO
	 * @param traceId
	 * @return
	 */
	File getPopularKeywordFile(RequestPopParamVO paramVO, String traceId);
	
}
