package com.rayful.elastic.api.loader.autokwd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * TB_AUTO_KWD(자동완성 키워드) 일괄 적재.
 *
 * "우장각 자동완성" 원본 키워드(21,305건)를 500건 단위로 쪼갠 INSERT 구문 파일들
 * (batch_001.sql ~ batch_043.sql, 각 파일이 그대로 완결된 SQL 구문 1개)을 파일명(번호) 순서대로
 * 하나씩 읽어 isearch.TB_AUTO_KWD 에 순차 실행한다.
 *
 * - 파일은 기본적으로 클래스패스(resources/sql-batches/*.sql)에 번들되어 배포되므로 별도 경로
 *   지정 없이 바로 동작한다. 운영 서버의 다른 경로에 있는 파일을 쓰고 싶다면 loadFromDirectory()
 *   로 외부 디렉터리 경로를 넘기면 된다.
 * - 각 파일은 이미 완결된 INSERT 구문 1개이므로, HikariCP 기본값(autoCommit=true)에 따라
 *   파일 단위(=INSERT 구문 단위)로 자동 커밋된다. 한 파일에서 오류(중복 키 등)가 나도 그 파일만
 *   건너뛰고 나머지 파일은 계속 진행한다.
 */
@Service
public class AutoKwdLoaderService {

    private static final Logger log = LoggerFactory.getLogger(AutoKwdLoaderService.class);

    private static final String DEFAULT_CLASSPATH_PATTERN = "classpath:sql-batches/*.sql";
    private static final String TABLE = "isearch.TB_AUTO_KWD";

    private final JdbcTemplate jdbcTemplate;

    public AutoKwdLoaderService(@Qualifier("autoKwdJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 번들된 클래스패스 리소스(resources/sql-batches/*.sql)를 순서대로 적재한다.
     */
    public AutoKwdLoadResult loadFromClasspath() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(DEFAULT_CLASSPATH_PATTERN);

            List<NamedSql> items = new ArrayList<>();
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                String sql = resource.getContentAsString(StandardCharsets.UTF_8);
                items.add(new NamedSql(fileName, sql));
            }
            items.sort(Comparator.comparing(NamedSql::fileName));

            return execute(items);
        } catch (IOException e) {
            throw new IllegalStateException("sql-batches 클래스패스 리소스를 읽는 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 서버 파일시스템의 외부 디렉터리에서 *.sql 파일을 찾아 순서대로 적재한다.
     * (배포 이미지에 들어있는 번들 파일이 아니라, 운영자가 직접 서버에 올려둔 파일을 쓰고 싶을 때 사용)
     */
    public AutoKwdLoadResult loadFromDirectory(String directoryPath) {
        File folder = new File(directoryPath);
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("디렉터리를 찾을 수 없습니다: " + directoryPath);
        }

        File[] sqlFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".sql"));
        if (sqlFiles == null || sqlFiles.length == 0) {
            throw new IllegalArgumentException(".sql 파일이 없습니다: " + directoryPath);
        }

        List<NamedSql> items = new ArrayList<>();
        for (File file : sqlFiles) {
            try {
                String sql = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                items.add(new NamedSql(file.getName(), sql));
            } catch (IOException e) {
                throw new IllegalStateException("파일을 읽는 중 오류가 발생했습니다: " + file.getName(), e);
            }
        }
        items.sort(Comparator.comparing(NamedSql::fileName));

        return execute(items);
    }

    /**
     * 적재 확인용 — SITE_ID 기준 TB_AUTO_KWD 건수를 센다.
     */
    public long countBySiteId(String siteId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TABLE + " WHERE SITE_ID = ?",
                Long.class,
                siteId);
        return count == null ? 0L : count;
    }

    private AutoKwdLoadResult execute(List<NamedSql> items) {
        AutoKwdLoadResult result = new AutoKwdLoadResult();
        result.setTotalFiles(items.size());

        long totalStart = System.currentTimeMillis();

        for (NamedSql item : items) {
            long start = System.currentTimeMillis();
            try {
                int affected = jdbcTemplate.update(item.sql());
                long elapsed = System.currentTimeMillis() - start;
                result.addSuccess(item.fileName(), affected, elapsed);
                log.info("[AutoKwdLoader] 성공 - {} ({}건, {}ms)", item.fileName(), affected, elapsed);
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - start;
                result.addFailure(item.fileName(), elapsed, e.getMessage());
                log.error("[AutoKwdLoader] 실패 - {} : {}", item.fileName(), e.getMessage(), e);
                // 한 파일이 실패해도 나머지 파일은 계속 진행한다.
            }
        }

        result.setElapsedMs(System.currentTimeMillis() - totalStart);
        log.info("[AutoKwdLoader] 완료 - 총 {}개 파일, 성공 {}건, 실패 {}건, 입력 {}행, {}ms",
                result.getTotalFiles(), result.getSuccessCount(), result.getFailCount(),
                result.getAffectedRows(), result.getElapsedMs());

        return result;
    }

    private record NamedSql(String fileName, String sql) {
    }
}
