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
package io.agentscope.core.rag.model;

/**
 * Result of a vector search operation.
 *
 * <p>This class represents a single result from a vector similarity search,
 * containing the document ID and the similarity score.
 */
public class VectorSearchResult {

    private final String id;
    private final double score;

    /**
     * Creates a new VectorSearchResult.
     *
     * @param id the document ID
     * @param score the similarity score
     */
    public VectorSearchResult(String id, double score) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        this.id = id;
        this.score = score;
    }

    /**
     * Gets the document ID.
     *
     * @return the document ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the similarity score.
     *
     * @return the similarity score
     */
    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return String.format("VectorSearchResult(id=%s, score=%.3f)", id, score);
    }
}
