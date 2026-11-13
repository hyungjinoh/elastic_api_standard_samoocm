package com.rayful.elastic.api.loader.autokwd;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * TB_AUTO_KWD(자동완성 키워드) 일괄 적재 전용 DataSource/JdbcTemplate.
 *
 * EcmDataSourceConfig 가 samwoocm(③ ECM 폴더) 전용 DataSource 를 별도로 두는 것과 같은 이유로,
 * 이 기능도 전용 빈을 따로 둔다: 이 프로젝트는 EcmDataSourceConfig 에서 "datasource.samwoocm.*" 라는
 * 또 다른 DataSource 빈을 이미 직접 등록해 두고 있어서, Spring Boot가 기본(spring.datasource.*)
 * DataSource/SqlSessionFactory를 "그 중 어떤 빈을 자동 주입해야 하는지" 애매해질 여지가 있다.
 * TB_AUTO_KWD 는 반드시 1차 데이터소스(spring.datasource.*, isearch 스키마)로만 적재되어야 하므로,
 * 다른 @Mapper/SqlSessionFactory 설정과 전혀 무관하게 spring.datasource.* 프로퍼티를 그대로 다시 바인딩해
 * 이 기능 전용 DataSource/JdbcTemplate 한 벌을 명시적으로 만든다. 기존 빈들과는 이름이 달라 서로 영향 없음.
 */
@Configuration
public class AutoKwdDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties autoKwdDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource autoKwdDataSource(
            @Qualifier("autoKwdDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate autoKwdJdbcTemplate(
            @Qualifier("autoKwdDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
