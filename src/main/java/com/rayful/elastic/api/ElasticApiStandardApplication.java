package com.rayful.elastic.api;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
// [버그 수정] annotationClass 없이 basePackages 만 주면, @MapperScan 은 그 패키지 밑의 "모든 인터페이스"를
// 매퍼로 등록해버린다(= @Mapper 유무와 무관) — @Mapper 를 뺀 IEcmFolderDao(EcmDataSourceConfig 가
// samwoocm 전용 SqlSessionFactory 에 수동으로 묶어 등록하는 바로 그 인터페이스)까지 여기서 또 한 번
// 자동 스캔돼서 1차(isearch) SqlSessionFactory 에 중복 등록되고, 그 결과 같은 타입의 빈이 2개가 되어
// "required a single bean, but 2 were found" 충돌이 났다. annotationClass = Mapper.class 를 줘서
// @Mapper 가 실제로 붙은 인터페이스(ICommonDao 등)만 스캔하도록 좁혔다.
@MapperScan(basePackages = "com.rayful.elastic.api", annotationClass = Mapper.class)
public class ElasticApiStandardApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElasticApiStandardApplication.class, args);
	}

}