-- =============================================================================
-- ③ 폴더경로(ECM 경로 탐색기) 트리 데이터 — MariaDB
--
-- 프런트의 src/data/folderTree.js 에 정적으로 박혀 있던 샘플 트리를 그대로 테이블로 옮긴 것이다.
-- 자기참조(adjacency list) 방식: PARENT_ID 가 NULL 이면 최상위(루트) 노드.
-- 같은 부모 아래 형제 노드들의 표시 순서는 SORT_ORDER 로 정한다(작을수록 위).
--
-- 실행: MariaDB의 isearch 데이터베이스(docker-compose.yml 의 SPRING_DATASOURCE_URL 과 동일)에
--       그대로 실행하면 된다. 예) mysql -h localhost -P 3306 -u isearch -p isearch < ecm_folder.sql
-- =============================================================================

CREATE TABLE IF NOT EXISTS TB_ECM_FOLDER (
    FOLDER_ID   VARCHAR(50)  NOT NULL COMMENT '폴더 고유 ID',
    PARENT_ID   VARCHAR(50)  NULL     COMMENT '상위 폴더 ID (NULL = 최상위 루트)',
    FOLDER_NAME VARCHAR(200) NOT NULL COMMENT '화면에 표시되는 폴더명',
    SORT_ORDER  INT          NOT NULL DEFAULT 0 COMMENT '같은 부모 아래 형제 노드 표시 순서(오름차순)',
    PRIMARY KEY (FOLDER_ID),
    CONSTRAINT FK_ECM_FOLDER_PARENT
        FOREIGN KEY (PARENT_ID) REFERENCES TB_ECM_FOLDER (FOLDER_ID)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='③ 폴더경로(ECM 경로 탐색기) 트리';

CREATE INDEX IDX_ECM_FOLDER_PARENT ON TB_ECM_FOLDER (PARENT_ID);

-- -----------------------------------------------------------------------------
-- 샘플 데이터 (folderTree.js 의 FOLDER_TREE 를 그대로 반영)
-- 부모보다 먼저 들어가면 FK 위반이 나므로, 반드시 루트 → 하위 순서로 INSERT 한다.
-- -----------------------------------------------------------------------------
-- [4차 리뷰 반영] "프로젝트" 아래 연도 폴더들이 전엔 'project' 바로 밑에 있었는데(우장각>프로젝트>2023년),
-- 이제 그 사이에 "표준폴더" 노드를 하나 끼워 넣는다(우장각>프로젝트>표준폴더>2023년) — LNB "표준폴더"
-- 파셋(검색 서버 쪽 pathLv4, SampleDataLoader.java 참고)이 내려주는 4단계 경로와 실제 ECM 트리 구조를
-- 일치시키기 위함이다. 기존에 'project' 를 PARENT_ID 로 쓰던 연도 노드(y2024/y2023/y2022)들을
-- 새 'standard-folder' 노드 밑으로 옮겼다.
INSERT INTO TB_ECM_FOLDER (FOLDER_ID, PARENT_ID, FOLDER_NAME, SORT_ORDER) VALUES
('woojanggak',       NULL,              '우장각',                    1),
('project',          'woojanggak',      '프로젝트',                  1),
('company',          'woojanggak',      '전사문서',                  2),
('dept',             'woojanggak',      '부서문서',                  3),
('standard-folder',  'project',         '표준폴더',                  1),
('y2024',            'standard-folder', '2024년',                    1),
('y2023',            'standard-folder', '2023년',                    2),
('y2022',            'standard-folder', '2022년',                    3),
('auto0069',      'y2023',       'AUTO-0069 부산광역시...',   1),
('perform-cm',    'y2023',       '01.수행(현장-CM)',          2),
('design-docs',   'y2023',       '02.설계도서',               3),
('contract-docs', 'y2023',       '03.계약문서',               4),
('safety-docs',   'y2023',       '04.안전관리',               5),
('quality-docs',  'y2023',       '05.품질관리',               6),
('cost-docs',     'y2023',       '06.기성내역',               7),
('defect-docs',   'y2023',       '07.하자보수',               8),
('etc-docs',      'y2023',       '08.기타문서',               9);
