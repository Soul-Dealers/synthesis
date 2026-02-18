package com.asakaa.synthesis.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseCitation {

    private String text;
    private String source;
    private Double relevanceScore;
}
