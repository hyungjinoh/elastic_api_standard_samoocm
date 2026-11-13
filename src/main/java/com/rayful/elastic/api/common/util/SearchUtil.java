package com.rayful.elastic.api.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.rayful.elastic.api.common.vo.ApiResultVO;
import com.rayful.elastic.api.common.vo.SearchResultVO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchUtil {
	
	static enum CodeType { chosung, jungsung, jongsung } 
	private final static String ignoreChars = "`1234567890-=[]\\;',./~!@#$%^&*()_+{}|:\"<>? "; 
	
	// 초성 
	private static final String init = "rRseEfaqQtTdwWczxvg"; 
	// 중성 
	private static final String[] mid = { "k", "o", "i", "O", "j", "p", "u", "P", "h", "hk", "ho", "hl", "y", "n", "nj", "np", "nl", "b", "m", "ml", "l" }; 
	// 종성 
	private static final String[] fin = { "r", "R", "rt", "s", "sw", "sg", "e", "f", "fr", "fa", "fq", "ft", "fx", "fv", "fg", "a", "q", "qt", "t", "T", "d", "w", "c", "z", "x", "v", "g" }; 
	
	/**
	 * Return header
	 * @param returnType
	 * @return
	 */
	public static HttpHeaders getHttpHeaders(String returnType) {
		HttpHeaders resHeaders = new HttpHeaders();
		String mediaType = null;
		
		if(StringUtils.hasText(returnType) && returnType.toUpperCase().indexOf("JSON") > -1) {
			mediaType = "application/json;charset=\"UTF-8\"";
		
		} else {
			mediaType = "text/xml;charset=\"UTF-8\"";
		}
		
		resHeaders.set("Content-Type", mediaType);
		
		return resHeaders;
	}
	
	/**
	 * String to List
	 * @param val
	 * @param separater
	 * @return
	 */
	public static List<String> getStringToList(String val, String separater) {
		List<String> resultList = null;
		String[] arrayVal = null;
		
		if(StringUtils.hasText(val)) {
			arrayVal = val.replaceAll(" ", "").split(separater);
			
			if(arrayVal.length > 0) {
				resultList = Arrays.asList(arrayVal);
			}
		
		} else {
			resultList = Arrays.asList();
		}
		
		return resultList;
	}
	
	/**
	 * 밀리초를 초로 변환
	 * @param milliseconds
	 * @return
	 */
	public static double getMsecToSec(int milliseconds) {
		return (double) milliseconds / 1000.0;
	}
	
	/**
	 * 검색결과 프린트
	 */
	public static void resultPrettyPrinter(SearchResultVO resultVO) {
		Map<String, Object> result = resultVO.getResult();
		// Jackson 3: ObjectMapper는 추상/불변화되어 JSON은 JsonMapper 사용. (tools.jackson.databind.json.JsonMapper)
		JsonMapper mapper = new JsonMapper();
		
		try {
			String json = mapper.writeValueAsString(result);
			json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
			log.debug("\n" + json);
			
		} catch (JacksonException e) {
			// Jackson 3에서 직렬화 예외는 unchecked(JacksonException)로 변경됨
			log.warn("결과 pretty print 직렬화 실패", e);
		}
	}
	
	/**
	 * URLDecode (UTF-8)
	 * @param str
	 * @return
	 */
	public static String getUrlDecode(String str) {
		String result = null;
		
		if(StringUtils.hasText(str)) {
			try {
				if(str.indexOf("%25") > -1) {
					str = str.replace("%25", "%");
				}

				result = new String(URLDecoder.decode(str, "UTF-8"));
				
			} catch (Exception e) {
				result = str;
			}
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param format
	 * @return
	 * @throws Exception
	 */
	public static String getDate(String format) {
		String result = null;
		
		try {
			result = new SimpleDateFormat(format).format(new Date());
			
		} catch (Exception e) {
			format = "yyyyMMddHHmmssSSS";
			result = new SimpleDateFormat(format).format(new Date());
			LoggingUtil.error(e);
			log.debug("Invalid date format. changed it to ‘yyyyMMddHHmmssSSS’");
		}
		
		return result;
	}
	
	/**
	 * INI 라인 내용 중 '#' 주석 제거, 8859_1 to KSC5601, trim
	 * @param str
	 * @return
	 */
	public static String processIniData(String str, boolean isEncode) {
		String result = str;
		
		if(StringUtils.hasText(result)) {
			if(isEncode) {
				try {
					result = new String(str.getBytes("8859_1"), "KSC5601");
				
				} catch (UnsupportedEncodingException e) {
					result = str;
				}
			}
			
			if(result.indexOf("#") > -1) {
				result = result.split("#")[0];
			}
			
			result = result.trim();
		}
		
		return result;
	}
	
	/**
	 * 로컬 호스트에 대한 아이피
	 * @return
	 */
	public static String getHostAddress() {
		String result = null;
		InetAddress ip = null;
		
		try {
			ip = InetAddress.getLocalHost();
			result = ip.getHostAddress();
		
		} catch (UnknownHostException e) {
			LoggingUtil.error(e);
		}
		
		return result;
	}
	
	/**
	 * 지정된 길이의 바이트 수를 가지도록 설정
	 * @param str
	 * @param len
	 * @param fillStr
	 * @return
	 */
	public static String getByteFill(String str, int len, String fillStr) {
		String result = "";
		
		if (StringUtils.hasText(str)) {
			int fillCnt = len - str.getBytes().length;
			
			if (fillCnt > 0) {
				StringBuilder stringBuilder = new StringBuilder(str);
				
				for (int i = 0; i < fillCnt; i++) {
					stringBuilder.append(fillStr);
				}
				
				result = stringBuilder.toString();
		    
			} else {
				result = str;
			}
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param str
	 * @param sLoc
	 * @param eLoc
	 * @return
	 */
	public static String getStr(String str, int sLoc, int eLoc) {
		String result = null;
		byte[] bStr = null;
		
		try {
			bStr = str.getBytes();
			result = new String(bStr, sLoc, eLoc);
			
		} catch (Exception e) {
			result = str;
			LoggingUtil.error(e);
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param str
	 * @param inWord
	 * @return
	 */
	public static String getAttr(String str, String inWord) {
		String result = "";
		String[] aStr = null;
		
		if(StringUtils.hasText(str)) {
			aStr = str.split("&");
			
			for (String item : aStr) {
				if(item.indexOf(inWord + "=") > -1) {
					result = item.replace(inWord + "=", "");
				}
			}
		}
		
		return result;
	}
	
	/**
	 * properties to hashMap
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public static Map<String, String> loadProperties(InputStream inputStream) throws IOException {
		Properties properties = new Properties();
		properties.load(inputStream);
		
		// Properties를 HashMap으로 변환
		Map<String, String> propertyMap = new HashMap<String, String>();
		for (String key : properties.stringPropertyNames()) {
		    propertyMap.put(key, properties.getProperty(key));
		}
		
		return propertyMap;
	}
	
	/**
	 * 
	 * @param str
	 * @return
	 */
	public static String getCtn(String str) {
		String result = "";
		
		if(StringUtils.hasText(str) && str.length() >= 11) {
			if (str.charAt(4) == '0') {
				result = str.substring(0, 3) + str.substring(5, 12);
			
			} else {
				result = str.substring(0, 3) + str.substring(4, 12);
			}
		
		} else {
			result = "01000000000";
		}
		
		return result;
	}
	
	/**
	 * null일 경우 빈값으로 치환
	 * @param str
	 * @return
	 */
	public static String replaceNullToEmpty(String str) {
        return str != null ? str : "";
    }
	
	/**
     * 입력한 값이 초성, 숫자, 특수문자, 공백, 또는 심볼로만 되어있는지 체크
     * 
     * @param str 입력 문자열
     * @return true - 문자열이 초성, 숫자, 특수문자, 공백, 또는 심볼로만 이루어져 있는 경우, false - 그 외의 경우
     */
    public static boolean isChosungOnly(String str) {
        // 정규 표현식을 사용하여 한글 초성(ㄱ-ㅎ), 숫자(0-9), 특수문자, 공백, 심볼을 포함한 패턴에 대해 검사
        return str.matches("^(?=.*[ㄱ-ㅎ])[ㄱ-ㅎ0-9\\s\\p{Punct}％]*$");
    }
	
	/**
	 * 파일 인코딩 확인
	 * @param filePath
	 * @return
	 */
	public static String getFileEncoding(String filePath) {
		String result = null;
		Path path = Paths.get(filePath);
		Charset charset = Charset.forName("UTF-8");
		String fileName = path.getFileName().toString();
		
		if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
			throw new IllegalArgumentException("Invalid filename");
		}
		
		// 파일의 존재 여부 확인
		if (Files.exists(path)) {
			try(BufferedReader br = Files.newBufferedReader(path, charset)) {
				br.readLine();
				result = "UTF-8";
    			
			} catch (Exception e) {
				result = "EUC-KR";
			}
		}
		
		return result;
	}
	
	/**
	 * TLO 로그 아이디 생성
	 */
	public static String getTloTraceId() {
		String result = null;
		Random random = new Random();
		int min = 1;
        int max = 99999999;
        int iSeq = random.nextInt(max - min + 1) + min;
        StringBuffer sb = new StringBuffer("" + iSeq);
        
        while (sb.length() < 8) {
			sb.insert(0, "0");
		}
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String sDate = sdf.format(new Date());
        result = sDate + sb.toString();
		
		return result;
	}
	
	/**
	 * 검색 로그 아이디 생성
	 */
	public static String getTraceId(String serviceType) {
		String result = null;
		Random random = new Random();
		int min = 1;
        int max = 999;
        int iSeq = random.nextInt(max - min + 1) + min;
        StringBuffer sb = new StringBuffer("" + iSeq);
        
        while (sb.length() < 3) {
			sb.insert(0, "0");
		}
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmmss");
        String sDate = sdf.format(new Date());
        
        if("popkeyword".equalsIgnoreCase(serviceType)) {
        	result = "POP_" + sDate + "_" + sb.toString();
        
        } else if("auto".equalsIgnoreCase(serviceType)) {
        	result = "AUTO_" + sDate + "_" + sb.toString();
        
        } else {
        	result = "SCH_" + sDate + "_" + sb.toString();
        } 
		
		return result;
	}
	
	/**
	 * base64 encoding
	 * @param str
	 * @return
	 */
	public static String getBase64EnStr(String str) {
		String encodedString = null;
		
		if(str != null && str.length() > 0) {
	        byte[] encodedBytes = Base64.getEncoder().encode(str.getBytes());
	        encodedString = new String(encodedBytes);
		}
		
		return encodedString;
	}
	
	/** 영어를 한글로 변환. 
	 * @param eng 영문
	 **/
	public static String engToKor(String eng) { 
		StringBuffer sb = new StringBuffer(); 
		
		int initialCode = 0, medialCode = 0, finalCode = 0; 
		int tempMedialCode, tempFinalCode; for (int i = 0; i < eng.length(); i++) { // 숫자특수문자 처리 
			if(ignoreChars.indexOf(eng.substring(i, i + 1)) > -1){ 
				sb.append(eng.substring(i, i + 1)); 
				continue; 
			} 
			
			// 초성코드 추출 
			initialCode = getCode(CodeType.chosung, eng.substring(i, i + 1)); 
			i++; 
			
			// 다음문자로 중성코드 추출 
			tempMedialCode = getDoubleMedial(i, eng); 
			
			// 두 자로 이루어진 중성코드 추출 
			if (tempMedialCode != -1) { 
				medialCode = tempMedialCode; i += 2; 
			} else { 
				// 없다면, 
				medialCode = getSingleMedial(i, eng); 
				// 한 자로 이루어진 중성코드 추출 
				i++; 
			} 
			
			// 종성코드 추출 
			tempFinalCode = getDoubleFinal(i, eng); 
			
			// 두 자로 이루어진 종성코드 추출 
			if (tempFinalCode != -1) { 
				finalCode = tempFinalCode; 
				
				// 그 다음의 중성 문자에 대한 코드를 추출한다. 
				tempMedialCode = getSingleMedial(i + 2, eng); 
				if (tempMedialCode != -1) { 
					// 코드 값이 있을 경우 
					finalCode = getSingleFinal(i, eng); 
					// 종성 코드 값을 저장한다. 
				} else { 
						i++; 
				} 
			} else { 
				// 코드 값이 없을 경우 , 
				tempMedialCode = getSingleMedial(i + 1, eng); 
				// 그 다음의 중성 문자에 대한 코드 추출. 
				if (tempMedialCode != -1) { 
					// 그 다음에 중성 문자가 존재할 경우, 
					finalCode = 0; // 종성 문자는 없음. 
					i--; 
				} else { 
					finalCode = getSingleFinal(i, eng); 
					// 종성 문자 추출 
					if (finalCode == -1){ 
						finalCode = 0; i--; 
						// 초성,중성 + 숫자,특수문자, 
						//기호가 나오는 경우 index를 줄임. 
					} 
				} 
			} 
			
			// 추출한 초성 문자 코드, 
			//중성 문자 코드, 종성 문자 코드를 합한 후 변환하여 스트링버퍼에 넘김 
			sb.append((char) (0xAC00 + initialCode + medialCode + finalCode)); 
		}
		
		return sb.toString(); 
	} 
	
	/** 해당 문자에 따른 코드를 추출한다. 
	 * @param type 초성 : chosung, 중성 : jungsung, 종성 : jongsung 구분 
	 * @param c 해당 문자 
	 **/ 
	static private int getCode(CodeType type, String c) { 
		switch (type) { 
			case chosung: 
				int index = init.indexOf(c); 
				if (index != -1) { return index * 21 * 28; } 
				break; 
			case jungsung: 
				for (int i = 0; i < mid.length; i++) { 
					if (mid[i].equals(c)) { 
						return i * 28; 
					} 
				} 
				break; 
			case jongsung: 
				for (int i = 0; i < fin.length; i++) { 
					if (fin[i].equals(c)) { 
						return i + 1; 
					} 
				} 
				break; 
			default: 
				System.out.println("잘못된 타입 입니다"); 
		} 
		
		return -1; 
	} 
	// 한 자로 된 중성값을 리턴한다
	// 인덱스를 벗어낫다면 -1을 리턴 
	static private int getSingleMedial(int i, String eng) { 
		if ((i + 1) <= eng.length()) { 
			return getCode(CodeType.jungsung, eng.substring(i, i + 1)); 
		} else { 
			return -1; 
		} 
	} 
	// 두 자로 된 중성을 체크하고, 있다면 값을 리턴한다. 
	// 없으면 리턴값은 -1 
	static private int getDoubleMedial(int i, String eng) { 
		int result; if ((i + 2) > eng.length()) { 
			return -1; 
		} else { 
			result = getCode(CodeType.jungsung, eng.substring(i, i + 2)); 
			if (result != -1) {
				return result; 
			} else { 
				return -1; 
			} 
		} 
	} 
	// 한 자로된 종성값을 리턴한다 
	// 인덱스를 벗어낫다면 -1을 리턴 
	static private int getSingleFinal(int i, String eng) { 
		if ((i + 1) <= eng.length()) { 
			return getCode(CodeType.jongsung, eng.substring(i, i + 1)); 
		} else { 
			return -1; 
		} 
	} 
	// 두 자로된 종성을 체크하고, 있다면 값을 리턴한다. 
	// 없으면 리턴값은 -1 
	static private int getDoubleFinal(int i, String eng) { 
		if ((i + 2) > eng.length()) { 
			return -1; 
		} else { 
			return getCode(CodeType.jongsung, eng.substring(i, i + 2)); 
			} 
	} 
	
	/**
	 * @param input
	 * @param keyword
	 * @param engTokorKeyword
	 * @param startTag
	 * @param endTag
	 * 
	 */
	public static String getHighlightingString(String input, String keyword, String engTokorKeyword, String startTag, String endTag) {
        // 공백 제거된 키워드
        String keywordNoSpaces = keyword.replaceAll("\\s+", "");

        // 대소문자를 구분하지 않고 검색어를 강조
        String highlightingString = highlightKeyword(input, keyword, keywordNoSpaces, startTag, endTag);

        // 키워드가 강조되지 않았다면, 오타 키워드를 강조
        if (!highlightingString.contains(startTag)) {
            highlightingString = highlightKeyword(input, engTokorKeyword, engTokorKeyword.replaceAll("\\s+", ""), startTag, endTag);
        }

        return highlightingString;
    }

    private static String highlightKeyword(String input, String keyword, String keywordNoSpaces, String startTag, String endTag) {
        StringBuilder sb = new StringBuilder();
        
        // 특수문자 처리 추가
        Pattern pattern = Pattern.compile(Pattern.quote(keywordNoSpaces), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input.replaceAll("\\s+", ""));

        int lastIndex = 0;
        while (matcher.find()) {
            int start = findPosition(input, matcher.start(), keywordNoSpaces.length());
            int end = findPosition(input, matcher.end() - 1, keywordNoSpaces.length()) + 1;
            sb.append(input, lastIndex, start).append(startTag);
            sb.append(input, start, end).append(endTag);
            lastIndex = end;
        }
        sb.append(input.substring(lastIndex));

        return sb.toString();
    }

    private static int findPosition(String input, int noSpaceIndex, int keywordLength) {
        int count = 0;
        int index = -1;

        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) != ' ') {
                if (count == noSpaceIndex) {
                    index = i;
                    break;
                }
                count++;
            }
        }

        return index;
    }
	
	/**
	 * 여러 키워드를 대상으로 입력 문자열(input)에서 일치하는 모든 구간(substring)을 찾아
	 * startTag/endTag로 감싸서 반환한다. (단일 검색어용 getHighlightingString()의 다중 키워드 버전)
	 * 
	 * - 대소문자 구분 없이 매칭한다.
	 * - 여러 키워드의 매칭 구간이 겹치거나 인접한 경우, 중첩 태그 없이 하나로 병합해서 감싼다.
	 * - 매칭되는 키워드가 하나도 없으면 원본 문자열을 그대로 반환한다.
	 * 
	 * @param input 하이라이팅 대상 원본 문자열 (예: title 필드 값)
	 * @param keywords 하이라이팅할 키워드 목록 (예: kwdList - 검색어 + 이전 검색어)
	 * @param startTag 시작 태그 (예: "<em>")
	 * @param endTag 종료 태그 (예: "</em>")
	 * @return 하이라이팅 태그가 삽입된 문자열
	 */
	public static String getHighlightingStringMulti(String input, List<String> keywords, String startTag, String endTag) {
		if (input == null || input.isEmpty() || keywords == null || keywords.isEmpty()) {
			return input;
		}

		// 1. 키워드별로 매칭 구간(start~end) 전부 수집
		List<int[]> ranges = new ArrayList<>();

		for (String kw : keywords) {
			if (kw == null || kw.trim().isEmpty()) {
				continue;
			}

			try {
				Pattern pattern = Pattern.compile(Pattern.quote(kw.trim()), Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(input);

				while (matcher.find()) {
					ranges.add(new int[] { matcher.start(), matcher.end() });
				}
			} catch (Exception e) {
				LoggingUtil.error(e);
			}
		}

		if (ranges.isEmpty()) {
			return input; // 매칭되는 키워드가 없으면 원본 그대로 반환
		}

		// 2. 구간을 시작 위치 기준으로 정렬 후, 겹치거나 맞닿은 구간을 하나로 병합
		//    (예: "부동산", "동산" 처럼 서로 겹치는 키워드가 있어도 태그가 중첩되지 않도록 함)
		ranges.sort((a, b) -> a[0] - b[0]);

		List<int[]> merged = new ArrayList<>();
		int[] current = ranges.get(0);

		for (int i = 1; i < ranges.size(); i++) {
			int[] next = ranges.get(i);

			if (next[0] <= current[1]) {
				current[1] = Math.max(current[1], next[1]);
			} else {
				merged.add(current);
				current = next;
			}
		}
		merged.add(current);

		// 3. 병합된 구간을 기준으로 태그를 삽입하며 최종 문자열 조립
		StringBuilder sb = new StringBuilder();
		int lastIndex = 0;

		for (int[] range : merged) {
			sb.append(input, lastIndex, range[0]);
			sb.append(startTag);
			sb.append(input, range[0], range[1]);
			sb.append(endTag);
			lastIndex = range[1];
		}
		sb.append(input.substring(lastIndex));

		return sb.toString();
	}
	
	/**
	 * 통합검색(방식 B) 결과 병합
	 * - 일반 인덱스 통합검색 결과(resultSearchParse)와 메뉴 검색결과(resultMenuSearchParse)를
	 *   각각 별도의 ES 호출로 얻은 뒤, 하나의 ApiResultVO(sectionList)로 합친다.
	 * - resultSearchParse / resultMenuSearchParse 두 메서드 모두 수정하지 않고,
	 *   각자 만든 최종 apiResult(Map)만 후처리로 병합한다.
	 *
	 * @param baseResult 일반 인덱스 통합검색 결과 (sectionList 앞쪽에 위치)
	 * @param additionalResult 메뉴 검색 결과 (sectionList 뒤에 append)
	 * @return 병합된 ApiResultVO (baseResult 인스턴스를 그대로 재사용)
	 */
	@SuppressWarnings("unchecked")
	public static ApiResultVO mergeApiResult(ApiResultVO baseResult, ApiResultVO additionalResult) {
		
		if (baseResult == null) {
			return additionalResult;
		}
		if (additionalResult == null) {
			return baseResult;
		}
		
		Map<String, Object> baseMap = (Map<String, Object>) baseResult.getApiResult();
		Map<String, Object> addMap = (Map<String, Object>) additionalResult.getApiResult();
		
		if (baseMap == null) {
			baseResult.setApiResult(addMap);
			return baseResult;
		}
		if (addMap == null) {
			return baseResult;
		}
		
		List<Map<String, Object>> baseSectionList = (List<Map<String, Object>>) baseMap.get("sectionList");
		List<Map<String, Object>> addSectionList = (List<Map<String, Object>>) addMap.get("sectionList");
		
		List<Map<String, Object>> mergedSectionList = new ArrayList<>();
		if (baseSectionList != null) {
			mergedSectionList.addAll(baseSectionList);
		}
		if (addSectionList != null) {
			mergedSectionList.addAll(addSectionList);
		}
		
		int baseTotalCnt = baseMap.get("totalCnt") != null ? (int) baseMap.get("totalCnt") : 0;
		int addTotalCnt = addMap.get("totalCnt") != null ? (int) addMap.get("totalCnt") : 0;
		
		int baseSectionSize = baseMap.get("sectionSize") != null ? (int) baseMap.get("sectionSize") : 0;
		int addSectionSize = addMap.get("sectionSize") != null ? (int) addMap.get("sectionSize") : 0;
		
		baseMap.put("sectionList", mergedSectionList);
		baseMap.put("totalCnt", baseTotalCnt + addTotalCnt);
		baseMap.put("sectionSize", baseSectionSize + addSectionSize);
		
		return baseResult;
	}
	
}
