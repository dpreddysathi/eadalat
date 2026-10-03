package com.eadalat.casework.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a lightweight extractive summary of a case's title + description:
 * the first two sentences plus the top keywords by token frequency.
 */
@Component
public class SummaryUtil {

    private static final int MAX_KEYWORDS = 8;

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "that", "this", "from", "are", "was",
            "were", "has", "have", "had", "not", "but", "what", "all", "can",
            "its", "our", "their", "been", "being", "into", "upon", "over",
            "under", "between", "through", "during", "such", "who", "whom",
            "which", "when", "where", "why", "how", "out", "about", "also",
            "may", "will", "would", "should", "could", "there", "here", "your"
    );

    public String buildSummary(String title, String description) {
        String text = joinNonBlank(title, description);
        if (text.isBlank()) {
            return "";
        }
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String sentence : sentences) {
            if (sentence.isBlank()) {
                continue;
            }
            if (count > 0) {
                sb.append(' ');
            }
            sb.append(sentence.trim());
            count++;
            if (count == 2) {
                break;
            }
        }
        List<String> keywords = extractKeywords(title, description);
        if (!keywords.isEmpty()) {
            sb.append(" Key terms: ").append(String.join(", ", keywords));
        }
        return sb.toString();
    }

    public List<String> extractKeywords(String title, String description) {
        String text = joinNonBlank(title, description);
        Map<String, Integer> frequency = new HashMap<>();
        for (String raw : text.split("[^A-Za-z0-9']+")) {
            String token = raw.toLowerCase().replace("'", "");
            if (token.length() <= 2 || STOPWORDS.contains(token)) {
                continue;
            }
            frequency.merge(token, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(frequency.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey));
        List<String> keywords = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            if (keywords.size() == MAX_KEYWORDS) {
                break;
            }
            keywords.add(entry.getKey());
        }
        return keywords;
    }

    private String joinNonBlank(String title, String description) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append(title.strip());
        }
        if (description != null && !description.isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(description.strip());
        }
        return sb.toString();
    }
}
