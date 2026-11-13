package com.rayful.elastic.api.loader.ecm;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ③ 폴더경로(ECM 경로 탐색기) — TB_ECM_FOLDER(MariaDB)를 평면으로 읽어 PARENT_ID 기준
 * 중첩 트리로 조립한다. 예전에는 프런트 src/data/folderTree.js 에 똑같은 모양의 정적 데이터가
 * 박혀 있었는데, 이제는 DB가 단일 소스(source of truth)가 되고 프런트는 /api/ecm/folders 를
 * 호출해서 매번 최신 트리를 받아오는 구조로 바뀐다.
 */
@Service
public class EcmFolderService {

    private final IEcmFolderDao dao;

    public EcmFolderService(IEcmFolderDao dao) {
        this.dao = dao;
    }

    public List<EcmFolderNode> getFolderTree() {
        List<EcmFolderVO> rows = dao.selectAllFolders();

        // 1) id -> 노드 매핑을 먼저 전부 만들어 둔다(부모가 먼저 나온다는 보장 없이도 안전하게 조립하기 위해).
        Map<String, EcmFolderNode> nodeById = new LinkedHashMap<>();
        for (EcmFolderVO row : rows) {
            EcmFolderNode node = new EcmFolderNode();
            node.setId(row.getFolderId());
            node.setName(row.getFolderName());
            nodeById.put(row.getFolderId(), node);
        }

        // 2) parentId -> 그 아래 자식 VO 목록. 루트는 key 가 null.
        Map<String, List<EcmFolderVO>> byParent = new HashMap<>();
        for (EcmFolderVO row : rows) {
            byParent.computeIfAbsent(row.getParentId(), k -> new ArrayList<>()).add(row);
        }
        // 형제 노드는 SORT_ORDER 오름차순으로 정렬(값이 없으면 0 취급)
        byParent.values().forEach(list -> list.sort(
                Comparator.comparingInt(v -> v.getSortOrder() == null ? 0 : v.getSortOrder())));

        // 3) 각 노드에 자식을 붙인다.
        for (EcmFolderVO row : rows) {
            List<EcmFolderVO> children = byParent.get(row.getFolderId());
            if (children == null || children.isEmpty()) continue;
            EcmFolderNode parentNode = nodeById.get(row.getFolderId());
            for (EcmFolderVO child : children) {
                parentNode.getChildren().add(nodeById.get(child.getFolderId()));
            }
        }

        // 4) PARENT_ID 가 null 인 행들이 최상위 루트 — 그 순서대로 반환(프런트의 FOLDER_TREE 배열과 동일한 모양).
        List<EcmFolderVO> roots = byParent.getOrDefault(null, Collections.emptyList());
        return roots.stream()
                .map(r -> nodeById.get(r.getFolderId()))
                .collect(Collectors.toList());
    }
}
