package com.asakaa.synthesis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;

@Configuration
public class KnowledgeBaseConfig {

    @Value("${aws.bedrock.region}")
    private String region;

    @Bean
    public BedrockAgentRuntimeClient bedrockAgentRuntimeClient() {
        return BedrockAgentRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}
