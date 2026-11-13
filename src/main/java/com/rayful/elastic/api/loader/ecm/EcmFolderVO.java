package com.rayful.elastic.api.loader.ecm;

/**
 * TB_ECM_FOLDER 1행을 그대로 담는 VO(평면 데이터). MyBatis resultMap 과 1:1 매핑된다.
 */
public class EcmFolderVO {

    private String folderId;
    private String parentId;   // 최상위 루트는 null
    private String folderName;
    private Integer sortOrder;

    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
