package com.rayful.elastic.api.tools;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

/**
 * [개발 유틸] jasypt ENC() 값 생성/검증 도구.
 *
 * mybatis-db-pool.xml 의 encryptorConfig 와 "완전히 동일한" 파라미터로 암호화하므로,
 * 여기서 만든 ENC(...) 값은 애플리케이션 런타임에서 그대로 복호화된다.
 * 마스터 키는 하드코딩하지 않고 환경변수 JASYPT_ENCRYPTOR_PASSWORD 에서 읽는다.
 *
 * [IntelliJ 실행]
 *   1) 이 클래스 우클릭 → Run 'JasyptEncryptTool.main()'
 *   2) 생성된 Run Configuration 편집:
 *      - Environment variables : JASYPT_ENCRYPTOR_PASSWORD=<마스터키>
 *      - Program arguments     : 암호화할 평문들을 공백으로 구분해 나열
 *        예) mydbpassword1 mysftppass "따옴표 포함 값"
 *   3) Run → 콘솔에 각 평문의 ENC(...) 출력 → config-*.yml 에 붙여넣기
 *
 * ※ 이 클래스는 src/test 에 있어 운영 JAR(배포본)에는 포함되지 않는다.
 */
public class JasyptEncryptTool {

    private static final String ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";
    private static final String KEY_ITERATIONS = "1000";
    private static final String SALT_GEN = "org.jasypt.salt.RandomSaltGenerator";
    private static final String IV_GEN = "org.jasypt.iv.RandomIvGenerator";
    private static final String OUTPUT_TYPE = "base64";

    public static void main(String[] args) {
        String password = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        if (password == null || password.isBlank()) {
            System.err.println("[오류] 환경변수 JASYPT_ENCRYPTOR_PASSWORD 를 설정하세요. (Run Config > Environment variables)");
            return;
        }
        if (args == null || args.length == 0) {
            System.err.println("[사용법] Program arguments 에 암호화할 평문을 공백으로 구분해 입력하세요.");
            System.err.println("         예) mydbpassword mysftppass");
            return;
        }

        StandardPBEStringEncryptor encryptor = buildEncryptor(password);

        System.out.println("===== jasypt 암호화 결과 (config-*.yml 에 붙여넣기) =====");
        for (String plain : args) {
            String encrypted = encryptor.encrypt(plain);
            // 복호화 검증(런타임과 동일 설정이므로 반드시 원문 복원되어야 함)
            boolean ok = plain.equals(encryptor.decrypt(encrypted));
            System.out.println("ENC(" + encrypted + ")   " + (ok ? "[검증 OK]" : "[검증 실패!]"));
        }
        System.out.println("=========================================================");
    }

    private static StandardPBEStringEncryptor buildEncryptor(String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm(ALGORITHM);
        config.setKeyObtentionIterations(KEY_ITERATIONS);
        config.setSaltGeneratorClassName(SALT_GEN);
        config.setIvGeneratorClassName(IV_GEN);
        config.setStringOutputType(OUTPUT_TYPE);
        encryptor.setConfig(config);
        return encryptor;
    }
}
