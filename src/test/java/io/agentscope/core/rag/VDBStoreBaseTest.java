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
package io.agentscope.core.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.rag.model.VectorSearchResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for VDBStoreBase interface.
 *
 * <p>Tests the VDBStoreBase interface, including default methods.
 */
@Tag("unit")
@DisplayName("VDBStoreBase Interface Unit Tests")
class VDBStoreBaseTest {

    /**
     * Simple test implementation of VDBStoreBase interface.
     */
    static class TestVDBStore implements VDBStoreBase {
        @Override
        public Mono<String> add(String id, double[] embedding) {
            return Mono.just(id);
        }

        @Override
        public Mono<List<VectorSearchResult>> search(double[] queryEmbedding, int topK) {
            return Mono.just(List.of());
        }
    }

    @Test
    @DisplayName("Should return empty Mono for getClient by default")
    void testGetClientDefault() {
        VDBStoreBase store = new TestVDBStore();

        StepVerifier.create(store.getClient()).verifyComplete();
    }

    @Test
    @DisplayName("Should return true for delete by default")
    void testDeleteDefault() {
        VDBStoreBase store = new TestVDBStore();

        StepVerifier.create(store.delete("test-id"))
                .assertNext(result -> assertTrue(result))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should add vector and return id")
    void testAdd() {
        VDBStoreBase store = new TestVDBStore();
        double[] embedding = new double[] {0.1, 0.2, 0.3};

        StepVerifier.create(store.add("test-id", embedding))
                .assertNext(id -> assertEquals("test-id", id))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should search vectors and return results")
    void testSearch() {
        VDBStoreBase store = new TestVDBStore();
        double[] queryEmbedding = new double[] {0.1, 0.2, 0.3};

        StepVerifier.create(store.search(queryEmbedding, 10))
                .assertNext(
                        results -> {
                            assertTrue(results instanceof List);
                        })
                .verifyComplete();
    }
}
