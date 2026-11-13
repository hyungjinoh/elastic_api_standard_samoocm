package com.rayful.elastic.api.loader.ecm;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

public interface IEcmFolderDao {

    /** TB_ECM_FOLDER 전체 행을 평면(flat)으로 조회한다. 트리 조립은 EcmFolderService 에서 한다. */
    List<EcmFolderVO> selectAllFolders();
}
