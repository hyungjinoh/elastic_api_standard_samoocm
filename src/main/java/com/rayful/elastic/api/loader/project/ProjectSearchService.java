package com.rayful.elastic.api.loader.project;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.rayful.elastic.api.loader.common.LoaderIndexException;
import com.rayful.elastic.api.loader.common.SecurityUtils;
import com.rayful.elastic.api.loader.domain.ProjectDocument;
import com.rayful.elastic.api.loader.index.IndexNames;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * [wjg-loader 병합] 프로젝트 검색 팝업 전용 서비스 (wjg-backend 원본 그대로, 패키지 경로만 이동).
 * 문서 검색(표준 플로우)과 별개로 wjg-projects(프로젝트 마스터)를 공식 ES 클라이언트로 직접 조회한다.
 * elastic_api_standard 의 collapseCd/필터 체계에는 없는 "프로젝트 엔티티 자체" 를 찾는 단순 목록/페이징이라
 * 표준 검색 플로우에 끼워넣지 않고, 로더와 같은 공식 클라이언트를 재사용하는 별도 엔드포인트로 둔다.
 */
@Service
public class ProjectSearchService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ElasticsearchClient client;

    public ProjectSearchService(ElasticsearchClient client) {
        this.client = client;
    }

    public ProjectSearchResponse search(ProjectSearchRequest req) {
        int size = SecurityUtils.clampSize(req.sizeOrDefault(), MAX_PAGE_SIZE);
        int page = SecurityUtils.clampPage(req.pageOrDefault());
        int from = page * size;

        boolean hasKeyword = StringUtils.hasText(req.keyword());
        Query query = hasKeyword ? keywordQuery(req.keyword().trim()) : Query.of(q -> q.matchAll(m -> m));

        List<SortOptions> sorts = new ArrayList<>();
        if (!hasKeyword) {
            sorts.add(SortOptions.of(so -> so.field(f -> f.field("projectName.keyword").order(SortOrder.Asc))));
        } else {
            sorts.add(SortOptions.of(so -> so.score(sc -> sc.order(SortOrder.Desc))));
        }
        sorts.add(SortOptions.of(so -> so.field(f -> f.field("projectNo").order(SortOrder.Asc))));

        try {
            var esResponse = client.search(s -> s
                            .index(IndexNames.PROJECTS)
                            .from(from)
                            .size(size)
                            .query(query)
                            .sort(sorts)
                            .trackTotalHits(t -> t.enabled(true)),
                    ProjectDocument.class);

            List<ProjectSearchResponse.ProjectItem> items = new ArrayList<>();
            for (Hit<ProjectDocument> hit : esResponse.hits().hits()) {
                ProjectDocument d = hit.source();
                if (d == null) continue;
                items.add(new ProjectSearchResponse.ProjectItem(
                        d.getProjectNo(), d.getProjectName(), d.getClientName(),
                        d.getProjectStatus(), d.getRegion(), d.getConstructionType(), d.getProjectYear()));
            }
            long total = esResponse.hits().total() != null ? esResponse.hits().total().value() : items.size();
            return new ProjectSearchResponse(total, page, size, items);
        } catch (IOException e) {
            throw new LoaderIndexException("프로젝트 검색 실패", e);
        }
    }

    private Query keywordQuery(String keyword) {
        String kw = keyword.toLowerCase();
        return Query.of(q -> q.bool(b -> b
                .should(sq -> sq.term(t -> t.field("projectNo").value(kw).boost(5.0f)))
                .should(sq -> sq.matchBoolPrefix(mp -> mp.field("projectName").query(keyword).boost(2.0f)))
                .should(sq -> sq.prefix(p -> p.field("projectNo").value(kw)))
                .minimumShouldMatch("1")));
    }
}
