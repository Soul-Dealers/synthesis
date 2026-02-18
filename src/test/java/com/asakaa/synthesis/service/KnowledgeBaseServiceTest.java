package com.asakaa.synthesis.service;

import com.asakaa.synthesis.domain.dto.response.KnowledgeBaseCitation;
import com.asakaa.synthesis.integration.knowledgebase.KnowledgeBaseClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock
    private KnowledgeBaseClient knowledgeBaseClient;

    @InjectMocks
    private KnowledgeBaseService knowledgeBaseService;

    @Test
    void queryGuidelines_WithValidSymptoms_ReturnsCitations() {
        // Arrange
        String symptoms = "severe malaria treatment";
        List<KnowledgeBaseCitation> mockCitations = List.of(
                KnowledgeBaseCitation.builder()
                        .text("WHO Guidelines: IV Artesunate is preferred for severe malaria")
                        .source("WHO_Malaria_Guidelines_2023.pdf")
                        .relevanceScore(0.95)
                        .build()
        );

        when(knowledgeBaseClient.retrieve(eq(symptoms), anyInt())).thenReturn(mockCitations);

        // Act
        List<KnowledgeBaseCitation> result = knowledgeBaseService.queryGuidelines(symptoms);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("WHO_Malaria_Guidelines_2023.pdf", result.get(0).getSource());
        verify(knowledgeBaseClient).retrieve(eq(symptoms), anyInt());
    }

    @Test
    void queryGuidelines_WhenClientThrows_ReturnsEmptyList() {
        // Arrange
        String symptoms = "test symptoms";
        when(knowledgeBaseClient.retrieve(anyString(), anyInt()))
                .thenThrow(new RuntimeException("KB error"));

        // Act
        List<KnowledgeBaseCitation> result = knowledgeBaseService.queryGuidelines(symptoms);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void formatCitationsForPrompt_WithCitations_ReturnsFormattedString() {
        // Arrange
        List<KnowledgeBaseCitation> citations = List.of(
                KnowledgeBaseCitation.builder()
                        .text("Treatment guideline text")
                        .source("WHO_Guidelines.pdf")
                        .relevanceScore(0.9)
                        .build()
        );

        // Act
        String result = knowledgeBaseService.formatCitationsForPrompt(citations);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("TRUSTED MEDICAL GUIDELINES"));
        assertTrue(result.contains("[Guideline 1]"));
        assertTrue(result.contains("WHO_Guidelines.pdf"));
    }

    @Test
    void formatCitationsForPrompt_WithEmptyList_ReturnsEmptyString() {
        // Act
        String result = knowledgeBaseService.formatCitationsForPrompt(List.of());

        // Assert
        assertEquals("", result);
    }

    @Test
    void extractCitationReferences_WhenAiReferencesGuideline_ReturnsReferences() {
        // Arrange
        String aiResponse = "Based on [Guideline 1], we recommend IV Artesunate";
        List<KnowledgeBaseCitation> citations = List.of(
                KnowledgeBaseCitation.builder()
                        .text("WHO Guidelines: IV Artesunate is preferred")
                        .source("WHO_Malaria_Guidelines_2023.pdf")
                        .relevanceScore(0.95)
                        .build()
        );

        // Act
        List<String> result = knowledgeBaseService.extractCitationReferences(aiResponse, citations);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("WHO_Malaria_Guidelines_2023.pdf"));
    }

    @Test
    void extractCitationReferences_WhenNoReferences_ReturnsEmptyList() {
        // Arrange
        String aiResponse = "General medical recommendation";
        List<KnowledgeBaseCitation> citations = List.of(
                KnowledgeBaseCitation.builder()
                        .text("Some guideline")
                        .source("source.pdf")
                        .relevanceScore(0.8)
                        .build()
        );

        // Act
        List<String> result = knowledgeBaseService.extractCitationReferences(aiResponse, citations);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
