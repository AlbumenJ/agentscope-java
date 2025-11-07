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
package io.agentscope.core.rag.store.impl;

import io.agentscope.core.rag.VDBStoreBase;
import io.agentscope.core.rag.model.VectorSearchResult;
import io.agentscope.core.rag.model.VectorStoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

/**
 * In-memory implementation of vector database storage.
 *
 * <p>This implementation stores vectors in memory using a ConcurrentHashMap for thread safety.
 * It uses cosine similarity for vector search, which is the most common metric for embedding
 * vectors.
 *
 * <p>This implementation is suitable for:
 * <ul>
 *   <li>Development and testing
 *   <li>Small to medium-sized datasets
 *   <li>Prototyping RAG systems
 * </ul>
 *
 * <p>For production use with large datasets, consider using a dedicated vector database
 * like Qdrant, ChromaDB, or Pinecone.
 *
 * <p><b>Exception Handling:</b>
 * <ul>
 *   <li>{@link IllegalArgumentException} - for invalid input parameters (null IDs, null embeddings, invalid topK)
 *   <li>{@link VectorStoreException} - for vector-specific errors (dimension mismatch)
 * </ul>
 */
public class InMemoryStore implements VDBStoreBase {

    private final Map<String, double[]> vectors;
    private final int dimensions;

    /**
     * Creates a new InMemoryStore with the specified vector dimensions.
     *
     * @param dimensions the dimension of vectors that will be stored
     * @throws IllegalArgumentException if dimensions is not positive
     */
    public InMemoryStore(final int dimensions) {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }
        this.dimensions = dimensions;
        this.vectors = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new InMemoryStore with default dimensions (1024).
     *
     * <p>This constructor is provided for convenience, but it's recommended to specify
     * the dimensions explicitly to match your embedding model.
     */
    public InMemoryStore() {
        this(1024);
    }

    @Override
    public Mono<String> add(final String id, final double[] embedding) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("ID cannot be null"));
        }

        try {
            validateDimensions(embedding, "Embedding");
        } catch (Exception e) {
            return Mono.error(e);
        }

        return Mono.fromCallable(
                () -> {
                    // Create a defensive copy to prevent external modification
                    double[] embeddingCopy = Arrays.copyOf(embedding, embedding.length);
                    vectors.put(id, embeddingCopy);
                    return id;
                });
    }

    @Override
    public Mono<List<VectorSearchResult>> search(final double[] queryEmbedding, final int topK) {
        try {
            validateDimensions(queryEmbedding, "Query embedding");
        } catch (Exception e) {
            return Mono.error(e);
        }

        if (topK <= 0) {
            return Mono.error(new IllegalArgumentException("TopK must be positive"));
        }

        return Mono.fromCallable(
                () -> {
                    if (vectors.isEmpty()) {
                        return new ArrayList<>();
                    }

                    List<VectorSearchResult> results = new ArrayList<>();

                    // Calculate similarity for all vectors
                    for (Map.Entry<String, double[]> entry : vectors.entrySet()) {
                        double similarity =
                                DistanceCalculator.cosineSimilarity(
                                        queryEmbedding, entry.getValue());
                        results.add(new VectorSearchResult(entry.getKey(), similarity));
                    }

                    // Sort by similarity (descending) and take top K
                    return results.stream()
                            .sorted(Comparator.comparing(VectorSearchResult::getScore).reversed())
                            .limit(topK)
                            .toList();
                });
    }

    @Override
    public Mono<Boolean> delete(final String id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("ID cannot be null"));
        }

        return Mono.fromCallable(
                () -> {
                    double[] removed = vectors.remove(id);
                    return removed != null;
                });
    }

    /**
     * Validates that an embedding has the correct dimensions.
     *
     * @param embedding the embedding to validate
     * @param paramName the parameter name for error messages
     * @throws IllegalArgumentException if embedding is null
     * @throws VectorStoreException if embedding dimension does not match expected dimensions
     */
    private void validateDimensions(final double[] embedding, final String paramName)
            throws VectorStoreException {
        if (embedding == null) {
            throw new IllegalArgumentException(paramName + " cannot be null");
        }
        if (embedding.length != dimensions) {
            throw new VectorStoreException(
                    String.format(
                            "%s dimension mismatch: expected %d, got %d",
                            paramName, dimensions, embedding.length));
        }
    }

    /**
     * Gets the number of vectors currently stored.
     *
     * <p>This method is thread-safe and returns the current snapshot of the store size.
     *
     * @return the number of stored vectors (always non-negative)
     */
    public int size() {
        return vectors.size();
    }

    /**
     * Checks if the store is empty.
     *
     * <p>Equivalent to {@code size() == 0}. This method is thread-safe.
     *
     * @return true if the store contains no vectors, false otherwise
     */
    public boolean isEmpty() {
        return vectors.isEmpty();
    }

    /**
     * Clears all vectors from the store.
     *
     * <p>After this operation, {@link #size()} returns 0 and {@link #isEmpty()} returns true.
     * This operation is thread-safe.
     */
    public void clear() {
        vectors.clear();
    }

    /**
     * Gets the dimension of vectors stored in this store.
     *
     * @return the vector dimension
     */
    public int getDimensions() {
        return dimensions;
    }
}
