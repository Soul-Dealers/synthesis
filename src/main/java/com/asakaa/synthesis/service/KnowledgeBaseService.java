package com.asakaa.synthesis.service;

import com.asakaa.synthesis.domain.dto.response.KnowledgeBaseCitation;
import com.asakaa.synthesis.integration.knowledgebase.KnowledgeBaseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseClient knowledgeBaseClient;

    /**
     * Query medical guidelines for relevant information
     * @param symptoms Clinical symptoms or condition description
     * @return List of relevant citations from WHO guidelines
     */
    public List<KnowledgeBaseCitation> queryGuidelines(String symptoms) {
        log.info("Querying medical guidelines for: {}", symptoms);

        try {
            List<KnowledgeBaseCitation> citations = knowledgeBaseClient.retrieve(symptoms, 5);

            if (citations.isEmpty()) {
                log.warn("No guidelines found for query: {}", symptoms);
            } else {
                log.info("Found {} relevant guideline citations", citations.size());
            }

            return citations;

        } catch (Exception e) {
            log.error("Failed to query guidelines, proceeding without KB context", e);
            return List.of();
        }
    }

    /**
     * Format citations for inclusion in AI prompt
     */
    public String formatCitationsForPrompt(List<KnowledgeBaseCitation> citations) {
        if (citations == null || citations.isEmpty()) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();
        formatted.append("\n\nTRUSTED MEDICAL GUIDELINES:\n");
        formatted.append("Use the following evidence-based guidelines to inform your diagnosis:\n\n");

        for (int i = 0; i < citations.size(); i++) {
            KnowledgeBaseCitation citation = citations.get(i);
            formatted.append(String.format("[Guideline %d] (Source: %s, Relevance: %.2f)\n",
                    i + 1, citation.getSource(), citation.getRelevanceScore()));
            formatted.append(citation.getText());
            formatted.append("\n\n");
        }

        formatted.append("IMPORTANT: Base your recommendations on these guidelines when applicable. ");
        formatted.append("Cite the guideline number (e.g., [Guideline 1]) in your reasoning.\n");

        return formatted.toString();
    }

    /**
     * Extract citation references from AI response
     */
    public List<String> extractCitationReferences(String aiResponse, List<KnowledgeBaseCitation> citations) {
        List<String> references = new java.util.ArrayList<>();

        if (citations == null || citations.isEmpty()) {
            return references;
        }

        for (int i = 0; i < citations.size(); i++) {
            String guidelineRef = "[Guideline " + (i + 1) + "]";
            if (aiResponse.contains(guidelineRef)) {
                KnowledgeBaseCitation citation = citations.get(i);
                references.add(String.format("%s: %s", citation.getSource(), 
                        truncateText(citation.getText())));
            }
        }

        return references;
    }

    private String truncateText(String text) {
        if (text == null || text.length() <= 100) {
            return text;
        }
        return text.substring(0, 100) + "...";
    }
}
