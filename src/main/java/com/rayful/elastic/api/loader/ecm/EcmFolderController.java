package com.rayful.elastic.api.loader.ecm;

import com.rayful.elastic.api.loader.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ③ 폴더경로(ECM 경로 탐색기) 트리 조회 API.
 * 프런트의 FolderTreeInline.jsx 가 기존에 쓰던 정적 데이터(src/data/folderTree.js)를 대체해서
 * 호출하는 엔드포인트 — 응답 모양을 그 정적 데이터의 FOLDER_TREE 와 완전히 동일하게 맞췄다
 * ([{ id, name, children: [...] }, ...]).
 */
@RestController
@RequestMapping("/api/ecm")
public class EcmFolderController {

    private final EcmFolderService ecmFolderService;

    public EcmFolderController(EcmFolderService ecmFolderService) {
        this.ecmFolderService = ecmFolderService;
    }

    @GetMapping("/folders")
    public ApiResponse<List<EcmFolderNode>> folders() {
        return ApiResponse.ok(ecmFolderService.getFolderTree());
    }
}
