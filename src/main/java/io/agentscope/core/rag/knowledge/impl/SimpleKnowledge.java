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
package io.agentscope.core.rag.knowledge.impl;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.rag.EmbeddingModel;
import io.agentscope.core.rag.KnowledgeBase;
import io.agentscope.core.rag.VDBStoreBase;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.model.RetrieveConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Simple implementation of a knowledge base.
 *
 * <p>This implementation integrates an embedding model and a vector store to provide
 * a complete RAG (Retrieval-Augmented Generation) knowledge base. It handles the
 * full workflow: document embedding, storage, and retrieval.
 *
 * <p>Workflow:
 * <ul>
 *   <li><b>addDocuments:</b> Embed documents → Store documents (with metadata/payload) in vector store
 *   <li><b>retrieve:</b> Embed query → Search documents → Filter by threshold → Return documents
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * EmbeddingModel embeddingModel = new DashScopeTextEmbedding(apiKey, "text-embedding-v3", 1024);
 * VDBStoreBase vectorStore = new InMemoryStore(1024);
 * KnowledgeBase knowledgeBase = new SimpleKnowledge(embeddingModel, vectorStore);
 *
 * // Add documents
 * List<Document> documents = reader.read(input).block();
 * knowledgeBase.addDocuments(documents).block();
 *
 * // Retrieve documents
 * RetrieveConfig config = RetrieveConfig.builder().limit(5).scoreThreshold(0.5).build();
 * List<Document> results = knowledgeBase.retrieve("query text", config).block();
 * }</pre>
 */
public class SimpleKnowledge implements KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(SimpleKnowledge.class);

    private final EmbeddingModel embeddingModel;
    private final VDBStoreBase embeddingStore;

    /**
     * Creates a new SimpleKnowledge instance.
     *
     * @param embeddingModel the embedding model to use for generating vectors
     * @param embeddingStore the vector store to use for storage and search
     * @throws IllegalArgumentException if any parameter is null
     */
    public SimpleKnowledge(EmbeddingModel embeddingModel, VDBStoreBase embeddingStore) {
        if (embeddingModel == null) {
            throw new IllegalArgumentException("Embedding model cannot be null");
        }
        if (embeddingStore == null) {
            throw new IllegalArgumentException("Embedding store cannot be null");
        }
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public Mono<Void> addDocuments(List<Document> documents) {
        if (documents == null) {
            return Mono.error(new IllegalArgumentException("Documents list cannot be null"));
        }
        if (documents.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(documents)
                .flatMap(
                        doc -> {
                            // Extract ContentBlock from document metadata
                            ContentBlock contentBlock = extractContentBlock(doc.getMetadata());
                            if (contentBlock == null) {
                                log.warn(
                                        "Cannot extract ContentBlock from document: {}",
                                        doc.getId());
                                return Mono.empty();
                            }
                            // Generate embedding for document content
                            return embeddingModel
                                    .embed(contentBlock)
                                    .doOnNext(embedding -> doc.setEmbedding(embedding))
                                    .thenReturn(doc);
                        })
                .collectList()
                .flatMap(
                        docsWithEmbeddings -> {
                            // Batch store documents in vector store (includes metadata/payload)
                            if (docsWithEmbeddings.isEmpty()) {
                                return Mono.empty();
                            }
                            return embeddingStore.add(docsWithEmbeddings);
                        })
                .onErrorResume(
                        error -> {
                            log.error("Failed to add documents to knowledge base", error);
                            return Mono.empty();
                        });
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

        // Convert query string to TextBlock
        TextBlock queryBlock = TextBlock.builder().text(query).build();
        return embeddingModel
                .embed(queryBlock)
                .flatMap(
                        queryEmbedding ->
                                embeddingStore.search(queryEmbedding, config.getLimit(), null))
                .flatMap(
                        results ->
                                Flux.fromIterable(results)
                                        .filter(
                                                doc ->
                                                        doc != null
                                                                && doc.getScore() != null
                                                                && doc.getScore()
                                                                        >= config
                                                                                .getScoreThreshold())
                                        .sort(
                                                Comparator.comparing(
                                                        Document::getScore,
                                                        Comparator.reverseOrder()))
                                        .collectList());
    }

    /**
     * Gets the number of documents currently stored in the knowledge base.
     *
     * <p>Note: This method is not supported by all vector stores. For stores that don't
     * support size queries, this will return -1.
     *
     * @return the number of stored documents, or -1 if not supported
     */
    public int size() {
        // Vector stores typically don't expose size directly
        // This would require additional API support
        return -1;
    }

    /**
     * Checks if the knowledge base is empty.
     *
     * <p>Note: This method is not supported by all vector stores. For stores that don't
     * support emptiness checks, this will return false.
     *
     * @return true if the knowledge base contains no documents, false otherwise
     */
    public boolean isEmpty() {
        // Vector stores typically don't expose emptiness checks directly
        // This would require additional API support
        return false;
    }

    /**
     * Clears all documents from the knowledge base.
     *
     * <p>Note: This method is not supported by all vector stores. For stores that don't
     * support clearing, this will do nothing.
     */
    public void clear() {
        // Vector stores typically don't support clearing directly
        // This would require additional API support or manual deletion
        log.warn("clear() is not supported for vector stores");
    }

    /**
     * Gets the embedding model used by this knowledge base.
     *
     * @return the embedding model
     */
    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     * Gets the vector store used by this knowledge base.
     *
     * @return the vector store
     */
    public VDBStoreBase getEmbeddingStore() {
        return embeddingStore;
    }

    /**
     * Extracts a ContentBlock from DocumentMetadata.
     *
     * <p>Since DocumentMetadata now directly stores ContentBlock, this method simply returns it.
     *
     * @param metadata the document metadata
     * @return ContentBlock from metadata, or null if not available
     */
    private ContentBlock extractContentBlock(DocumentMetadata metadata) {
        return metadata.getContent();
    }
}
