package com.rayful.elastic.api.loader.ecm;

import java.util.ArrayList;
import java.util.List;

/**
 * 중첩 트리로 조립된 폴더 노드. 프런트 src/data/folderTree.js 의 FOLDER_TREE 노드 모양
 * ({ id, name, children: [...] })과 필드명을 그대로 맞췄다 — 응답 JSON을 그 구조로 바로 써도 되게.
 */
public class EcmFolderNode {

    private String id;
    private String name;
    private List<EcmFolderNode> children = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<EcmFolderNode> getChildren() { return children; }
    public void setChildren(List<EcmFolderNode> children) { this.children = children; }
}
