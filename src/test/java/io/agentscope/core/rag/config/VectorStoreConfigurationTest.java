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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for VectorStoreConfiguration.
 */
@Tag("unit")
@DisplayName("VectorStoreConfiguration Unit Tests")
class VectorStoreConfigurationTest {

    @Test
    @DisplayName("Should create VectorStoreConfiguration with default values")
    void testDefaultConfig() {
        VectorStoreConfiguration config = VectorStoreConfiguration.defaultConfig();

        assertEquals("memory", config.getProvider());
        assertEquals(":memory:", config.getLocation());
        assertEquals("agentscope_rag", config.getCollectionName());
        assertEquals(1024, config.getDimensions());
        assertEquals(10, config.getSearchLimit());
        assertEquals(0.5, config.getScoreThreshold());
        assertTrue(config.getAdditionalConfig().isEmpty());
    }

    @Test
    @DisplayName("Should create VectorStoreConfiguration with builder")
    void testBuilder() {
        VectorStoreConfiguration config =
                VectorStoreConfiguration.builder()
                        .provider("qdrant")
                        .location("./data/qdrant")
                        .collectionName("my_collection")
                        .dimensions(1536)
                        .searchLimit(20)
                        .scoreThreshold(0.7)
                        .additionalConfig("key1", "value1")
                        .additionalConfig("key2", 42)
                        .build();

        assertEquals("qdrant", config.getProvider());
        assertEquals("./data/qdrant", config.getLocation());
        assertEquals("my_collection", config.getCollectionName());
        assertEquals(1536, config.getDimensions());
        assertEquals(20, config.getSearchLimit());
        assertEquals(0.7, config.getScoreThreshold());
        assertEquals(2, config.getAdditionalConfig().size());
        assertEquals("value1", config.getAdditionalConfig().get("key1"));
        assertEquals(42, config.getAdditionalConfig().get("key2"));
    }

    @Test
    @DisplayName("Should throw exception for null provider")
    void testNullProvider() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VectorStoreConfiguration.builder().provider(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> VectorStoreConfiguration.builder().provider(""));
    }

    @Test
    @DisplayName("Should throw exception for invalid dimensions")
    void testInvalidDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VectorStoreConfiguration.builder().dimensions(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> VectorStoreConfiguration.builder().dimensions(-1));
    }

    @Test
    @DisplayName("Should throw exception for invalid search limit")
    void testInvalidSearchLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VectorStoreConfiguration.builder().searchLimit(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> VectorStoreConfiguration.builder().searchLimit(-1));
    }

    @Test
    @DisplayName("Should throw exception for invalid score threshold")
    void testInvalidScoreThreshold() {
        assertThrows(
                IllegalArgumentException.class,
                () -> VectorStoreConfiguration.builder().scoreThreshold(-0.1));
        assertThrows(
                IllegalArgumentException.class,
                () -> VectorStoreConfiguration.builder().scoreThreshold(1.1));
    }

    @Test
    @DisplayName("Should allow valid score threshold range")
    void testValidScoreThreshold() {
        VectorStoreConfiguration config1 =
                VectorStoreConfiguration.builder().scoreThreshold(0.0).build();
        assertEquals(0.0, config1.getScoreThreshold());

        VectorStoreConfiguration config2 =
                VectorStoreConfiguration.builder().scoreThreshold(1.0).build();
        assertEquals(1.0, config2.getScoreThreshold());
    }

    @Test
    @DisplayName("Should handle null location")
    void testNullLocation() {
        VectorStoreConfiguration config = VectorStoreConfiguration.builder().location(null).build();
        assertEquals(":memory:", config.getLocation());
    }

    @Test
    @DisplayName("Should handle null collection name")
    void testNullCollectionName() {
        VectorStoreConfiguration config =
                VectorStoreConfiguration.builder().collectionName(null).build();
        assertEquals("agentscope_rag", config.getCollectionName());
    }

    @Test
    @DisplayName("Should set additional config from map")
    void testAdditionalConfigMap() {
        Map<String, Object> configMap = Map.of("key1", "value1", "key2", 42);
        VectorStoreConfiguration config =
                VectorStoreConfiguration.builder().additionalConfig(configMap).build();

        assertEquals(2, config.getAdditionalConfig().size());
        assertEquals("value1", config.getAdditionalConfig().get("key1"));
        assertEquals(42, config.getAdditionalConfig().get("key2"));
    }

    @Test
    @DisplayName("Should return unmodifiable additional config")
    void testUnmodifiableAdditionalConfig() {
        VectorStoreConfiguration config =
                VectorStoreConfiguration.builder().additionalConfig("key", "value").build();

        assertThrows(
                UnsupportedOperationException.class,
                () -> config.getAdditionalConfig().put("newKey", "newValue"));
    }
}
