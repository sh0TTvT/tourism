package com.tourismqa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tourismqa.config.AppProperties;
import com.tourismqa.dto.KgContextResponse;
import com.tourismqa.dto.KgNodeResponse;

/**
 * 景点开放状态上下文增强服务。
 * 使用场景：
 * 在用户询问是否开放、是否闭馆、是否需要预约时，优先读取本地结构化属性。
 * 核心职责：
 * 1. 从知识图谱命中的景点实体中提取营业与预约属性。
 * 2. 生成适合大模型消费的开放状态提示文本。
 * 3. 无结构化数据时，明确提示需要参考景区官方公告。
 */
@Service
public class AttractionStatusService {

    private final AppProperties appProperties;

    public AttractionStatusService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String buildPromptContext(String question, KgContextResponse graphContext) {
        if (!appProperties.getRealtime().isEnabled()
                || !appProperties.getRealtime().getAttractionStatus().isEnabled()) {
            return null;
        }
        if (!StringUtils.hasText(question) || !hasAttractionStatusIntent(question)) {
            return null;
        }
        if (graphContext == null || graphContext.nodes().isEmpty()) {
            return "景点开放状态补充：当前未命中本地景点实体，请在回答中明确说明缺少该景点的结构化开放状态数据，并建议用户查看官方公告或预约页面。";
        }

        int maxItems = Math.max(1, appProperties.getRealtime().getAttractionStatus().getMaxItems());
        List<String> lines = new ArrayList<>();
        int count = 0;
        for (KgNodeResponse node : graphContext.nodes()) {
            if (!isAttractionLike(node)) {
                continue;
            }
            String line = buildNodeStatusLine(node);
            if (line != null) {
                lines.add(line);
                count++;
                if (count >= maxItems) {
                    break;
                }
            }
        }

        if (lines.isEmpty()) {
            return "景点开放状态补充：本地知识库未记录该景点的 `openingStatus/openingHours/reservationRequired` 等结构化字段。请不要编造开放情况，需明确提示用户以景区官方公告、预约页或客服电话为准。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下为系统补充的景点开放状态信息，仅在与用户问题直接相关时引用；若字段缺失，不要自行补全。\n");
        sb.append("景点状态：\n");
        for (String line : lines) {
            sb.append("- ").append(line).append("\n");
        }
        sb.append("若信息不完整，请直接建议用户查看景区官方公告。");
        return sb.toString().trim();
    }

    private String buildNodeStatusLine(KgNodeResponse node) {
        Map<String, Object> attributes = node.attributes();
        List<String> facts = new ArrayList<>();

        appendFact(facts, "开放状态", attributes.get("openingStatus"));
        appendFact(facts, "开放时间", firstNonNull(
                attributes.get("openingHours"),
                attributes.get("businessHours"),
                attributes.get("hours")));
        appendFact(facts, "闭馆规则", firstNonNull(
                attributes.get("closedDays"),
                attributes.get("closingRule")));
        appendFact(facts, "预约要求", attributes.get("reservationRequired"));
        appendFact(facts, "补充说明", firstNonNull(
                attributes.get("notice"),
                attributes.get("visitNotice")));

        String verifyUrl = asText(firstNonNull(
                attributes.get("officialUrl"),
                attributes.get("sourceUrl"),
                attributes.get("wikipediaUrl")));
        if (StringUtils.hasText(verifyUrl)) {
            facts.add("核验链接=" + verifyUrl);
        }
        if (facts.isEmpty()) {
            return null;
        }
        return node.name() + "： " + String.join("；", facts);
    }

    private boolean isAttractionLike(KgNodeResponse node) {
        String entityType = asText(node.attributes().get("entityType"));
        if ("web_source".equalsIgnoreCase(entityType)) {
            return false;
        }
        String text = (safe(node.category()) + " "
                + safe(node.description()) + " "
                + String.join(" ", safeList(node.tags()))).toLowerCase();
        String[] keywords = {
                "景点", "景区", "博物馆", "公园", "乐园", "古镇", "寺", "山", "湖", "遗址",
                "attraction", "museum", "park"
        };
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return hasKnownStatusAttributes(node.attributes());
    }

    private boolean hasKnownStatusAttributes(Map<String, Object> attributes) {
        return attributes.containsKey("openingStatus")
                || attributes.containsKey("openingHours")
                || attributes.containsKey("businessHours")
                || attributes.containsKey("reservationRequired")
                || attributes.containsKey("closedDays");
    }

    private boolean hasAttractionStatusIntent(String question) {
        String[] keywords = { "开放", "开门", "营业", "闭馆", "关门", "预约", "开放时间", "营业时间" };
        for (String keyword : keywords) {
            if (question.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void appendFact(List<String> facts, String label, Object value) {
        String text = asText(value);
        if (StringUtils.hasText(text)) {
            facts.add(label + "=" + text);
        }
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asText(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
