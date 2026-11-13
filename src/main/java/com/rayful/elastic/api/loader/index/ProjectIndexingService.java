package com.rayful.elastic.api.loader.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.rayful.elastic.api.loader.common.LoaderIndexException;
import com.rayful.elastic.api.loader.domain.ProjectDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 프로젝트 마스터 벌크 색인 서비스 (IndexingService 와 동일한 패턴).
 * 실제 운영에서는 프로젝트 관리 시스템(RDBMS)의 프로젝트 마스터 테이블에서 읽어 색인한다.
 */
@Service
public class ProjectIndexingService {

    private static final Logger log = LoggerFactory.getLogger(ProjectIndexingService.class);

    private final ElasticsearchClient client;

    public ProjectIndexingService(ElasticsearchClient client) {
        this.client = client;
    }

    public int bulkIndex(List<ProjectDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            return 0;
        }
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (ProjectDocument doc : docs) {
            br.operations(op -> op.index(idx -> idx
                    .index(IndexNames.PROJECTS)
                    .id(doc.getProjectNo())
                    .document(doc)));
        }
        try {
            BulkResponse result = client.bulk(br.build());
            if (result.errors()) {
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        log.error("프로젝트 색인 실패 id={} reason={}", item.id(), item.error().reason());
                    }
                }
                throw new LoaderIndexException("일부 프로젝트 색인에 실패했습니다.");
            }
            log.info("프로젝트 색인 완료: {} 건", docs.size());
            return docs.size();
        } catch (IOException e) {
            throw new LoaderIndexException("프로젝트 벌크 색인 실패", e);
        }
    }
}
