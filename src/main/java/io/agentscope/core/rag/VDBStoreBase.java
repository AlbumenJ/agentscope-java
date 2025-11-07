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
package io.agentscope.core.rag;

import io.agentscope.core.rag.model.VectorSearchResult;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Interface for vector database storage.
 *
 * <p>This interface provides a unified API for storing and searching vector embeddings.
 * Implementations can use in-memory storage, Qdrant, ChromaDB, or other vector databases.
 */
public interface VDBStoreBase {

    /**
     * Adds a vector to the store.
     *
     * @param id the document ID
     * @param embedding the embedding vector
     * @return a Mono that emits the document ID when the operation completes
     */
    Mono<String> add(String id, double[] embedding);

    /**
     * Searches for similar vectors.
     *
     * @param queryEmbedding the query embedding vector
     * @param topK the maximum number of results to return
     * @return a Mono that emits a list of VectorSearchResult objects, sorted by similarity
     */
    Mono<List<VectorSearchResult>> search(double[] queryEmbedding, int topK);

    /**
     * Gets the underlying client (optional).
     *
     * <p>This method allows access to advanced features of the vector database.
     * Returns an empty Mono if not supported.
     *
     * @return a Mono that emits the client object, or empty if not supported
     */
    default Mono<Object> getClient() {
        return Mono.empty();
    }

    /**
     * Deletes a vector from the store (optional).
     *
     * @param id the document ID to delete
     * @return a Mono that emits true if the deletion was successful, false otherwise
     */
    default Mono<Boolean> delete(String id) {
        return Mono.just(true);
    }
}
