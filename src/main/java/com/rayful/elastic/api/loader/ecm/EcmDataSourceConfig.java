package com.rayful.elastic.api.loader.ecm;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * ③ 폴더경로(ECM 경로 탐색기) 전용 DataSource 구성.
 *
 * 앱의 기본(1차) 데이터소스(spring.datasource.*)는 122.199.202.101 의 isearch 스키마를 본다.
 * TB_ECM_FOLDER 는 그와 완전히 다른 MariaDB 서버(samwoocm)에 있으므로, 같은 서버 안 스키마처럼
 * "DB명만 바꿔 쿼리"로는 해결이 안 되고 — 별도의 DataSource/SqlSessionFactory/SqlSessionTemplate
 * 한 벌을 통째로 더 만들어야 한다. 기존 1차 데이터소스(및 그걸 쓰는 ICommonDao 등 다른 모든 @Mapper)는
 * 이 클래스와 전혀 무관하게 지금까지 하던 대로 동작한다 — 영향 없음.
 *
 * application-local.yml 의 datasource.samwoocm.* 프로퍼티를 그대로 바인딩해서 쓴다.
 */
@Configuration
public class EcmDataSourceConfig {

    /** datasource.samwoocm.{url,username,password,driver-class-name} 바인딩. */
    @Bean
    @ConfigurationProperties(prefix = "datasource.samwoocm")
    public DataSourceProperties samwoocmDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource samwoocmDataSource(
            @Qualifier("samwoocmDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }

    @Bean
    public SqlSessionFactory samwoocmSqlSessionFactory(
            @Qualifier("samwoocmDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        // /mapper/**/*.xml (1차 SqlSessionFactory 가 읽는 경로)과 겹치지 않도록 별도 폴더에서 읽는다.
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:ecm-mapper/*.xml"));
        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate samwoocmSqlSessionTemplate(
            @Qualifier("samwoocmSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * IEcmFolderDao 를 samwoocmSqlSessionTemplate 에 묶어서 직접 Spring 빈으로 등록한다.
     * (IEcmFolderDao 에 @Mapper 를 안 붙인 이유가 바로 이거다 — 전역 @MapperScan 과 충돌하지 않도록.)
     */
    @Bean
    public MapperFactoryBean<IEcmFolderDao> ecmFolderDao(
            @Qualifier("samwoocmSqlSessionTemplate") SqlSessionTemplate sqlSessionTemplate) {
        MapperFactoryBean<IEcmFolderDao> factoryBean = new MapperFactoryBean<>(IEcmFolderDao.class);
        factoryBean.setSqlSessionTemplate(sqlSessionTemplate);
        return factoryBean;
    }
}
