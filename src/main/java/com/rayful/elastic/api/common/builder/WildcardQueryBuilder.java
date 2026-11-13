package com.rayful.elastic.api.common.builder;

import java.util.HashMap;
import java.util.Map;

/**
 * [신규] contains(부분일치) 연산자 전용. TermQueryBuilder/FullTextQueryBuilder와 달리,
 * 형태소 분석(nori)을 타는 match 쿼리가 아니라 분석 없이 원문 그대로 저장된 keyword
 * 서브필드에 와일드카드("*값*")로 매칭한다 — "우장각"처럼 사전에 없는 고유명사가 품사
 * 분석에서 통째로 걸러져 토큰이 0개가 되고, 그 결과 match 쿼리가 아무것도 매칭하지
 * 못하던(match_none) 문제를 구조적으로 피하기 위함이다.
 */
public class WildcardQueryBuilder implements IQueryBuilder {

    private final Map<String, Object> wildcardMap = new HashMap<>();

    /**
     * @param field keyword 서브필드 이름까지 포함해서 넘긴다(예: "path.keyword").
     *              호출 쪽에서 ".keyword" 를 붙여서 넘기는 걸 원칙으로 한다 —
     *              이 빌더가 임의로 추측해서 붙이면 keyword 서브필드명이 다른 매핑에서
     *              깨질 수 있어 명시적으로 받는다.
     * @param value 부분일치할 원문 값. 앞뒤로 "*"를 붙여 포함(contains)으로 만든다.
     */
    public WildcardQueryBuilder wildcardContains(String field, String value) {
        Map<String, Object> settingMap = new HashMap<>();
        settingMap.put("value", "*" + escape(value) + "*");
        settingMap.put("case_insensitive", true);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put(field, settingMap);

        this.wildcardMap.put("wildcard", fieldMap);
        return this;
    }

    // 와일드카드 쿼리의 메타문자(*, ?, \)가 값 안에 실제로 들어있는 경우를 이스케이프.
    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("*", "\\*").replace("?", "\\?");
    }

    @Override
    public Map<String, Object> build() {
        return this.wildcardMap;
    }
}
