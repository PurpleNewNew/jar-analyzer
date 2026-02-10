/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.leak;

import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("all")
public class BaseRule {
    private static final Logger logger = LogManager.getLogger();

    static List<String> matchGroup0(String regex, String input) {
        String val = maybeDecodeBase64(input);
        List<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(val);
        while (matcher.find()) {
            results.add(matcher.group().trim());
        }
        return results;
    }

    public static List<String> matchGroup1(String regex, String input) {
        String val = maybeDecodeBase64(input);
        List<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(val);
        while (matcher.find()) {
            results.add(matcher.group(1).trim());
        }
        return results;
    }

    // 添加支持获取第二个捕获组的方法
    public static List<String> matchGroup2(String regex, String input) {
        String val = maybeDecodeBase64(input);
        List<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(val);
        while (matcher.find()) {
            if (matcher.groupCount() >= 2) {
                results.add(matcher.group(2).trim());
            }
        }
        return results;
    }

    // 添加多正则匹配方法 - Java 8 兼容版本
    public static List<String> matchMultipleRegex(List<String> regexList, String input) {
        List<String> results = new ArrayList<>();
        for (String regex : regexList) {
            results.addAll(matchGroup1(regex, input));
        }
        // Java 8 兼容写法
        return results.stream().distinct().collect(Collectors.toList());
    }

    // 添加验证方法
    public static boolean isValidMatch(String pattern, String input) {
        Pattern p = Pattern.compile(pattern);
        return p.matcher(input).matches();
    }

    // 添加敏感度配置
    public static List<String> matchWithSensitivity(String regex, String input, boolean caseSensitive) {
        String val = preprocessInput(input);
        List<String> results = new ArrayList<>();
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(regex, flags);
        Matcher matcher = pattern.matcher(val);
        while (matcher.find()) {
            results.add(matcher.group(1).trim());
        }
        return results;
    }

    private static String preprocessInput(String input) {
        return maybeDecodeBase64(input);
    }

    private static String maybeDecodeBase64(String input) {
        if (!LeakContext.isDetectBase64Enabled()) {
            return input;
        }
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        String trimmed = input.trim();
        if (!looksLikeBase64(trimmed)) {
            return input;
        }
        try {
            return new String(Base64.getDecoder().decode(trimmed));
        } catch (IllegalArgumentException ex) {
            // base64 could be user-controlled and may contain secrets, only log length.
            logger.debug("base64 decode failed (len={}): {}", trimmed.length(), ex.toString());
            return input;
        }
    }

    private static boolean looksLikeBase64(String value) {
        if (value == null) {
            return false;
        }
        int len = value.length();
        if (len < 8 || (len % 4) != 0) {
            return false;
        }
        int firstEq = -1;
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c == '=') {
                if (firstEq < 0) {
                    firstEq = i;
                }
                continue;
            }
            if (firstEq >= 0) {
                return false;
            }
            boolean ok = (c >= 'A' && c <= 'Z')
                    || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '+' || c == '/';
            if (!ok) {
                return false;
            }
        }
        if (firstEq < 0) {
            return true;
        }
        return firstEq >= len - 2;
    }
}
