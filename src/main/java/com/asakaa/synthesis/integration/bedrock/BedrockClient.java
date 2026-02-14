package com.asakaa.synthesis.integration.bedrock;

import com.asakaa.synthesis.exception.DiagnosticException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockClient {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.bedrock.model-id}")
    private String modelId;

    @Value("${synthesis.ai.max-tokens}")
    private int maxTokens;

    @Value("${synthesis.ai.temperature}")
    private double temperature;

    public String invoke(String prompt) {
        try {
            log.info("Invoking Bedrock model: {}", modelId);

            // Build Claude messages API request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("anthropic_version", "bedrock-2023-05-31");
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            
            ArrayNode content = message.putArray("content");
            ObjectNode textContent = content.addObject();
            textContent.put("type", "text");
            textContent.put("text", prompt);

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            log.debug("Request body: {}", requestBodyJson);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(requestBodyJson))
                    .build();

            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();

            log.debug("Response body: {}", responseBody);

            // Parse Claude response to extract the text content
            ObjectNode responseJson = (ObjectNode) objectMapper.readTree(responseBody);
            ArrayNode responseContent = (ArrayNode) responseJson.get("content");
            String responseText = responseContent.get(0).get("text").asText();

            log.info("Successfully received response from Bedrock");
            return responseText;

        } catch (Exception e) {
            log.error("Error invoking Bedrock model", e);
            throw new DiagnosticException("Failed to invoke AI diagnostic model: " + e.getMessage(), e);
        }
    }
}
