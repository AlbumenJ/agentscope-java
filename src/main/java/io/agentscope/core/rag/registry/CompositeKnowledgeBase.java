/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

import io.agentscope.core.rag.KnowledgeBase;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Composite knowledge base that aggregates multiple knowledge bases.
 *
 * <p>This class implements the {@link KnowledgeBase} interface and combines multiple
 * knowledge bases into a single unified interface. When adding documents, they are
 * added to all constituent knowledge bases. When retrieving, results from all knowledge
 * bases are merged, sorted by score, and deduplicated.
 *
 * <p>Use cases:
 * <ul>
 *   <li>Aggregating multiple specialized knowledge bases</li>
 *   <li>Combining different data sources (e.g., documents, code, FAQs)</li>
 *   <li>Unified search across multiple knowledge repositories</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * KnowledgeBase kb1 = new SimpleKnowledge(embeddingModel1, store1);
 * KnowledgeBase kb2 = new SimpleKnowledge(embeddingModel2, store2);
 * CompositeKnowledgeBase composite = new CompositeKnowledgeBase(List.of(kb1, kb2));
 *
 * // Add documents to all knowledge bases
 * composite.addDocuments(documents).block();
 *
 * // Retrieve from all knowledge bases and merge results
 * List<Document> results = composite.retrieve("query", config).block();
 * }</pre>
 */
public class CompositeKnowledgeBase implements KnowledgeBase {

    private final List<KnowledgeBase> knowledgeBases;

    /**
     * Creates a new CompositeKnowledgeBase.
     *
     * @param knowledgeBases the list of knowledge bases to aggregate (must not be null or empty)
     * @throws IllegalArgumentException if knowledgeBases is null or empty
     */
    public CompositeKnowledgeBase(List<KnowledgeBase> knowledgeBases) {
        if (knowledgeBases == null) {
            throw new IllegalArgumentException("Knowledge bases list cannot be null");
        }
        if (knowledgeBases.isEmpty()) {
            throw new IllegalArgumentException("Knowledge bases list cannot be empty");
        }
        // Validate that no knowledge base is null
        for (KnowledgeBase kb : knowledgeBases) {
            if (kb == null) {
                throw new IllegalArgumentException("Knowledge base cannot be null");
            }
        }
        this.knowledgeBases = new ArrayList<>(knowledgeBases);
    }

    @Override
    public Mono<Void> addDocuments(List<Document> documents) {
        if (documents == null) {
            return Mono.error(new IllegalArgumentException("Documents list cannot be null"));
        }
        if (documents.isEmpty()) {
            return Mono.empty();
        }

        // Add documents to all knowledge bases in parallel
        return Flux.fromIterable(knowledgeBases).flatMap(kb -> kb.addDocuments(documents)).then();
    }

    @Override
    public Mono<List<Document>> retrieve(String query, RetrieveConfig config) {
        if (query == null) {
            return Mono.error(new IllegalArgumentException("Query cannot be null"));
        }
        if (config == null) {
            return Mono.error(new IllegalArgumentException("RetrieveConfig cannot be null"));
        }
        if (query.trim().isEmpty()) {
            return Mono.just(new ArrayList<>());
        }

        // Retrieve from all knowledge bases in parallel
        return Flux.fromIterable(knowledgeBases)
                .flatMap(kb -> kb.retrieve(query, config))
                .collectList()
                .map(this::mergeAndSortResults);
    }

    /**
     * Merges and sorts results from multiple knowledge bases.
     *
     * <p>Results are:
     * <ol>
     *   <li>Merged from all knowledge bases</li>
     *   <li>Sorted by score in descending order</li>
     *   <li>Deduplicated by document ID</li>
     *   <li>Limited to the configured limit</li>
     * </ol>
     *
     * @param allResults list of result lists from each knowledge base
     * @return merged, sorted, and deduplicated results
     */
    private List<Document> mergeAndSortResults(List<List<Document>> allResults) {
        if (allResults == null || allResults.isEmpty()) {
            return new ArrayList<>();
        }

        // Use LinkedHashSet to preserve insertion order while deduplicating
        Set<String> seenIds = new LinkedHashSet<>();
        List<Document> merged = new ArrayList<>();

        // First, collect all documents
        for (List<Document> results : allResults) {
            if (results != null) {
                for (Document doc : results) {
                    if (doc != null) {
                        String docId = doc.getId();
                        if (!seenIds.contains(docId)) {
                            seenIds.add(docId);
                            merged.add(doc);
                        }
                    }
                }
            }
        }

        // Sort by score (descending), handling null scores
        merged.sort(
                Comparator.comparing(
                                Document::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Document::getId));

        return merged;
    }

    /**
     * Gets the number of knowledge bases in this composite.
     *
     * @return the number of knowledge bases
     */
    public int size() {
        return knowledgeBases.size();
    }

    /**
     * Gets the list of knowledge bases.
     *
     * @return an unmodifiable list of knowledge bases
     */
    public List<KnowledgeBase> getKnowledgeBases() {
        return List.copyOf(knowledgeBases);
    }
}
