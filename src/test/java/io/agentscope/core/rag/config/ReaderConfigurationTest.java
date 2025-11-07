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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.rag.reader.impl.SplitStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ReaderConfiguration.
 */
@Tag("unit")
@DisplayName("ReaderConfiguration Unit Tests")
class ReaderConfigurationTest {

    @Test
    @DisplayName("Should create ReaderConfiguration with default values")
    void testDefaultConfig() {
        ReaderConfiguration config = ReaderConfiguration.defaultConfig();

        assertEquals(512, config.getDefaultChunkSize());
        assertEquals(SplitStrategy.PARAGRAPH, config.getDefaultSplitStrategy());
        assertEquals(50, config.getDefaultOverlapSize());
        assertFalse(config.isImageOCREnabled());
    }

    @Test
    @DisplayName("Should create ReaderConfiguration with builder")
    void testBuilder() {
        ReaderConfiguration config =
                ReaderConfiguration.builder()
                        .chunkSize(1024)
                        .splitStrategy(SplitStrategy.TOKEN)
                        .overlapSize(100)
                        .enableImageOCR(true)
                        .build();

        assertEquals(1024, config.getDefaultChunkSize());
        assertEquals(SplitStrategy.TOKEN, config.getDefaultSplitStrategy());
        assertEquals(100, config.getDefaultOverlapSize());
        assertTrue(config.isImageOCREnabled());
    }

    @Test
    @DisplayName("Should throw exception for invalid chunk size")
    void testInvalidChunkSize() {
        assertThrows(
                IllegalArgumentException.class, () -> ReaderConfiguration.builder().chunkSize(0));
        assertThrows(
                IllegalArgumentException.class, () -> ReaderConfiguration.builder().chunkSize(-1));
    }

    @Test
    @DisplayName("Should throw exception for null split strategy")
    void testNullSplitStrategy() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ReaderConfiguration.builder().splitStrategy(null));
    }

    @Test
    @DisplayName("Should throw exception for negative overlap size")
    void testNegativeOverlapSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ReaderConfiguration.builder().overlapSize(-1));
    }

    @Test
    @DisplayName("Should allow zero overlap size")
    void testZeroOverlapSize() {
        ReaderConfiguration config = ReaderConfiguration.builder().overlapSize(0).build();
        assertEquals(0, config.getDefaultOverlapSize());
    }

    @Test
    @DisplayName("Should support all split strategies")
    void testAllSplitStrategies() {
        for (SplitStrategy strategy : SplitStrategy.values()) {
            ReaderConfiguration config =
                    ReaderConfiguration.builder().splitStrategy(strategy).build();
            assertEquals(strategy, config.getDefaultSplitStrategy());
        }
    }
}
