package com.aiexam;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.regex.Pattern;

/** 将模型的文字选项或对象选项统一为 key/text，并校验标准答案。 */
public final class AiQuestionNormalizer {
    private static final Pattern PREFIX=Pattern.compile("^([A-Ha-h])[.、:：)）\\s]+(.+)$",Pattern.DOTALL);
    private AiQuestionNormalizer() {}

    public static List<Map<String,Object>> normalize(String type,int count,JsonNode array) {
        if(!array.isArray()||array.size()!=count) throw new IllegalArgumentException("模型返回题目数量与请求不一致");
        List<Map<String,Object>> result=new ArrayList<>();
        for(JsonNode node:array) {
            String content=node.path("content").asText("").trim();
            String answer=node.path("answer").asText("").trim();
            if(content.isBlank()||answer.isBlank()) throw new IllegalArgumentException("题干或标准答案缺失");
            Map<String,Object> question=new LinkedHashMap<>();
            question.put("content",content);
            if("SINGLE".equals(type)||"MULTI".equals(type)) {
                var options=options(node.path("options"));
                Set<String> keys=new HashSet<>();options.forEach(o->keys.add(o.get("key")));
                answer=answer.toUpperCase(Locale.ROOT).replace("，",",").replace("、",",");
                if("SINGLE".equals(type)) {
                    if(!keys.contains(answer)) throw new IllegalArgumentException("单选题答案不在选项中");
                } else {
                    if(answer.matches("[A-H]{2,}"))answer=String.join(",",answer.split(""));
                    SortedSet<String> choices=new TreeSet<>();
                    for(String key:answer.split(",",-1)) {
                        key=key.trim();
                        if(!keys.contains(key))throw new IllegalArgumentException("多选题答案不在选项中");
                        choices.add(key);
                    }
                    if(choices.size()<2)throw new IllegalArgumentException("多选题至少需要两个正确选项");
                    answer=String.join(",",choices);
                }
                question.put("options",options);
            } else {
                question.put("options",null);
                if("JUDGE".equals(type)) {
                    answer=answer.toLowerCase(Locale.ROOT);
                    if(!Set.of("true","false").contains(answer))throw new IllegalArgumentException("判断题答案应为true或false");
                }
            }
            question.put("answer",answer);
            question.put("analysis",node.path("analysis").asText(""));
            result.add(question);
        }
        return result;
    }

    private static List<Map<String,String>> options(JsonNode raw) {
        List<Map<String,String>> result=new ArrayList<>();
        if(raw.isObject()) {
            raw.fields().forEachRemaining(e->{
                if(!e.getValue().isTextual())throw new IllegalArgumentException("选项内容必须为文字");
                result.add(option(e.getKey(),e.getValue().asText()));
            });
        } else if(raw.isArray()) {
            for(int i=0;i<raw.size();i++) {
                JsonNode node=raw.get(i);
                String key=String.valueOf((char)('A'+i)),text;
                if(node.isTextual()) {
                    text=node.asText().trim();var match=PREFIX.matcher(text);
                    if(match.matches()){key=match.group(1);text=match.group(2);}
                } else if(node.isObject()) {
                    key=node.path("key").asText(node.path("label").asText(key));
                    text=node.path("text").asText(node.path("content").asText(node.path("value").asText("")));
                } else throw new IllegalArgumentException("选项格式无效");
                result.add(option(key,text));
            }
        } else throw new IllegalArgumentException("选择题缺少选项");
        if(result.size()<2||result.size()>8)throw new IllegalArgumentException("选择题需要2至8个选项");
        Set<String> keys=new HashSet<>();
        for(var o:result)if(!keys.add(o.get("key")))throw new IllegalArgumentException("选项编号重复");
        return result;
    }

    private static Map<String,String> option(String key,String text) {
        key=key.trim().toUpperCase(Locale.ROOT);text=text.trim();
        if(!key.matches("[A-H]")||text.isBlank())throw new IllegalArgumentException("选项编号或内容无效");
        return Map.of("key",key,"text",text);
    }
}
