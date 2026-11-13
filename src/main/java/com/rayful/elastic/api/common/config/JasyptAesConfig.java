package com.rayful.elastic.api.common.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

/**
 * application.yml 의 ENC(...) 로 감싼 값(예: DB 접속정보)을 기동 시점에 복호화하기 위한 Jasypt 설정.
 *
 * [시큐어코딩 조치 이력]
 * - (조치 전) 알고리즘이 PBEWithMD5AndDES 였음
 *     · DES(56bit) + MD5 조합으로, 행안부 SW개발보안 가이드 "취약한 암호화 알고리즘 사용"(CWE-327)에 해당.
 * - (조치 전) 암호화 키가 소스에 상수로 하드코딩("@standardSecurityKey") 되어 있었음
 *     · "하드코딩된 암호화 키"(CWE-321). 소스 유출 시 DB 자격증명이 즉시 복호화 가능.
 *
 * - (조치 후) 알고리즘을 PBEWITHHMACSHA512ANDAES_256(AES-256 기반) 으로 상향.
 * - (조치 후) 키는 환경변수 JASYPT_ENCRYPTOR_PASSWORD 로만 주입하며, 없거나 비어있으면 기동을 실패시킨다.
 *     · SecurityModule(AES-256, SECURITY_AES_KEY 환경변수)과 동일한 "키는 환경변수로만" 원칙을 적용.
 * - salt/IV 는 매 암호화마다 SecureRandom(RandomSaltGenerator/RandomIvGenerator)으로 생성.
 *
 * ※ [운영 필수 조치] 알고리즘과 키가 모두 바뀌었으므로, 기존 application-*.yml 의 ENC(...) 값은
 *   그대로 두면 복호화에 실패한다. 아래 절차로 새 키/알고리즘으로 재암호화하여 교체할 것.
 *
 *   1) 배포 서버(또는 개발 PC)에 환경변수 설정
 *        export JASYPT_ENCRYPTOR_PASSWORD='운영에서 관리하는 강력한 비밀값'
 *   2) 평문을 재암호화 (이 클래스의 main 사용)
 *        java -cp app.jar com.rayful.elastic.api.common.config.JasyptAesConfig '평문DB비밀번호'
 *      → 출력된 문자열을 application-*.yml 에 ENC(출력값) 형태로 교체
 *   3) 애플리케이션 기동 시 동일한 JASYPT_ENCRYPTOR_PASSWORD 환경변수가 설정되어 있어야 함.
 */
@Configuration
@EnableEncryptableProperties
public class JasyptAesConfig {

	/** 강한 PBE 알고리즘 (AES-256 기반). 기존 PBEWithMD5AndDES(취약) 대체. */
	public static final String ALGORITHM_KEY = "PBEWITHHMACSHA512ANDAES_256";

	/** 암호화 키를 담는 환경변수명. 소스/설정파일에 평문 저장 금지. */
	public static final String KEY_ENV_NAME = "JASYPT_ENCRYPTOR_PASSWORD";

	/**
	 * 환경변수에서 암호화 키를 읽는다. 없거나 공백이면 기동 자체를 실패시킨다(fail-fast).
	 */
	private static String loadPassword() {
		String password = System.getenv(KEY_ENV_NAME);
		if (password == null || password.trim().isEmpty()) {
			throw new IllegalStateException(
					KEY_ENV_NAME + " 환경변수가 설정되지 않았습니다. "
					+ "ENC(...) 복호화에 사용할 Jasypt 암호화 키는 소스/설정파일에 하드코딩하지 않고 "
					+ "환경변수로만 주입해야 합니다.");
		}
		return password;
	}

	/**
	 * 공통 강화 암호화기 생성 (런타임 빈 / 복호화 헬퍼 / 재암호화 도구가 동일 설정을 공유).
	 */
	private static PooledPBEStringEncryptor buildEncryptor() {
		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
		SimpleStringPBEConfig config = new SimpleStringPBEConfig();

		config.setPassword(loadPassword());                                  // 키: 환경변수
		config.setAlgorithm(ALGORITHM_KEY);                                  // AES-256 기반
		config.setKeyObtentionIterations("1000");                            // 해싱 반복 횟수
		config.setPoolSize("1");                                             // 인스턴스 Pool
		config.setProviderName("SunJCE");
		config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator"); // salt 랜덤
		config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");       // IV 랜덤
		config.setStringOutputType("base64");
		encryptor.setConfig(config);

		return encryptor;
	}

	// application.yml 에 지정된 ENC(...) 값을 복호화하는 런타임 빈
	@Bean("jasyptEncryptorAES")
	public StringEncryptor stringEncryptor() {
		return buildEncryptor();
	}

	// 문자열 복호화 헬퍼 (기존 시그니처 유지)
	public String strDecryptor(String str) {
		return buildEncryptor().decrypt(str);
	}

	// 문자열 암호화 테스트/재암호화 도구
	public static String getEncryptedTest(String enStr) {
		return buildEncryptor().encrypt(enStr);
	}

	/**
	 * 평문을 ENC(...) 에 넣을 암호문으로 변환하는 CLI 도구.
	 * 사용법: JASYPT_ENCRYPTOR_PASSWORD 환경변수 설정 후
	 *        java -cp app.jar com.rayful.elastic.api.common.config.JasyptAesConfig '평문'
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("사용법: JasyptAesConfig '<암호화할 평문>'  (환경변수 " + KEY_ENV_NAME + " 필요)");
			return;
		}
		System.out.println(getEncryptedTest(args[0]));
	}
}
