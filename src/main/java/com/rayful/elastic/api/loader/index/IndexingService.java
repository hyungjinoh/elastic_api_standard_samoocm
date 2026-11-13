package com.rayful.elastic.api.loader.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.rayful.elastic.api.loader.common.LoaderIndexException;
import com.rayful.elastic.api.loader.domain.SearchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 문서 벌크 색인 서비스.
 * 실제 운영에서는 RDBMS/IBM Notes/SFTP/문서파일 소스에서 읽어 이 서비스로 색인한다.
 * (elastic_bulker / elastic_batch 의 색인 파이프라인 대체 지점)
 */
@Service
public class IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private final ElasticsearchClient client;

    public IndexingService(ElasticsearchClient client) {
        this.client = client;
    }

    public int bulkIndex(List<SearchDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            return 0;
        }
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (SearchDocument doc : docs) {
            // [탭별 개별 인덱스 분리] area 값(PROJECT/KNOWLEDGE/COMPANY/DEPT)에 맞는 실제 인덱스로 라우팅.
            String targetIndex = IndexNames.documentsIndexFor(doc.getArea());
            // 예: idx_wjg_documents1 에 적재되는 문서는 collapsedCd=90001 을 함께 저장.
            doc.setCollapsedCd(IndexNames.collapseCdFor(doc.getArea()));
            br.operations(op -> op.index(idx -> idx
                    .index(targetIndex)
                    .id(doc.getId())
                    .document(doc)));
        }
        try {
            BulkResponse result = client.bulk(br.build());
            if (result.errors()) {
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        log.error("색인 실패 id={} reason={}", item.id(), item.error().reason());
                    }
                }
                throw new LoaderIndexException("일부 문서 색인에 실패했습니다.");
            }
            log.info("색인 완료: {} 건", docs.size());
            return docs.size();
        } catch (IOException e) {
            throw new LoaderIndexException("벌크 색인 실패", e);
        }
    }
}
