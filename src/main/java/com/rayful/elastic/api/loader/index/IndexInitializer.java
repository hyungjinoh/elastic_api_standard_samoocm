package com.rayful.elastic.api.loader.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.rayful.elastic.api.loader.common.LoaderIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * 색인 생성/삭제. settings+mappings 는 resources/elastic/index-documents.json 사용.
 *
 * [탭별 개별 인덱스 분리] 이제 문서 인덱스가 4개(IndexNames.ALL_DOCUMENT_INDICES)라, 모든 메서드가
 * 그 4개 전부에 대해 동일한 매핑으로 반복 수행한다(필드 구조는 4개 인덱스가 전부 동일 — 물리적으로만 분리).
 */
@Component
public class IndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(IndexInitializer.class);

    private final ElasticsearchClient client;

    public IndexInitializer(ElasticsearchClient client) {
        this.client = client;
    }

    private boolean existsOne(String indexName) {
        try {
            return client.indices().exists(e -> e.index(indexName)).value();
        } catch (IOException e) {
            throw new LoaderIndexException("색인 존재 여부 확인 실패: " + indexName, e);
        }
    }

    /** 4개 문서 인덱스가 전부 존재하면 true. */
    public boolean exists() {
        return IndexNames.ALL_DOCUMENT_INDICES.stream().allMatch(this::existsOne);
    }

    /** 없는 인덱스만 생성. 이미 있는 건 건드리지 않는다. */
    public void createIfAbsent() {
        for (String indexName : IndexNames.ALL_DOCUMENT_INDICES) {
            if (existsOne(indexName)) {
                log.info("색인 이미 존재: {}", indexName);
            } else {
                createOne(indexName);
            }
        }
    }

    public void create() {
        for (String indexName : IndexNames.ALL_DOCUMENT_INDICES) {
            createOne(indexName);
        }
    }

    private void createOne(String indexName) {
        try (InputStream in = new ClassPathResource("elastic/index-documents.json").getInputStream()) {
            client.indices().create(c -> c.index(indexName).withJson(in));
            log.info("색인 생성 완료: {}", indexName);
        } catch (IOException e) {
            throw new LoaderIndexException("색인 생성 실패 (nori 플러그인 설치 여부 확인): " + indexName, e);
        }
    }

    public void delete() {
        for (String indexName : IndexNames.ALL_DOCUMENT_INDICES) {
            try {
                if (existsOne(indexName)) {
                    client.indices().delete(d -> d.index(indexName));
                    log.info("색인 삭제 완료: {}", indexName);
                }
            } catch (IOException e) {
                throw new LoaderIndexException("색인 삭제 실패: " + indexName, e);
            }
        }
    }

    /** 재생성(삭제 후 생성). 매핑 변경 시 사용. 4개 인덱스 전부 대상. */
    public void recreate() {
        delete();
        create();
    }
}
