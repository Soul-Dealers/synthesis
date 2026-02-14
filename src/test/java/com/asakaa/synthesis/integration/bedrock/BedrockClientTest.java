package com.asakaa.synthesis.integration.bedrock;

import com.asakaa.synthesis.exception.DiagnosticException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BedrockClientTest {

    @Mock
    private BedrockRuntimeClient bedrockRuntimeClient;

    @InjectMocks
    private BedrockClient bedrockClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bedrockClient, "modelId", "anthropic.claude-3-sonnet-20240229-v1:0");
        ReflectionTestUtils.setField(bedrockClient, "maxTokens", 2048);
        ReflectionTestUtils.setField(bedrockClient, "temperature", 0.2);
    }

    @Test
    void invoke_ReturnsResponseString_WhenAwsSdkSucceeds() throws Exception {
        // Arrange
        String prompt = "Test prompt";
        String responseText = "Test response from Claude";
        
        ObjectMapper mapper = new ObjectMapper();
        String responseBody = mapper.writeValueAsString(
            mapper.createObjectNode()
                .set("content", mapper.createArrayNode()
                    .add(mapper.createObjectNode()
                        .put("type", "text")
                        .put("text", responseText)))
        );

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
                .body(SdkBytes.fromUtf8String(responseBody))
                .build();

        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class))).thenReturn(mockResponse);

        // Act
        String result = bedrockClient.invoke(prompt);

        // Assert
        assertNotNull(result);
        assertEquals(responseText, result);
        verify(bedrockRuntimeClient).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    void invoke_ThrowsDiagnosticException_WhenAwsSdkThrows() {
        // Arrange
        String prompt = "Test prompt";
        when(bedrockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
                .thenThrow(new RuntimeException("AWS SDK error"));

        // Act & Assert
        assertThrows(DiagnosticException.class, () -> bedrockClient.invoke(prompt));
    }
}
