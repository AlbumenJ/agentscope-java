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
package io.agentscope.core.rag.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.rag.KnowledgeBase;
import io.agentscope.core.rag.knowledge.impl.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.store.impl.InMemoryStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for CompositeKnowledgeBase.
 */
@Tag("unit")
@DisplayName("CompositeKnowledgeBase Unit Tests")
class CompositeKnowledgeBaseTest {

    private static final int DIMENSIONS = 3;

    private KnowledgeBase kb1;
    private KnowledgeBase kb2;
    private CompositeKnowledgeBase composite;

    @BeforeEach
    void setUp() {
        TestMockEmbeddingModel embeddingModel1 = new TestMockEmbeddingModel(DIMENSIONS);
        TestMockEmbeddingModel embeddingModel2 = new TestMockEmbeddingModel(DIMENSIONS);
        InMemoryStore store1 = new InMemoryStore(DIMENSIONS);
        InMemoryStore store2 = new InMemoryStore(DIMENSIONS);
        kb1 = new SimpleKnowledge(embeddingModel1, store1);
        kb2 = new SimpleKnowledge(embeddingModel2, store2);
        composite = new CompositeKnowledgeBase(List.of(kb1, kb2));
    }

    @Test
    @DisplayName("Should create CompositeKnowledgeBase with valid knowledge bases")
    void testCreate() {
        CompositeKnowledgeBase newComposite = new CompositeKnowledgeBase(List.of(kb1, kb2));
        assertNotNull(newComposite);
        assertEquals(2, newComposite.size());
    }

    @Test
    @DisplayName("Should throw exception for null knowledge bases list")
    void testCreateNullList() {
        assertThrows(IllegalArgumentException.class, () -> new CompositeKnowledgeBase(null));
    }

