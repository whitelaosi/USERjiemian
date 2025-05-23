package com.example.rdfsearch.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryTemplateUtil {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(.*?)\\}\\}");

    /**
     * 将模板中的占位符替换为实际值
     * 
     * @param template SPARQL查询模板字符串
     * @param params   参数键值对
     * @return 生成的SPARQL查询
     */
    public static String applyTemplate(String template, Map<String, Object> params) {
        if (template == null || params == null) {
            return template;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1).trim();
            Object value = params.getOrDefault(placeholder, "");

            // 处理不同类型的值进行适当的转义和格式化
            String replacement = formatValue(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 根据值的类型格式化为SPARQL中可用的表示
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof String) {
            // 处理字符串，添加引号和转义特殊字符
            String stringValue = (String) value;

            // 检查是否是URI
            if (stringValue.startsWith("http://") || stringValue.startsWith("https://")) {
                return "<" + stringValue + ">";
            } else {
                return "\"" + stringValue.replace("\"", "\\\"") + "\"";
            }
        } else if (value instanceof Number) {
            // 数字无需引号
            return value.toString();
        } else if (value instanceof Boolean) {
            // 布尔值转换为true/false
            return value.toString();
        } else {
            // 其他类型转为字符串并添加引号
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
    }
}