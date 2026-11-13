# 오픈소스 고지 (THIRD-PARTY NOTICES)

본 소프트웨어(`elastic_api_standard`)는 아래 오픈소스 소프트웨어를 포함/사용합니다.
각 구성요소는 해당 라이선스 조건에 따라 배포됩니다. 라이선스 전문은 각 프로젝트의
배포물 또는 공식 저장소에서 확인할 수 있습니다.

> 버전 표기 중 "(관리됨)"은 Spring Boot 3.5.16 BOM이 관리하는 버전을 의미하며,
> 최종 배포 전 `mvn dependency:tree` 결과로 실제 버전을 확정하여 갱신하십시오.

## Apache License 2.0
- Spring Boot 4.1.0 / Spring Framework 7.0.8 구성요소 (spring-boot-starter-web, -validation, -webflux, -tomcat 등)
- MyBatis Spring Boot Starter 4.0.1 (org.mybatis.spring.boot)
- springdoc-openapi 3.0.3 및 내장 swagger-ui / swagger-core
- Jackson 3 (tools.jackson.core:jackson-databind/jackson-core, tools.jackson.dataformat:jackson-dataformat-xml) 및 Woodstox
  (단, jackson-annotations는 com.fasterxml.jackson.core 유지 — Apache-2.0)
- Netty, Project Reactor (webflux 경유)
- Hibernate Validator
- Apache Commons Codec 1.21.0
- SnakeYAML
- Google Gson
- jasypt-spring-boot-starter 4.0.4 및 jasypt 코어

  라이선스 전문: https://www.apache.org/licenses/LICENSE-2.0

## MIT License
- SLF4J (slf4j-api, jcl-over-slf4j)
- Project Lombok (컴파일 전용, 배포물 미포함)

## Eclipse Public License 1.0 (선택) — 이중 라이선스 항목
- **Logback** (ch.qos.logback:logback-classic, logback-core)
  - Logback은 EPL-1.0 또는 LGPL-2.1 이중 라이선스로 배포됩니다.
  - **본 배포물은 EPL-1.0 조건을 선택하여 사용합니다.**
  - 라이선스 전문: https://www.eclipse.org/legal/epl-v10.html

## GNU Lesser General Public License, version 2.1 (LGPL-2.1-or-later)
- **MariaDB Connector/J** (org.mariadb.jdbc:mariadb-java-client)
  - JDBC 드라이버로서 **동적 링크**되며, `WEB-INF/lib` 내 **독립 JAR** 형태로 포함되어
    사용자가 라이브러리를 **교체**할 수 있습니다. 라이브러리 소스는 **수정되지 않았습니다.**
  - 라이선스 전문: https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
  - 소스/저장소: https://github.com/mariadb-corporation/mariadb-connector-j

---

### LGPL 준수 안내
LGPL-2.1 대상 구성요소(MariaDB Connector/J, 그리고 LGPL 선택 시 Logback)는
소스코드를 수정하지 않고 동적 링크로만 사용되며, 별도 JAR로 포함되어 교체가 가능합니다.
해당 라이브러리 소스코드 사본이 필요한 경우 위 공식 저장소 링크에서 입수할 수 있습니다.

*이 파일은 검토 시점 기준 초안입니다. 최종 배포 전 실제 의존성 트리(`mvn dependency:tree`)와
`license-maven-plugin:download-licenses` 산출물로 목록·버전을 확정하십시오.*


<<<<<<<<<<<<<<<참고>>>>>>>>>>>>>>>>>>>>>>>>>
첨부된 고지 문서에서 `##`로 시작하는 각 섹션 타이틀은 프로젝트에 포함된 오픈소스 라이브러리들이 따르고 있는 **라이선스의 명칭과 적용 방식**을 나타냅니다.

* **## Apache License 2.0**: 아파치 소프트웨어 재단에서 만든 대표적인 허용적(permissive) 오픈소스 라이선스입니다. 
 상용 목적 사용, 수정, 배포가 자유롭지만, 라이선스 사본 포함, 저작권 및 면책 고지, 수정 사항 명시, 특허권 보호 조항 준수 등의 조건이 적용됩니다.


* **## MIT License**: 매우 간결하고 제약이 적은 대표적인 허용적 라이선스입니다. 
  상용 이용, 수정, 재배포 등 거의 모든 행위가 자유로우며, 원작자의 저작권 표시와 면책 조항만 유지하면 됩니다.


* **## Eclipse Public License 1.0 (선택) — 이중 라이선스 항목**: 이클립스 재단 기반의 약한 카피레프트(Weak Copyleft) 라이선스입니다. 
  원본 프로젝트(Logback)가 EPL-1.0과 LGPL-2.1 두 가지 라이선스를 모두 허용하는 '이중 라이선스(Dual License)' 형태인데,
  본 프로젝트 배포 시 의무 조항 준수 전략에 따라 **EPL-1.0 조건을 적용하기로 선택**했음을 명시한 제목입니다.


* **## GNU Lesser General Public License, version 2.1 (LGPL-2.1-or-later)**: 강력한 카피레프트인 GPL에 비해 
   라이브러리 연동 제약을 완화한 라이선스입니다. 라이브러리 자체를 수정한 경우에는 해당 수정 소스코드를 공개해야 하지만,
   수정 없이 독립된 JAR 형태로 동적 링크하여 사용자가 라이브러리를 교체할 수 있게 구성하면 자체 작성한 비즈니스 소스코드는 공개할 의무가 없습니다.