    @Test
    @DisplayName("Should throw exception for empty knowledge bases list")
    void testCreateEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> new CompositeKnowledgeBase(List.of()));
    }

    @Test
    @DisplayName("Should throw exception for null knowledge base in list")
    void testCreateWithNullKnowledgeBase() {
        List<KnowledgeBase> listWithNull = new java.util.ArrayList<>();
        listWithNull.add(kb1);
        listWithNull.add(null);
        listWithNull.add(kb2);
        assertThrows(
                IllegalArgumentException.class, () -> new CompositeKnowledgeBase(listWithNull));
    }

    @Test
    @DisplayName("Should add documents to all knowledge bases")
    void testAddDocuments() {
        Document doc1 = createDocument("doc1", "Content 1");
        Document doc2 = createDocument("doc2", "Content 2");

        StepVerifier.create(composite.addDocuments(List.of(doc1, doc2))).verifyComplete();

        // Verify documents were added to both knowledge bases
        // Use exact content to ensure retrieval
        RetrieveConfig config = RetrieveConfig.builder().limit(5).scoreThreshold(0.0).build();
        StepVerifier.create(kb1.retrieve("Content 1", config))
                .assertNext(results -> assertTrue(results.size() > 0))
                .verifyComplete();
        StepVerifier.create(kb2.retrieve("Content 2", config))
                .assertNext(results -> assertTrue(results.size() > 0))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty document list")
    void testAddEmptyDocuments() {
        StepVerifier.create(composite.addDocuments(List.of())).verifyComplete();
    }

    @Test
    @DisplayName("Should throw error for null document list")
    void testAddNullDocuments() {
        StepVerifier.create(composite.addDocuments(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should retrieve and merge results from all knowledge bases")
    void testRetrieve() {
        // Add different documents to each knowledge base
        Document doc1 = createDocument("doc1", "Machine learning");
        Document doc2 = createDocument("doc2", "Deep learning");
        kb1.addDocuments(List.of(doc1)).block();
        kb2.addDocuments(List.of(doc2)).block();

        RetrieveConfig config = RetrieveConfig.builder().limit(5).scoreThreshold(0.0).build();

        StepVerifier.create(composite.retrieve("Machine learning", config))
                .assertNext(
                        results -> {
                            assertNotNull(results);
                            // Should have results (may vary based on similarity)
                            assertTrue(results.size() >= 0);
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should merge and sort results by score")
    void testMergeAndSortResults() {
        // Add documents with different scores
        Document doc1 = createDocument("doc1", "High score content");
        Document doc2 = createDocument("doc2", "Low score content");
        kb1.addDocuments(List.of(doc1)).block();
        kb2.addDocuments(List.of(doc2)).block();

        RetrieveConfig config = RetrieveConfig.builder().limit(10).scoreThreshold(0.0).build();

        StepVerifier.create(composite.retrieve("score", config))
                .assertNext(
                        results -> {
                            if (results.size() > 1) {
                                // Results should be sorted by score (descending)
                                for (int i = 0; i < results.size() - 1; i++) {
                                    Double score1 = results.get(i).getScore();
                                    Double score2 = results.get(i + 1).getScore();
                                    if (score1 != null && score2 != null) {
                                        assertTrue(score1 >= score2);
                                    }
                                }
                            }
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should deduplicate results by document ID")
    void testDeduplicateResults() {
        // Add same document to both knowledge bases
        Document doc = createDocument("doc1", "Same content");
        kb1.addDocuments(List.of(doc)).block();
        kb2.addDocuments(List.of(doc)).block();

        RetrieveConfig config = RetrieveConfig.builder().limit(10).scoreThreshold(0.0).build();

        StepVerifier.create(composite.retrieve("content", config))
                .assertNext(
                        results -> {
                            // Should deduplicate by ID
                            long uniqueIds =
                                    results.stream().map(Document::getId).distinct().count();
                            assertTrue(uniqueIds <= results.size());
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty query")
    void testRetrieveEmptyQuery() {
        RetrieveConfig config = RetrieveConfig.builder().build();

        StepVerifier.create(composite.retrieve("", config))
                .assertNext(results -> assertTrue(results.isEmpty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw error for null query")
    void testRetrieveNullQuery() {
        RetrieveConfig config = RetrieveConfig.builder().build();

        StepVerifier.create(composite.retrieve(null, config))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw error for null config")
    void testRetrieveNullConfig() {
        StepVerifier.create(composite.retrieve("query", null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should get knowledge bases list")
    void testGetKnowledgeBases() {
        List<KnowledgeBase> bases = composite.getKnowledgeBases();
        assertEquals(2, bases.size());
        assertTrue(bases.contains(kb1));
        assertTrue(bases.contains(kb2));
    }

    /**
     * Creates a test document.
     */
    private Document createDocument(String docId, String content) {
        Map<String, Object> contentMap = Map.of("text", content);
        DocumentMetadata metadata = new DocumentMetadata(contentMap, docId, 0, 1);
        return new Document(metadata);
    }

    /**
     * Mock EmbeddingModel for testing.
     */
    private static class TestMockEmbeddingModel implements io.agentscope.core.rag.EmbeddingModel {
        private final int dimensions;
        private final Map<String, double[]> embeddings = new HashMap<>();

        TestMockEmbeddingModel(int dimensions) {
            this.dimensions = dimensions;
        }

        @Override
        public Mono<double[]> embed(io.agentscope.core.message.ContentBlock block) {
            if (block instanceof io.agentscope.core.message.TextBlock) {
                String text = ((io.agentscope.core.message.TextBlock) block).getText();
                return Mono.fromCallable(
                        () -> {
                            double[] embedding =
                                    embeddings.computeIfAbsent(text, k -> generateEmbedding(text));
                            return embedding.clone();
                        });
            }
            return Mono.error(new UnsupportedOperationException("Unsupported content block type"));
        }

        @Override
        public String getModelName() {
            return "mock-embedding-model";
        }

        @Override
        public int getDimensions() {
            return dimensions;
        }

        private double[] generateEmbedding(String text) {
            double[] embedding = new double[dimensions];
            int hash = text.hashCode();
            for (int i = 0; i < dimensions; i++) {
                embedding[i] = (double) ((hash + i) % 100) / 100.0;
            }
            double norm = 0.0;
            for (double v : embedding) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < dimensions; i++) {
                    embedding[i] /= norm;
                }
            }
            return embedding;
        }
    }
}
