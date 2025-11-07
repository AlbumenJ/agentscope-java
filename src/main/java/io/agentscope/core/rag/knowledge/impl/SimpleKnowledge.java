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
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.URLSource;
import io.agentscope.core.rag.EmbeddingModel;
import io.agentscope.core.rag.KnowledgeBase;
import io.agentscope.core.rag.VDBStoreBase;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.model.VectorSearchResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 *   <li><b>addDocuments:</b> Embed documents → Store vectors → Cache documents
 *   <li><b>retrieve:</b> Embed query → Search vectors → Filter by threshold → Return documents
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
    private final Map<String, Document> documentCache;

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
        this.documentCache = new ConcurrentHashMap<>();
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
                .flatMap(
                        doc -> {
                            // Store vector in vector store
                            return embeddingStore
                                    .add(doc.getId(), doc.getEmbedding())
                                    .doOnSuccess(
                                            id -> {
                                                // Cache document for later retrieval
                                                documentCache.put(id, doc);
                                            })
                                    .thenReturn(doc);
                        })
                .onErrorResume(
                        error -> {
                            log.error("Failed to add documents to knowledge base", error);
                            // Continue processing other documents
                            return Mono.empty();
                        })
                .then();
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
                .flatMap(queryEmbedding -> embeddingStore.search(queryEmbedding, config.getLimit()))
                .flatMap(
                        results ->
                                Flux.fromIterable(results)
                                        .map(result -> createDocumentFromResult(result, config))
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
     * Creates a Document from a VectorSearchResult.
     *
     * <p>Retrieves the original document from cache and sets the similarity score.
     *
     * @param result the vector search result
     * @param config the retrieval config (for potential future use)
     * @return a Document with the score set, or null if document not found in cache
     */
    private Document createDocumentFromResult(VectorSearchResult result, RetrieveConfig config) {
        Document originalDoc = documentCache.get(result.getId());
        if (originalDoc == null) {
            log.warn("Document not found in cache for ID: {}", result.getId());
            return null;
        }

        // Create a copy with the score set
        // Note: Document is immutable in terms of ID and metadata, but we can set score
        Document docWithScore = new Document(originalDoc.getMetadata());
        docWithScore.setEmbedding(originalDoc.getEmbedding());
        docWithScore.setScore(result.getScore());

        return docWithScore;
    }

    /**
     * Gets the number of documents currently stored in the knowledge base.
     *
     * @return the number of stored documents
     */
    public int size() {
        return documentCache.size();
    }

    /**
     * Checks if the knowledge base is empty.
     *
     * @return true if the knowledge base contains no documents
     */
    public boolean isEmpty() {
        return documentCache.isEmpty();
    }

    /**
     * Clears all documents from the knowledge base.
     *
     * <p>Note: This only clears the document cache. The vector store may still contain
     * vectors. For a complete cleanup, you may need to clear the vector store separately.
     */
    public void clear() {
        documentCache.clear();
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
     * Extracts a ContentBlock from DocumentMetadata's content map.
     *
     * <p>Supports:
     * <ul>
     *   <li>Text content: content map with "text" key → TextBlock
     *   <li>Image content: content map with "type": "image" and "source" → ImageBlock
     * </ul>
     *
     * @param metadata the document metadata
     * @return ContentBlock extracted from metadata, or null if extraction fails
     */
    private ContentBlock extractContentBlock(DocumentMetadata metadata) {
        Map<String, Object> content = metadata.getContent();
        if (content == null) {
            return null;
        }

        // Check for image content
        Object typeObj = content.get("type");
        if (typeObj != null && "image".equals(typeObj.toString())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sourceMap = (Map<String, Object>) content.get("source");
            if (sourceMap != null) {
                Object sourceTypeObj = sourceMap.get("type");
                if (sourceTypeObj != null) {
                    String sourceType = sourceTypeObj.toString();
                    if ("url".equals(sourceType)) {
                        Object urlObj = sourceMap.get("url");
                        if (urlObj != null) {
                            URLSource source = URLSource.builder().url(urlObj.toString()).build();
                            return ImageBlock.builder().source(source).build();
                        }
                    } else if ("file".equals(sourceType)) {
                        Object pathObj = sourceMap.get("path");
                        if (pathObj != null) {
                            URLSource source = URLSource.builder().url(pathObj.toString()).build();
                            return ImageBlock.builder().source(source).build();
                        }
                    }
                }
            }
        }

        // Default to text content
        String text = metadata.getContentText();
        if (text != null && !text.trim().isEmpty()) {
            return TextBlock.builder().text(text).build();
        }

        return null;
    }
}
