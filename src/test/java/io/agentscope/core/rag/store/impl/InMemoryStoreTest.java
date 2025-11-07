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
package io.agentscope.core.rag.store.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.rag.model.VectorSearchResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/**
 * Unit tests for InMemoryStore.
 */
@Tag("unit")
@DisplayName("InMemoryStore Unit Tests")
class InMemoryStoreTest {

    private InMemoryStore store;
    private static final int DIMENSIONS = 3;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore(DIMENSIONS);
    }

    @Test
    @DisplayName("Should create store with specified dimensions")
    void testCreateStore() {
        InMemoryStore newStore = new InMemoryStore(1024);
        assertEquals(1024, newStore.getDimensions());
        assertTrue(newStore.isEmpty());
    }

    @Test
    @DisplayName("Should create store with default dimensions")
    void testCreateStoreDefault() {
        InMemoryStore defaultStore = new InMemoryStore();
        assertEquals(1024, defaultStore.getDimensions());
    }

    @Test
    @DisplayName("Should throw exception for invalid dimensions")
    void testCreateStoreInvalidDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryStore(0));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryStore(-1));
    }

    @Test
    @DisplayName("Should add vector to store")
    void testAdd() {
        String id = "doc-1";
        double[] embedding = {1.0, 2.0, 3.0};

        StepVerifier.create(store.add(id, embedding))
                .assertNext(returnedId -> assertEquals(id, returnedId))
                .verifyComplete();

        assertEquals(1, store.size());
    }

    @Test
    @DisplayName("Should throw error when adding null ID")
    void testAddNullId() {
        double[] embedding = {1.0, 2.0, 3.0};

        StepVerifier.create(store.add(null, embedding))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw error when adding null embedding")
    void testAddNullEmbedding() {
        StepVerifier.create(store.add("doc-1", null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw error when embedding dimension mismatch")
    void testAddDimensionMismatch() {
        double[] embedding = {1.0, 2.0}; // Wrong dimension

        StepVerifier.create(store.add("doc-1", embedding))
                .expectError(io.agentscope.core.rag.model.VectorStoreException.class)
                .verify();
    }

    @Test
    @DisplayName("Should replace existing vector with same ID")
    void testAddReplace() {
        String id = "doc-1";
        double[] embedding1 = {1.0, 2.0, 3.0};
        double[] embedding2 = {4.0, 5.0, 6.0};

        store.add(id, embedding1).block();
        store.add(id, embedding2).block();

        assertEquals(1, store.size());
    }

    @Test
    @DisplayName("Should search for similar vectors")
    void testSearch() {
        // Add some vectors
        double[] v1 = {1.0, 0.0, 0.0};
        double[] v2 = {0.0, 1.0, 0.0};
        double[] v3 = {0.0, 0.0, 1.0};

        store.add("doc-1", v1).block();
        store.add("doc-2", v2).block();
        store.add("doc-3", v3).block();

        // Search for vector similar to v1
        double[] query = {1.0, 0.0, 0.0};

        StepVerifier.create(store.search(query, 2))
                .assertNext(
                        results -> {
                            assertEquals(2, results.size());
                            // First result should be doc-1 (identical, similarity = 1.0)
                            assertEquals("doc-1", results.get(0).getId());
                            assertEquals(1.0, results.get(0).getScore(), 1e-9);
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list when store is empty")
    void testSearchEmptyStore() {
        double[] query = {1.0, 2.0, 3.0};

        StepVerifier.create(store.search(query, 5))
                .assertNext(results -> assertTrue(results.isEmpty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return top K results")
    void testSearchTopK() {
        // Add 5 vectors
        for (int i = 0; i < 5; i++) {
            double[] embedding = {(double) i, 0.0, 0.0};
            store.add("doc-" + i, embedding).block();
        }

        double[] query = {0.0, 0.0, 0.0};

        StepVerifier.create(store.search(query, 3))
                .assertNext(results -> assertEquals(3, results.size()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return results sorted by similarity")
    void testSearchSorted() {
        double[] v1 = {1.0, 0.0, 0.0}; // Most similar to query
        double[] v2 = {0.5, 0.5, 0.0}; // Less similar
        double[] v3 = {0.0, 1.0, 0.0}; // Least similar

        store.add("doc-1", v1).block();
        store.add("doc-2", v2).block();
        store.add("doc-3", v3).block();

        double[] query = {1.0, 0.0, 0.0};

        StepVerifier.create(store.search(query, 3))
                .assertNext(
                        results -> {
                            assertEquals(3, results.size());
                            // Results should be sorted by similarity (descending)
                            assertTrue(results.get(0).getScore() >= results.get(1).getScore());
                            assertTrue(results.get(1).getScore() >= results.get(2).getScore());
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw error when searching with null query")
    void testSearchNullQuery() {
        StepVerifier.create(store.search(null, 5))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw error when searching with invalid topK")
    void testSearchInvalidTopK() {
        double[] query = {1.0, 2.0, 3.0};

        StepVerifier.create(store.search(query, 0))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(store.search(query, -1))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should delete vector from store")
    void testDelete() {
        String id = "doc-1";
        double[] embedding = {1.0, 2.0, 3.0};

        store.add(id, embedding).block();
        assertEquals(1, store.size());

        StepVerifier.create(store.delete(id))
                .assertNext(deleted -> assertTrue(deleted))
                .verifyComplete();

        assertEquals(0, store.size());
    }

    @Test
    @DisplayName("Should return false when deleting non-existent vector")
    void testDeleteNonExistent() {
        StepVerifier.create(store.delete("non-existent"))
                .assertNext(deleted -> assertFalse(deleted))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw error when deleting with null ID")
    void testDeleteNullId() {
        StepVerifier.create(store.delete(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should clear all vectors")
    void testClear() {
        store.add("doc-1", new double[] {1.0, 2.0, 3.0}).block();
        store.add("doc-2", new double[] {4.0, 5.0, 6.0}).block();

        assertEquals(2, store.size());

        store.clear();

        assertEquals(0, store.size());
        assertTrue(store.isEmpty());
    }

    @Test
    @DisplayName("Should maintain thread safety")
    void testThreadSafety() throws InterruptedException {
        int numThreads = 10;
        int vectorsPerThread = 10;
        Thread[] threads = new Thread[numThreads];

        // Concurrent adds
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] =
                    new Thread(
                            () -> {
                                for (int j = 0; j < vectorsPerThread; j++) {
                                    String id = "doc-" + threadId + "-" + j;
                                    double[] embedding = {(double) threadId, (double) j, 0.0};
                                    store.add(id, embedding).block();
                                }
                            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(numThreads * vectorsPerThread, store.size());
    }

    @Test
    @DisplayName("Should not modify original embedding array")
    void testImmutableEmbedding() {
        String id = "doc-1";
        double[] original = {1.0, 2.0, 3.0};

        store.add(id, original).block();

        // Modify original array
        original[0] = 999.0;

        // Search should still use the original values (defensive copy was made)
        double[] query = {1.0, 2.0, 3.0};
        List<VectorSearchResult> results = store.search(query, 1).block();

        assertNotNull(results);
        assertEquals(1, results.size());
        // The stored vector should not be affected by the modification
        // We verify this by checking the search result is still similar to the original query
        assertTrue(results.get(0).getScore() > 0.9);
    }
}
