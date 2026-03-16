package io.notifyhub.channel.sns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AwsSnsConfig")
class AwsSnsConfigTest {

    @Test
    @DisplayName("Should create config with required fields")
    void shouldCreateConfigWithRequiredFields() {
        AwsSnsConfig config = AwsSnsConfig.builder()
                .region("us-east-1")
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .build();

        assertEquals("us-east-1", config.getRegion());
        assertEquals("AKIAIOSFODNN7EXAMPLE", config.getAccessKeyId());
        assertEquals("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", config.getSecretAccessKey());
        assertNull(config.getTopicArn());
    }

    @Test
    @DisplayName("Should create config with optional topicArn")
    void shouldCreateConfigWithOptionalTopicArn() {
        AwsSnsConfig config = AwsSnsConfig.builder()
                .region("us-east-1")
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .secretAccessKey("secret")
                .topicArn("arn:aws:sns:us-east-1:123456789012:MyTopic")
                .build();

        assertEquals("arn:aws:sns:us-east-1:123456789012:MyTopic", config.getTopicArn());
    }

    @Test
    @DisplayName("Should throw when region is null")
    void shouldThrowWhenRegionIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AwsSnsConfig.builder()
                        .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                        .secretAccessKey("secret")
                        .build());

        assertTrue(ex.getMessage().contains("region"));
    }

    @Test
    @DisplayName("Should throw when accessKeyId is blank")
    void shouldThrowWhenAccessKeyIdIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AwsSnsConfig.builder()
                        .region("us-east-1")
                        .accessKeyId("  ")
                        .secretAccessKey("secret")
                        .build());

        assertTrue(ex.getMessage().contains("accessKeyId"));
    }

    @Test
    @DisplayName("Should throw when secretAccessKey is null")
    void shouldThrowWhenSecretAccessKeyIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AwsSnsConfig.builder()
                        .region("us-east-1")
                        .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                        .build());

        assertTrue(ex.getMessage().contains("secretAccessKey"));
    }
}
