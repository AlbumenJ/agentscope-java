/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.core.rag.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for EmbeddingConfiguration.
 */
@Tag("unit")
@DisplayName("EmbeddingConfiguration Unit Tests")
class EmbeddingConfigurationTest {

    @Test
    @DisplayName("Should create EmbeddingConfiguration with default values")
    void testDefaultConfig() {
        EmbeddingConfiguration config = EmbeddingConfiguration.defaultConfig();

        assertEquals("dashscope", config.getProvider());
        assertEquals("text-embedding-v3", config.getModel());
        assertEquals(1024, config.getDimensions());
        assertEquals(Duration.ofSeconds(30), config.getTimeout());
        assertFalse(config.isCacheEnabled());
        assertEquals("./cache/embeddings", config.getCacheDirectory());
        assertNull(config.getApiKey());
    }

    @Test
    @DisplayName("Should create EmbeddingConfiguration with builder")
    void testBuilder() {
        EmbeddingConfiguration config =
                EmbeddingConfiguration.builder()
                        .provider("openai")
                        .model("text-embedding-ada-002")
                        .dimensions(1536)
                        .timeout(Duration.ofMinutes(1))
                        .enableCache(true)
                        .cacheDirectory("./custom/cache")
                        .apiKey("test-api-key")
                        .build();

        assertEquals("openai", config.getProvider());
        assertEquals("text-embedding-ada-002", config.getModel());
        assertEquals(1536, config.getDimensions());
        assertEquals(Duration.ofMinutes(1), config.getTimeout());
        assertTrue(config.isCacheEnabled());
        assertEquals("./custom/cache", config.getCacheDirectory());
        assertEquals("test-api-key", config.getApiKey());
    }

    @Test
    @DisplayName("Should throw exception for null provider")
    void testNullProvider() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EmbeddingConfiguration.builder().provider(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> EmbeddingConfiguration.builder().provider(""));
    }

    @Test
    @DisplayName("Should throw exception for null model")
    void testNullModel() {
        assertThrows(
                IllegalArgumentException.class, () -> EmbeddingConfiguration.builder().model(null));
        assertThrows(
                IllegalArgumentException.class, () -> EmbeddingConfiguration.builder().model(""));
    }

    @Test
    @DisplayName("Should throw exception for invalid dimensions")
    void testInvalidDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EmbeddingConfiguration.builder().dimensions(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> EmbeddingConfiguration.builder().dimensions(-1));
    }

    @Test
    @DisplayName("Should throw exception for null timeout")
    void testNullTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EmbeddingConfiguration.builder().timeout(null));
    }

    @Test
    @DisplayName("Should allow null cache directory")
    void testNullCacheDirectory() {
        EmbeddingConfiguration config =
                EmbeddingConfiguration.builder().cacheDirectory(null).build();
        assertNotNull(config.getCacheDirectory());
    }

    @Test
    @DisplayName("Should allow null API key")
    void testNullApiKey() {
        EmbeddingConfiguration config = EmbeddingConfiguration.builder().apiKey(null).build();
        assertNull(config.getApiKey());
    }
}
