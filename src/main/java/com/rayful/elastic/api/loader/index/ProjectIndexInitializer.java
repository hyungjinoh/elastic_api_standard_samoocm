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
 * 프로젝트 마스터 색인(wjg-projects) 생성/삭제.
 * settings+mappings 는 resources/elastic/index-projects.json 사용.
 * (IndexInitializer 와 동일한 패턴 - 문서 색인과 별개 라이프사이클로 관리)
 */
@Component
public class ProjectIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(ProjectIndexInitializer.class);

    private final ElasticsearchClient client;

    public ProjectIndexInitializer(ElasticsearchClient client) {
        this.client = client;
    }

    public boolean exists() {
        try {
            return client.indices().exists(e -> e.index(IndexNames.PROJECTS)).value();
        } catch (IOException e) {
            throw new LoaderIndexException("프로젝트 색인 존재 여부 확인 실패", e);
        }
    }

    public void createIfAbsent() {
        if (exists()) {
            log.info("프로젝트 색인 이미 존재: {}", IndexNames.PROJECTS);
            return;
        }
        create();
    }

    public void create() {
        try (InputStream in = new ClassPathResource("elastic/index-projects.json").getInputStream()) {
            client.indices().create(c -> c.index(IndexNames.PROJECTS).withJson(in));
            log.info("프로젝트 색인 생성 완료: {}", IndexNames.PROJECTS);
        } catch (IOException e) {
            throw new LoaderIndexException("프로젝트 색인 생성 실패 (nori 플러그인 설치 여부 확인)", e);
        }
    }

    public void delete() {
        try {
            if (exists()) {
                client.indices().delete(d -> d.index(IndexNames.PROJECTS));
                log.info("프로젝트 색인 삭제 완료: {}", IndexNames.PROJECTS);
            }
        } catch (IOException e) {
            throw new LoaderIndexException("프로젝트 색인 삭제 실패", e);
        }
    }

    public void recreate() {
        delete();
        create();
    }
}
