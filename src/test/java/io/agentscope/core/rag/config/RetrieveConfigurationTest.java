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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RetrieveConfiguration.
 */
@Tag("unit")
@DisplayName("RetrieveConfiguration Unit Tests")
class RetrieveConfigurationTest {

    @Test
    @DisplayName("Should create RetrieveConfiguration with default values")
    void testDefaultConfig() {
        RetrieveConfiguration config = RetrieveConfiguration.defaultConfig();

        assertEquals(5, config.getLimit());
        assertEquals(0.5, config.getScoreThreshold());
        assertFalse(config.isRerankingEnabled());
        assertNull(config.getReranker());
    }

    @Test
    @DisplayName("Should create RetrieveConfiguration with builder")
    void testBuilder() {
        RetrieveConfiguration config =
                RetrieveConfiguration.builder()
                        .limit(10)
                        .scoreThreshold(0.7)
                        .enableReranking(true)
                        .reranker("cross-encoder")
                        .build();

        assertEquals(10, config.getLimit());
        assertEquals(0.7, config.getScoreThreshold());
        assertTrue(config.isRerankingEnabled());
        assertEquals("cross-encoder", config.getReranker());
    }

    @Test
    @DisplayName("Should throw exception for invalid limit")
    void testInvalidLimit() {
        assertThrows(
                IllegalArgumentException.class, () -> RetrieveConfiguration.builder().limit(0));
        assertThrows(
                IllegalArgumentException.class, () -> RetrieveConfiguration.builder().limit(-1));
    }

    @Test
    @DisplayName("Should throw exception for invalid score threshold")
    void testInvalidScoreThreshold() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RetrieveConfiguration.builder().scoreThreshold(-0.1));
        assertThrows(
                IllegalArgumentException.class,
                () -> RetrieveConfiguration.builder().scoreThreshold(1.1));
    }

    @Test
    @DisplayName("Should allow valid score threshold range")
    void testValidScoreThreshold() {
        RetrieveConfiguration config1 = RetrieveConfiguration.builder().scoreThreshold(0.0).build();
        assertEquals(0.0, config1.getScoreThreshold());

        RetrieveConfiguration config2 = RetrieveConfiguration.builder().scoreThreshold(1.0).build();
        assertEquals(1.0, config2.getScoreThreshold());
    }

    @Test
    @DisplayName("Should allow null reranker")
    void testNullReranker() {
        RetrieveConfiguration config = RetrieveConfiguration.builder().reranker(null).build();
        assertNull(config.getReranker());
    }
}
