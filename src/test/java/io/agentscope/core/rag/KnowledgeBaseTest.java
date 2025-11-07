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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for KnowledgeBase interface.
 *
 * <p>Tests the KnowledgeBase interface using a simple test implementation.
 */
@Tag("unit")
@DisplayName("KnowledgeBase Interface Unit Tests")
class KnowledgeBaseTest {

    /**
     * Simple test implementation of KnowledgeBase interface.
     */
    static class TestKnowledgeBase implements KnowledgeBase {
        @Override
        public Mono<Void> addDocuments(List<Document> documents) {
            return Mono.empty();
        }

        @Override
        public Mono<List<Document>> retrieve(String query, RetrieveConfig config) {
            return Mono.just(List.of());
        }
    }

    @Test
    @DisplayName("Should add documents")
    void testAddDocuments() {
        KnowledgeBase knowledgeBase = new TestKnowledgeBase();
        List<Document> documents = List.of();

        StepVerifier.create(knowledgeBase.addDocuments(documents)).verifyComplete();
    }

    @Test
    @DisplayName("Should retrieve documents")
    void testRetrieve() {
        KnowledgeBase knowledgeBase = new TestKnowledgeBase();
        RetrieveConfig config = RetrieveConfig.builder().build();

        StepVerifier.create(knowledgeBase.retrieve("test query", config))
                .assertNext(
                        results -> {
                            assertNotNull(results);
                        })
                .verifyComplete();
    }
}
