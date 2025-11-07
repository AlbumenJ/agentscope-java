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
package io.agentscope.core.rag.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for VectorSearchResult.
 */
@Tag("unit")
@DisplayName("VectorSearchResult Unit Tests")
class VectorSearchResultTest {

    @Test
    @DisplayName("Should create VectorSearchResult with id and score")
    void testCreateResult() {
        String id = "doc-1";
        double score = 0.95;

        VectorSearchResult result = new VectorSearchResult(id, score);

        assertEquals(id, result.getId());
        assertEquals(score, result.getScore());
    }

    @Test
    @DisplayName("Should throw exception when id is null")
    void testCreateResultNullId() {
        assertThrows(IllegalArgumentException.class, () -> new VectorSearchResult(null, 0.95));
    }

    @Test
    @DisplayName("Should handle zero score")
    void testZeroScore() {
        VectorSearchResult result = new VectorSearchResult("doc-1", 0.0);
        assertEquals(0.0, result.getScore());
    }

    @Test
    @DisplayName("Should handle negative score")
    void testNegativeScore() {
        VectorSearchResult result = new VectorSearchResult("doc-1", -0.5);
        assertEquals(-0.5, result.getScore());
    }

    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        VectorSearchResult result = new VectorSearchResult("doc-1", 0.95);
        String str = result.toString();

        assertEquals(true, str.contains("VectorSearchResult"));
        assertEquals(true, str.contains("doc-1"));
        assertEquals(true, str.contains("0.950"));
    }
}
