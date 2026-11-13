package com.rayful.elastic.api.common.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ICommonDao {
	void insertSearchLog(Map<String, Object> paramMap);
	
	List<Map<String,Object>> selectPopkeywordList(Map<String, Object> paramMap);
}
