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

import com.google.common.util.concurrent.ListenableFuture;
import io.agentscope.core.rag.VDBStoreBase;
import io.agentscope.core.rag.model.VectorSearchResult;
import io.agentscope.core.rag.model.VectorStoreException;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.Vector;
import io.qdrant.client.grpc.Points.Vectors;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Qdrant vector database store implementation.
 *
 * <p>This class provides an interface for storing and searching vectors using Qdrant, a
 * production-ready vector database. It implements the VDBStoreBase interface to provide
 * a unified API for vector storage operations.
 *
 * <p>This implementation uses the Qdrant Java SDK (io.qdrant:client). To use this class,
 * add the following dependency:
 *
 * <pre>{@code
 * <dependency>
 *     <groupId>io.qdrant</groupId>
 *     <artifactId>client</artifactId>
 *     <version>1.15.0</version>
 *     <scope>provided</scope>
 * </dependency>
 * }</pre>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Using try-with-resources (recommended)
 * try (QdrantStore store = new QdrantStore("http://localhost:6333", "my_collection", 1024)) {
 *     store.add("doc1", embedding).block();
 *     List<VectorSearchResult> results = store.search(queryEmbedding, 5).block();
 *     // Store is automatically closed here
 * }
 *
 * // Manual resource management
 * QdrantStore store = new QdrantStore("http://localhost:6333", "my_collection", 1024);
 * try {
 *     store.add("doc1", embedding).block();
 *     List<VectorSearchResult> results = store.search(queryEmbedding, 5).block();
 * } finally {
 *     store.close();
 * }
 * }</pre>
 *
 * <p>Note: The location parameter can be:
 * <ul>
 *   <li>HTTP URL: "http://localhost:6333" - will use gRPC on port 6334</li>
 *   <li>gRPC URL: "localhost:6334" - direct gRPC connection</li>
 *   <li>Local path: "file:///path/to/qdrant" - local Qdrant instance</li>
 *   <li>Memory mode: ":memory:" - in-memory Qdrant (requires local Qdrant)</li>
 * </ul>
 */
