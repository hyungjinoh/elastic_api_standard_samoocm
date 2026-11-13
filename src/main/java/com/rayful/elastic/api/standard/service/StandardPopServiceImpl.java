package com.rayful.elastic.api.standard.service;

import java.io.File;

//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

//import com.rayful.elastic.api.common.config.SearchConfig;

import com.rayful.elastic.api.standard.vo.RequestPopParamVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Primary
public class StandardPopServiceImpl implements IStandardPopService {
	
	//@Autowired
	//private SearchConfig searchConfig;
	
	/**
	 * 인기 검색어 파일 - single
	 */
	public File getPopularKeywordFile(RequestPopParamVO paramVO, String traceId) {
		String filePath = "";
		//String filePath = searchConfig.getPopularPath().get(paramVO.getCycle());
		//String fileName = paramVO.getCycle().toLowerCase() + "_" + paramVO.getDate() + ".kwd";
		String fileName = "";
		
		if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
			throw new IllegalArgumentException("Invalid filename");
		}
		
		File file = new File(filePath, fileName);
		
		if(!file.exists()) {
			log.info("[{}] 인기검색어 파일이 존재하지 않습니다. ({})", traceId, file.getAbsolutePath());
			return null;
		}
		
		return file;
	}
	
}
