package com.asakaa.synthesis.integration.knowledgebase;

import com.asakaa.synthesis.domain.dto.response.KnowledgeBaseCitation;
import com.asakaa.synthesis.exception.DiagnosticException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseClient {

    private final BedrockAgentRuntimeClient bedrockAgentRuntimeClient;

    @Value("${aws.bedrock.knowledge-base-id}")
    private String knowledgeBaseId;

    /**
     * Query the knowledge base for relevant medical guidelines
     *
     * @param query      Clinical query (symptoms, condition, treatment)
     * @param maxResults Maximum number of results to return
     * @return List of citations with relevant text and sources
     */
    public List<KnowledgeBaseCitation> retrieve(String query, int maxResults) {
        try {
            log.info("Querying knowledge base: {} with query: {}", knowledgeBaseId, query);

            KnowledgeBaseRetrievalConfiguration retrievalConfig =
                    KnowledgeBaseRetrievalConfiguration.builder()
                            .vectorSearchConfiguration(
                                    KnowledgeBaseVectorSearchConfiguration.builder()
                                            .numberOfResults(maxResults)
                                            .build()
                            )
                            .build();

            RetrieveRequest request = RetrieveRequest.builder()
                    .knowledgeBaseId(knowledgeBaseId)
                    .retrievalQuery(
                            KnowledgeBaseQuery.builder()
                                    .text(query)
                                    .build()
                    )
                    .retrievalConfiguration(retrievalConfig)
                    .build();

            RetrieveResponse response = bedrockAgentRuntimeClient.retrieve(request);

            List<KnowledgeBaseCitation> citations = new ArrayList<>();
            for (KnowledgeBaseRetrievalResult result : response.retrievalResults()) {
                if (result.content() != null && result.content().text() != null) {
                    KnowledgeBaseCitation citation = KnowledgeBaseCitation.builder()
                            .text(result.content().text())
                            .source(extractSource(result.location()))
                            .relevanceScore(result.score())
                            .build();
                    citations.add(citation);
                }
            }

            log.info("Retrieved {} citations from knowledge base", citations.size());
            return citations;

        } catch (Exception e) {
            log.error("Error querying knowledge base", e);
            throw new DiagnosticException("Failed to query medical knowledge base: " + e.getMessage(), e);
        }
    }

    /**
     * Extract source information from retrieval result
     */
    private String extractSource(RetrievalResultLocation result) {
        if (result != null && result.s3Location() != null) {
            String uri = result.s3Location().uri();
            if (uri != null && uri.contains("/")) {
                String[] parts = uri.split("/");
                return parts[parts.length - 1];
            }
            return uri;
        }
        return "Unknown Source";
    }
}