public class QdrantStore implements VDBStoreBase, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(QdrantStore.class);

    private final String location;
    private final String collectionName;
    private final int dimensions;
    private final String apiKey;
    private final boolean useTransportLayerSecurity;
    private final boolean checkCompatibility;
    private final QdrantClient qdrantClient;
    private final QdrantGrpcClient grpcClient;
    private volatile boolean closed = false;

    /**
     * Creates a new QdrantStore using the builder configuration.
     *
     * @param builder the builder instance
     * @throws VectorStoreException if client initialization or collection creation fails
     */
    private QdrantStore(Builder builder) throws VectorStoreException {
        this.location = builder.location;
        this.collectionName = builder.collectionName;
        this.dimensions = builder.dimensions;
        this.apiKey = builder.apiKey;
        this.useTransportLayerSecurity = builder.useTransportLayerSecurity;
        this.checkCompatibility = builder.checkCompatibility;

        // Initialize client and collection immediately
        QdrantGrpcClient tempGrpcClient = null;
        QdrantClient tempQdrantClient = null;

        try {
            ConnectionInfo connInfo = parseLocation(location);

            // Build gRPC client with TLS and compatibility check options
            QdrantGrpcClient.Builder grpcClientBuilder =
                    QdrantGrpcClient.newBuilder(
                            connInfo.host,
                            connInfo.port,
                            useTransportLayerSecurity,
                            checkCompatibility);

            if (apiKey != null && !apiKey.trim().isEmpty()) {
                grpcClientBuilder = grpcClientBuilder.withApiKey(apiKey);
            }

            tempGrpcClient = grpcClientBuilder.build();

            // Create QdrantClient
            tempQdrantClient = new QdrantClient(tempGrpcClient);

            log.debug(
                    "Initialized Qdrant client: host={}, port={}, collection={}",
                    connInfo.host,
                    connInfo.port,
                    collectionName);

            // Ensure collection exists
            ensureCollection(tempQdrantClient);

            // Assign to final fields only after successful initialization
            this.grpcClient = tempGrpcClient;
            this.qdrantClient = tempQdrantClient;

            log.debug("QdrantStore initialized successfully for collection: {}", collectionName);
        } catch (Exception e) {
            // Clean up if initialization fails
            try {
                if (tempQdrantClient != null) {
                    tempQdrantClient.close();
                }
            } catch (Exception cleanupException) {
                log.warn(
                        "Error closing QdrantClient after initialization failure",
                        cleanupException);
            }
            try {
                if (tempGrpcClient != null) {
                    tempGrpcClient.close();
                }
            } catch (Exception cleanupException) {
                log.warn(
                        "Error closing QdrantGrpcClient after initialization failure",
                        cleanupException);
            }
            throw new VectorStoreException("Failed to initialize QdrantStore", e);
        }
    }

    @Override
    public Mono<String> add(String id, double[] embedding) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("ID cannot be null"));
        }
        if (embedding == null) {
            return Mono.error(new IllegalArgumentException("Embedding cannot be null"));
        }
        if (embedding.length != dimensions) {
            return Mono.error(
                    new VectorStoreException(
                            String.format(
                                    "Embedding dimension mismatch: expected %d, got %d",
                                    dimensions, embedding.length)));
        }

        return Mono.fromCallable(
                        () -> {
                            try {
                                ensureNotClosed();
                                return addVectorToQdrant(id, embedding);
                            } catch (Exception e) {
                                throw new VectorStoreException("Failed to add vector to Qdrant", e);
                            }
                        })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(
                        e ->
                                e instanceof VectorStoreException
                                        ? e
                                        : new VectorStoreException(
                                                "Failed to add vector to Qdrant", e));
    }

    @Override
    public Mono<List<VectorSearchResult>> search(double[] queryEmbedding, int topK) {
        if (queryEmbedding == null) {
            return Mono.error(new IllegalArgumentException("Query embedding cannot be null"));
        }
        if (queryEmbedding.length != dimensions) {
            return Mono.error(
                    new VectorStoreException(
                            String.format(
                                    "Query embedding dimension mismatch: expected %d, got %d",
                                    dimensions, queryEmbedding.length)));
        }
        if (topK <= 0) {
            return Mono.error(new IllegalArgumentException("TopK must be positive"));
        }

        return Mono.fromCallable(
                        () -> {
                            try {
                                ensureNotClosed();
                                return searchVectorsInQdrant(queryEmbedding, topK);
                            } catch (Exception e) {
                                throw new VectorStoreException(
                                        "Failed to search vectors in Qdrant", e);
                            }
                        })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(
                        e ->
                                e instanceof VectorStoreException
                                        ? e
                                        : new VectorStoreException(
                                                "Failed to search vectors in Qdrant", e));
    }

    @Override
    public Mono<Boolean> delete(String id) {
        if (id == null) {
            return Mono.error(new IllegalArgumentException("ID cannot be null"));
        }

        return Mono.fromCallable(
                        () -> {
                            try {
                                ensureNotClosed();
                                return deleteVectorFromQdrant(id);
                            } catch (Exception e) {
                                throw new VectorStoreException(
                                        "Failed to delete vector from Qdrant", e);
                            }
                        })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(
                        e ->
                                e instanceof VectorStoreException
                                        ? e
                                        : new VectorStoreException(
                                                "Failed to delete vector from Qdrant", e));
    }

    @Override
    public Mono<Object> getClient() {
        return Mono.fromCallable(
                        () -> {
                            ensureNotClosed();
                            return (Object) qdrantClient;
                        })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Ensures the store is not closed before performing operations.
     *
     * @throws VectorStoreException if the store has been closed
     */
    private void ensureNotClosed() throws VectorStoreException {
        if (closed) {
            throw new VectorStoreException("QdrantStore has been closed");
        }
    }

    /**
     * Ensures the collection exists, creating it if necessary.
     *
     * @param client the QdrantClient to use
     * @throws VectorStoreException if collection creation fails
     */
    private void ensureCollection(QdrantClient client) throws VectorStoreException {
        try {
            // Check if collection exists
            ListenableFuture<Boolean> existsFuture = client.collectionExistsAsync(collectionName);
            Boolean exists = existsFuture.get();
            if (Boolean.TRUE.equals(exists)) {
                log.debug("Collection '{}' already exists", collectionName);
                return;
            }

            // Create collection
            createCollection(client);
            log.debug("Created collection '{}' with dimensions {}", collectionName, dimensions);
        } catch (Exception e) {
            throw new VectorStoreException(
                    "Failed to ensure collection exists: " + collectionName, e);
        }
    }

    /**
     * Creates a new collection with the specified dimensions.
     *
     * @param client the QdrantClient to use
     * @throws Exception if collection creation fails
     */
    private void createCollection(QdrantClient client) throws Exception {
        VectorParams vectorParams =
                VectorParams.newBuilder().setDistance(Distance.Cosine).setSize(dimensions).build();

        ListenableFuture<?> future = client.createCollectionAsync(collectionName, vectorParams);
        future.get();
    }

    /**
     * Adds a vector to Qdrant.
     *
     * @param id the document ID
     * @param embedding the embedding vector
     * @return the document ID
     * @throws Exception if the operation fails
     */
    private String addVectorToQdrant(String id, double[] embedding) throws Exception {
        // Convert double[] to List<Float>
        List<Float> floatList = new ArrayList<>(embedding.length);
        for (double d : embedding) {
            floatList.add((float) d);
        }

        // Build PointId (using UUID for string ID)
        PointId pointId = PointId.newBuilder().setUuid(id).build();

        // Build Vector
        Vector vector = Vector.newBuilder().addAllData(floatList).build();

        // Build Vectors
        Vectors vectors = Vectors.newBuilder().setVector(vector).build();

        // Build PointStruct
        PointStruct point = PointStruct.newBuilder().setId(pointId).setVectors(vectors).build();

        // Upsert point
        ListenableFuture<?> future = qdrantClient.upsertAsync(collectionName, List.of(point));
        future.get();

        return id;
    }

    /**
     * Searches for similar vectors in Qdrant.
     *
     * @param queryEmbedding the query embedding vector
     * @param topK the maximum number of results
     * @return a list of search results
     * @throws Exception if the operation fails
     */
    private List<VectorSearchResult> searchVectorsInQdrant(double[] queryEmbedding, int topK)
            throws Exception {
        // Convert double[] to List<Float>
        List<Float> floatList = new ArrayList<>(queryEmbedding.length);
        for (double d : queryEmbedding) {
            floatList.add((float) d);
        }

        // Build SearchPoints
        SearchPoints searchPoints =
                SearchPoints.newBuilder()
                        .setCollectionName(collectionName)
                        .addAllVector(floatList)
                        .setLimit(topK)
                        .build();

        // Search
        ListenableFuture<List<ScoredPoint>> future = qdrantClient.searchAsync(searchPoints);
        List<ScoredPoint> scoredPoints = future.get();

        // Extract results
        List<VectorSearchResult> results = new ArrayList<>();
        for (ScoredPoint scoredPoint : scoredPoints) {
            // Get ID
            PointId pointId = scoredPoint.getId();
            String id;
            if (pointId.hasUuid()) {
                id = pointId.getUuid();
            } else {
                id = String.valueOf(pointId.getNum());
            }

            // Get score
            double score = scoredPoint.getScore();

            results.add(new VectorSearchResult(id, score));
        }

        return results;
    }

    /**
     * Deletes a vector from Qdrant.
     *
     * @param id the document ID to delete
     * @return true if the deletion was successful
     * @throws Exception if the operation fails
     */
    private boolean deleteVectorFromQdrant(String id) throws Exception {
        // Build PointId (using UUID for string ID)
        PointId pointId = PointId.newBuilder().setUuid(id).build();

        // Delete point
        ListenableFuture<?> future = qdrantClient.deleteAsync(collectionName, List.of(pointId));
        future.get();

        return true;
    }

    /**
     * Parses the location string to extract host and port information.
     *
     * @param location the location string (HTTP URL, gRPC URL, or file path)
     * @return connection information
     */
    private ConnectionInfo parseLocation(String location) {
        // Handle :memory: mode
        if (":memory:".equals(location)) {
            return new ConnectionInfo("localhost", 6334);
        }

        // Handle file:// protocol (local Qdrant)
        if (location.startsWith("file://")) {
            return new ConnectionInfo("localhost", 6334);
        }

        try {
            URI uri = URI.create(location);
            String host = uri.getHost();
            int port = uri.getPort();

            // If it's an HTTP URL, use gRPC port (6334) instead of HTTP port (6333)
            if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
                if (port == 6333 || port == -1) {
                    port = 6334; // Default gRPC port
                }
            }

            // If no port specified and no scheme, assume it's a direct gRPC connection
            if (host == null && port == -1) {
                // Try parsing as host:port
                if (location.contains(":")) {
                    String[] parts = location.split(":", 2);
                    host = parts[0];
                    try {
                        port = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        port = 6334; // Default gRPC port
                    }
                } else {
                    host = location;
                    port = 6334; // Default gRPC port
                }
            }

            if (host == null) {
                host = "localhost";
            }
            if (port == -1) {
                port = 6334; // Default gRPC port
            }

            return new ConnectionInfo(host, port);
        } catch (Exception e) {
            // Fallback: treat as hostname
            log.warn("Failed to parse location '{}', using as hostname", location, e);
            return new ConnectionInfo(location, 6334);
        }
    }

    /**
     * Connection information holder.
     */
    private static class ConnectionInfo {
        final String host;
        final int port;

        ConnectionInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    /**
     * Gets the Qdrant server location.
     *
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Gets the collection name.
     *
     * @return the collection name
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * Gets the vector dimensions.
     *
     * @return the dimensions
     */
    public int getDimensions() {
        return dimensions;
    }

    /**
     * Gets the API key (if set).
     *
     * @return the API key, or null if not set
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Closes the Qdrant client and releases all resources.
     *
     * <p>This method closes both the QdrantClient and the underlying QdrantGrpcClient,
     * releasing all gRPC connections and associated resources. This method is idempotent
     * and can be called multiple times safely.
     *
     * <p>After closing, all operations on this store will fail with a VectorStoreException.
     * It's recommended to use try-with-resources for automatic resource management:
     *
     * <pre>{@code
     * try (QdrantStore store = new QdrantStore("http://localhost:6333", "collection", 1024)) {
     *     store.add("doc1", embedding).block();
     *     // Store is automatically closed here
     * }
     * }</pre>
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        synchronized (this) {
            if (closed) {
                return;
            }

            closed = true;

            try {
                if (qdrantClient != null) {
                    log.debug("Closing QdrantClient for collection: {}", collectionName);
                    qdrantClient.close();
                }
            } catch (Exception e) {
                log.warn("Error closing QdrantClient", e);
            }

            try {
                if (grpcClient != null) {
                    log.debug("Closing QdrantGrpcClient for collection: {}", collectionName);
                    grpcClient.close();
                }
            } catch (Exception e) {
                log.warn("Error closing QdrantGrpcClient", e);
            }

            log.debug("QdrantStore closed for collection: {}", collectionName);
        }
    }

    /**
     * Checks if this store has been closed.
     *
     * @return true if the store has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }
}
