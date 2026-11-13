package com.rayful.elastic.api.common.util;

import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * AES-256/CBC 암复화 유틸
 *
 * [보안 수정 이력]
 * - (수정 전) 암호키가 소스코드에 평문으로 하드코딩되어 있었음 (SECURITY_KEY 상수)
 * - (수정 전) IV(초기화벡터)가 항상 0으로 고정되어 있어, 같은 평문은 항상 같은 암호문으로 나옴
 *             (CBC 모드에서 IV 재사용은 패턴 분석에 취약해지는 심각한 보안 결함)
 *
 * [수정 후]
 * - 암호키는 환경변수(SECURITY_AES_KEY)로만 주입, 소스/설정파일에 평문 저장하지 않음
 * - IV는 암호화할 때마다 SecureRandom으로 새로 생성
 * - IV는 비밀정보가 아니므로, 암호문 앞에 그대로 붙여서 함께 저장/전달하고
 *   복호화 시 그 앞부분에서 다시 꺼내 씀 (업계 표준 방식)
 */
public class SecurityModule {

	private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
	private static final int IV_LENGTH_BYTES = 16;

	private static final byte[] SECRET_KEY_BYTES = loadSecretKey();

	private SecurityModule() {
	}

	/**
	 * 환경변수(SECURITY_AES_KEY)에서 암호키를 읽어온다.
	 * - 값이 없거나 길이가 AES 규격(16/24/32바이트)에 안 맞으면, 애플리케이션 기동 자체를 실패시킨다.
	 *   (예전처럼 "키가 없으면 하드코딩된 기본값으로 조용히 동작"하는 방식은 금지)
	 */
	private static byte[] loadSecretKey() {
		String key = System.getenv("SECURITY_AES_KEY");

		if (key == null || key.isEmpty()) {
			throw new IllegalStateException(
					"SECURITY_AES_KEY 환경변수가 설정되지 않았습니다. "
					+ "암호키를 소스코드에 하드코딩하지 않도록, 반드시 환경변수로 주입해야 합니다.");
		}

		byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);

		if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
			throw new IllegalStateException(
					"SECURITY_AES_KEY 길이가 올바르지 않습니다. AES 키는 16/24/32바이트여야 합니다. "
					+ "현재 길이: " + keyBytes.length + "바이트");
		}

		return keyBytes;
	}

	/**
	 * AES256 암호화
	 * - 매 호출마다 새로운 랜덤 IV를 생성해서 사용하고, 결과값 앞부분에 그 IV를 붙여서 함께 반환한다.
	 * @param str 평문
	 * @return Base64( IV(16byte) + 암호문 )
	 */
	public static String aes256Encrypt(String str) {
		try {
			byte[] textBytes = str.getBytes("UTF-8");

			byte[] ivBytes = new byte[IV_LENGTH_BYTES];
			new SecureRandom().nextBytes(ivBytes);
			AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);

			SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY_BYTES, "AES");
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

			byte[] encrypted = cipher.doFinal(textBytes);

			// IV + 암호문을 이어붙여서 하나의 값으로 저장/전달 (IV는 비밀정보가 아님)
			byte[] combined = new byte[ivBytes.length + encrypted.length];
			System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
			System.arraycopy(encrypted, 0, combined, ivBytes.length, encrypted.length);

			return Base64.encodeBase64String(combined);

		} catch (Exception e) {
			LoggingUtil.error(e);
			return null; // 실패를 "Fail"이라는 문자열로 감추지 않고 명시적으로 null 반환
		}
	}

	/**
	 * AES256 복호화
	 * - 앞의 16바이트를 IV로 분리해서 사용한다.
	 * @param str Base64( IV(16byte) + 암호문 )
	 * @return 평문
	 */
	public static String aes256Decrypt(String str) {
		try {
			byte[] combined = Base64.decodeBase64(str);

			if (combined.length <= IV_LENGTH_BYTES) {
				throw new IllegalArgumentException("암호화된 값의 길이가 올바르지 않습니다.");
			}

			byte[] ivBytes = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
			byte[] encryptedBytes = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

			AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
			SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY_BYTES, "AES");
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

			return new String(cipher.doFinal(encryptedBytes), "UTF-8");

		} catch (Exception e) {
			LoggingUtil.error(e);
			return null;
		}
	}

}
