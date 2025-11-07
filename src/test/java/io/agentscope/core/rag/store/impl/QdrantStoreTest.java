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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.agentscope.core.rag.model.VectorStoreException;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.CollectionOperationResponse;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.UpdateResult;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

/**
 * Unit tests for QdrantStore.
 */
@Tag("unit")
@DisplayName("QdrantStore Unit Tests")
class QdrantStoreTest {

    private static final String TEST_LOCATION = "http://localhost:6333";
    private static final String TEST_COLLECTION = "test_collection";
    private static final int TEST_DIMENSIONS = 1024;

    @Test
    @DisplayName("Should create QdrantStore with valid parameters")
    void testConstructor() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
        assertEquals(TEST_LOCATION, store.getLocation());
        assertEquals(TEST_COLLECTION, store.getCollectionName());
        assertEquals(TEST_DIMENSIONS, store.getDimensions());
    }

    @Test
    @DisplayName("Should create QdrantStore with API key")
    void testConstructorWithApiKey() {
        QdrantStore store =
                new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS, "test-api-key");
        assertEquals("test-api-key", store.getApiKey());
    }

    @Test
    @DisplayName("Should throw exception for null location")
    void testNullLocation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QdrantStore(null, TEST_COLLECTION, TEST_DIMENSIONS));
        assertThrows(
                IllegalArgumentException.class,
                () -> new QdrantStore("", TEST_COLLECTION, TEST_DIMENSIONS));
    }

    @Test
    @DisplayName("Should throw exception for null collection name")
    void testNullCollectionName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QdrantStore(TEST_LOCATION, null, TEST_DIMENSIONS));
        assertThrows(
                IllegalArgumentException.class,
                () -> new QdrantStore(TEST_LOCATION, "", TEST_DIMENSIONS));
    }

    @Test
    @DisplayName("Should throw exception for invalid dimensions")
    void testInvalidDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QdrantStore(TEST_LOCATION, TEST_COLLECTION, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new QdrantStore(TEST_LOCATION, TEST_COLLECTION, -1));
    }

    @Test
    @DisplayName("Should throw exception when adding vector without client")
    void testAddRequiresClient() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
        double[] embedding = new double[TEST_DIMENSIONS];

        StepVerifier.create(store.add("doc1", embedding))
                .expectError(VectorStoreException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw exception for null ID when adding")
    void testAddNullId() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
        double[] embedding = new double[TEST_DIMENSIONS];

        StepVerifier.create(store.add(null, embedding))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw exception for null embedding when adding")
    void testAddNullEmbedding() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);

        StepVerifier.create(store.add("doc1", null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw exception for dimension mismatch when adding")
    void testAddDimensionMismatch() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
        double[] embedding = new double[512]; // Wrong dimension

        StepVerifier.create(store.add("doc1", embedding))
                .expectError(VectorStoreException.class)
                .verify();
    }

    @Test
    @DisplayName("Should add vector successfully")
    void testAddSuccess() throws Exception {
        // Mock collection exists check
        SettableFuture<Boolean> existsFuture = SettableFuture.create();
        existsFuture.set(false);
        ListenableFuture<Boolean> existsListenableFuture = existsFuture;

        // Mock collection creation
        SettableFuture<CollectionOperationResponse> createFuture = SettableFuture.create();
        createFuture.set(CollectionOperationResponse.getDefaultInstance());
        ListenableFuture<CollectionOperationResponse> createListenableFuture = createFuture;

        // Mock upsert
        SettableFuture<UpdateResult> upsertFuture = SettableFuture.create();
        upsertFuture.set(UpdateResult.getDefaultInstance());
        ListenableFuture<UpdateResult> upsertListenableFuture = upsertFuture;

        // Mock QdrantGrpcClient and Builder
        QdrantGrpcClient mockGrpcClient = Mockito.mock(QdrantGrpcClient.class);
        QdrantGrpcClient.Builder mockBuilder = Mockito.mock(QdrantGrpcClient.Builder.class);
        when(mockBuilder.withApiKey(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockGrpcClient);

        try (MockedStatic<QdrantGrpcClient> grpcClientStaticMock =
                        Mockito.mockStatic(QdrantGrpcClient.class);
                MockedConstruction<QdrantClient> clientMock =
                        Mockito.mockConstruction(
                                QdrantClient.class,
                                (mock, context) -> {
                                    when(mock.collectionExistsAsync(anyString()))
                                            .thenReturn(existsListenableFuture);
                                    when(mock.createCollectionAsync(
                                                    anyString(), isA(VectorParams.class)))
                                            .thenReturn(createListenableFuture);
                                    when(mock.upsertAsync(anyString(), anyList()))
                                            .thenReturn(upsertListenableFuture);
                                })) {

            grpcClientStaticMock
                    .when(() -> QdrantGrpcClient.newBuilder(anyString(), anyInt()))
                    .thenReturn(mockBuilder);

            QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
            double[] embedding = new double[TEST_DIMENSIONS];
            for (int i = 0; i < TEST_DIMENSIONS; i++) {
                embedding[i] = 0.1 * i;
            }

            StepVerifier.create(store.add("doc1", embedding))
                    .assertNext(id -> assertEquals("doc1", id))
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("Should throw exception when searching without client")
    void testSearchRequiresClient() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
        double[] queryEmbedding = new double[TEST_DIMENSIONS];

        StepVerifier.create(store.search(queryEmbedding, 5))
                .expectError(VectorStoreException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw exception for null query embedding")
    void testSearchNullEmbedding() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);

        StepVerifier.create(store.search(null, 5))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw exception for dimension mismatch when searching")
    void testSearchDimensionMismatch() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
        double[] queryEmbedding = new double[512]; // Wrong dimension

        StepVerifier.create(store.search(queryEmbedding, 5))
                .expectError(VectorStoreException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw exception for invalid topK")
    void testSearchInvalidTopK() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
        double[] queryEmbedding = new double[TEST_DIMENSIONS];

        StepVerifier.create(store.search(queryEmbedding, 0))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should search vectors successfully")
    void testSearchSuccess() throws Exception {
        // Mock collection exists check
        SettableFuture<Boolean> existsFuture = SettableFuture.create();
        existsFuture.set(true);
        ListenableFuture<Boolean> existsListenableFuture = existsFuture;

        // Mock search
        SettableFuture<List<ScoredPoint>> searchFuture = SettableFuture.create();
        PointId pointId1 = PointId.newBuilder().setUuid("doc1").build();
        PointId pointId2 = PointId.newBuilder().setUuid("doc2").build();
        ScoredPoint scoredPoint1 = ScoredPoint.newBuilder().setId(pointId1).setScore(0.95f).build();
        ScoredPoint scoredPoint2 = ScoredPoint.newBuilder().setId(pointId2).setScore(0.85f).build();
        searchFuture.set(List.of(scoredPoint1, scoredPoint2));
        ListenableFuture<List<ScoredPoint>> searchListenableFuture = searchFuture;

        // Mock QdrantGrpcClient and Builder
        QdrantGrpcClient mockGrpcClient = Mockito.mock(QdrantGrpcClient.class);
        QdrantGrpcClient.Builder mockBuilder = Mockito.mock(QdrantGrpcClient.Builder.class);
        when(mockBuilder.withApiKey(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockGrpcClient);

        try (MockedStatic<QdrantGrpcClient> grpcClientStaticMock =
                        Mockito.mockStatic(QdrantGrpcClient.class);
                MockedConstruction<QdrantClient> clientMock =
                        Mockito.mockConstruction(
                                QdrantClient.class,
                                (mock, context) -> {
                                    when(mock.collectionExistsAsync(anyString()))
                                            .thenReturn(existsListenableFuture);
                                    when(mock.searchAsync(any()))
                                            .thenReturn(searchListenableFuture);
                                })) {

            grpcClientStaticMock
                    .when(() -> QdrantGrpcClient.newBuilder(anyString(), anyInt()))
                    .thenReturn(mockBuilder);

            QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
            double[] queryEmbedding = new double[TEST_DIMENSIONS];
            for (int i = 0; i < TEST_DIMENSIONS; i++) {
                queryEmbedding[i] = 0.1 * i;
            }

            StepVerifier.create(store.search(queryEmbedding, 5))
                    .assertNext(
                            results -> {
                                assertEquals(2, results.size());
                                assertEquals("doc1", results.get(0).getId());
                                assertEquals(0.95, results.get(0).getScore(), 1e-9);
                                assertEquals("doc2", results.get(1).getId());
                                assertEquals(0.85, results.get(1).getScore(), 1e-9);
                            })
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("Should return empty list when no results found")
    void testSearchEmptyResults() throws Exception {
        // Mock collection exists check
        SettableFuture<Boolean> existsFuture = SettableFuture.create();
        existsFuture.set(true);
        ListenableFuture<Boolean> existsListenableFuture = existsFuture;

        // Mock search with empty results
        SettableFuture<List<ScoredPoint>> searchFuture = SettableFuture.create();
        searchFuture.set(List.of());
        ListenableFuture<List<ScoredPoint>> searchListenableFuture = searchFuture;

        // Mock QdrantGrpcClient and Builder
        QdrantGrpcClient mockGrpcClient = Mockito.mock(QdrantGrpcClient.class);
        QdrantGrpcClient.Builder mockBuilder = Mockito.mock(QdrantGrpcClient.Builder.class);
        when(mockBuilder.withApiKey(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockGrpcClient);

        try (MockedStatic<QdrantGrpcClient> grpcClientStaticMock =
                        Mockito.mockStatic(QdrantGrpcClient.class);
                MockedConstruction<QdrantClient> clientMock =
                        Mockito.mockConstruction(
                                QdrantClient.class,
                                (mock, context) -> {
                                    when(mock.collectionExistsAsync(anyString()))
                                            .thenReturn(existsListenableFuture);
                                    when(mock.searchAsync(any()))
                                            .thenReturn(searchListenableFuture);
                                })) {

            grpcClientStaticMock
                    .when(() -> QdrantGrpcClient.newBuilder(anyString(), anyInt()))
                    .thenReturn(mockBuilder);

            QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
            double[] queryEmbedding = new double[TEST_DIMENSIONS];

            StepVerifier.create(store.search(queryEmbedding, 5))
                    .assertNext(results -> assertEquals(0, results.size()))
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("Should handle numeric point IDs in search results")
    void testSearchWithNumericId() throws Exception {
        // Mock collection exists check
        SettableFuture<Boolean> existsFuture = SettableFuture.create();
        existsFuture.set(true);
        ListenableFuture<Boolean> existsListenableFuture = existsFuture;

        // Mock search with numeric ID
        SettableFuture<List<ScoredPoint>> searchFuture = SettableFuture.create();
        PointId pointId = PointId.newBuilder().setNum(12345L).build();
        ScoredPoint scoredPoint = ScoredPoint.newBuilder().setId(pointId).setScore(0.9f).build();
        searchFuture.set(List.of(scoredPoint));
        ListenableFuture<List<ScoredPoint>> searchListenableFuture = searchFuture;

        // Mock QdrantGrpcClient and Builder
        QdrantGrpcClient mockGrpcClient = Mockito.mock(QdrantGrpcClient.class);
        QdrantGrpcClient.Builder mockBuilder = Mockito.mock(QdrantGrpcClient.Builder.class);
        when(mockBuilder.withApiKey(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockGrpcClient);

        try (MockedStatic<QdrantGrpcClient> grpcClientStaticMock =
                        Mockito.mockStatic(QdrantGrpcClient.class);
                MockedConstruction<QdrantClient> clientMock =
                        Mockito.mockConstruction(
                                QdrantClient.class,
                                (mock, context) -> {
                                    when(mock.collectionExistsAsync(anyString()))
                                            .thenReturn(existsListenableFuture);
                                    when(mock.searchAsync(any()))
                                            .thenReturn(searchListenableFuture);
                                })) {

            grpcClientStaticMock
                    .when(() -> QdrantGrpcClient.newBuilder(anyString(), anyInt()))
                    .thenReturn(mockBuilder);

            QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
            double[] queryEmbedding = new double[TEST_DIMENSIONS];

            StepVerifier.create(store.search(queryEmbedding, 5))
                    .assertNext(
                            results -> {
                                assertEquals(1, results.size());
                                assertEquals("12345", results.get(0).getId());
                                assertEquals(0.9, results.get(0).getScore(), 1e-9);
                            })
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("Should throw exception when deleting without client")
    void testDeleteRequiresClient() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);

        StepVerifier.create(store.delete("doc1")).expectError(VectorStoreException.class).verify();
    }

    @Test
    @DisplayName("Should throw exception for null ID when deleting")
    void testDeleteNullId() {
        QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);

        StepVerifier.create(store.delete(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    @DisplayName("Should delete vector successfully")
    void testDeleteSuccess() throws Exception {
        // Mock collection exists check
        SettableFuture<Boolean> existsFuture = SettableFuture.create();
        existsFuture.set(true);
        ListenableFuture<Boolean> existsListenableFuture = existsFuture;

        // Mock delete
        SettableFuture<UpdateResult> deleteFuture = SettableFuture.create();
        deleteFuture.set(UpdateResult.getDefaultInstance());
        ListenableFuture<UpdateResult> deleteListenableFuture = deleteFuture;

        // Mock QdrantGrpcClient and Builder
        QdrantGrpcClient mockGrpcClient = Mockito.mock(QdrantGrpcClient.class);
        QdrantGrpcClient.Builder mockBuilder = Mockito.mock(QdrantGrpcClient.Builder.class);
        when(mockBuilder.withApiKey(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockGrpcClient);

        try (MockedStatic<QdrantGrpcClient> grpcClientStaticMock =
                        Mockito.mockStatic(QdrantGrpcClient.class);
                MockedConstruction<QdrantClient> clientMock =
                        Mockito.mockConstruction(
                                QdrantClient.class,
                                (mock, context) -> {
                                    when(mock.collectionExistsAsync(anyString()))
                                            .thenReturn(existsListenableFuture);
                                    when(mock.deleteAsync(anyString(), anyList()))
                                            .thenReturn(deleteListenableFuture);
                                })) {

            grpcClientStaticMock
                    .when(() -> QdrantGrpcClient.newBuilder(anyString(), anyInt()))
                    .thenReturn(mockBuilder);

            QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);

            StepVerifier.create(store.delete("doc1"))
                    .assertNext(deleted -> assertEquals(true, deleted))
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("Should get client successfully")
    void testGetClient() throws Exception {
        // Mock collection exists check
        SettableFuture<Boolean> existsFuture = SettableFuture.create();
        existsFuture.set(true);
        ListenableFuture<Boolean> existsListenableFuture = existsFuture;

        // Mock QdrantGrpcClient and Builder
        QdrantGrpcClient mockGrpcClient = Mockito.mock(QdrantGrpcClient.class);
        QdrantGrpcClient.Builder mockBuilder = Mockito.mock(QdrantGrpcClient.Builder.class);
        when(mockBuilder.withApiKey(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockGrpcClient);

        try (MockedStatic<QdrantGrpcClient> grpcClientStaticMock =
                        Mockito.mockStatic(QdrantGrpcClient.class);
                MockedConstruction<QdrantClient> clientMock =
                        Mockito.mockConstruction(
                                QdrantClient.class,
                                (mock, context) -> {
                                    when(mock.collectionExistsAsync(anyString()))
                                            .thenReturn(existsListenableFuture);
                                })) {

            grpcClientStaticMock
                    .when(() -> QdrantGrpcClient.newBuilder(anyString(), anyInt()))
                    .thenReturn(mockBuilder);

            QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);

            StepVerifier.create(store.getClient())
                    .assertNext(client -> assertNotNull(client))
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("Should create collection if it doesn't exist")
    void testCreateCollectionIfNotExists() throws Exception {
        // Mock collection doesn't exist
        SettableFuture<Boolean> existsFuture = SettableFuture.create();
        existsFuture.set(false);
        ListenableFuture<Boolean> existsListenableFuture = existsFuture;

        // Mock collection creation
        SettableFuture<CollectionOperationResponse> createFuture = SettableFuture.create();
        createFuture.set(CollectionOperationResponse.getDefaultInstance());
        ListenableFuture<CollectionOperationResponse> createListenableFuture = createFuture;

        // Mock upsert
        SettableFuture<UpdateResult> upsertFuture = SettableFuture.create();
        upsertFuture.set(UpdateResult.getDefaultInstance());
        ListenableFuture<UpdateResult> upsertListenableFuture = upsertFuture;

        // Mock QdrantGrpcClient and Builder
        QdrantGrpcClient mockGrpcClient = Mockito.mock(QdrantGrpcClient.class);
        QdrantGrpcClient.Builder mockBuilder = Mockito.mock(QdrantGrpcClient.Builder.class);
        when(mockBuilder.withApiKey(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockGrpcClient);

        try (MockedStatic<QdrantGrpcClient> grpcClientStaticMock =
                        Mockito.mockStatic(QdrantGrpcClient.class);
                MockedConstruction<QdrantClient> clientMock =
                        Mockito.mockConstruction(
                                QdrantClient.class,
                                (mock, context) -> {
                                    when(mock.collectionExistsAsync(anyString()))
                                            .thenReturn(existsListenableFuture);
                                    when(mock.createCollectionAsync(
                                                    anyString(), isA(VectorParams.class)))
                                            .thenReturn(createListenableFuture);
                                    when(mock.upsertAsync(anyString(), anyList()))
                                            .thenReturn(upsertListenableFuture);
                                })) {

            grpcClientStaticMock
                    .when(() -> QdrantGrpcClient.newBuilder(anyString(), anyInt()))
                    .thenReturn(mockBuilder);

            QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
            double[] embedding = new double[TEST_DIMENSIONS];

            StepVerifier.create(store.add("doc1", embedding))
                    .assertNext(id -> assertEquals("doc1", id))
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("Should handle location parsing for different formats")
    void testLocationParsing() {
        // Test HTTP URL
        QdrantStore store1 = new QdrantStore("http://localhost:6333", TEST_COLLECTION, 1024);
        assertEquals("http://localhost:6333", store1.getLocation());

        // Test gRPC URL
        QdrantStore store2 = new QdrantStore("localhost:6334", TEST_COLLECTION, 1024);
        assertEquals("localhost:6334", store2.getLocation());

        // Test memory mode
        QdrantStore store3 = new QdrantStore(":memory:", TEST_COLLECTION, 1024);
        assertEquals(":memory:", store3.getLocation());

        // Test file protocol
        QdrantStore store4 = new QdrantStore("file:///path/to/qdrant", TEST_COLLECTION, 1024);
        assertEquals("file:///path/to/qdrant", store4.getLocation());
    }

    @Test
    @DisplayName("Should handle API errors gracefully")
    void testApiErrorHandling() throws Exception {
        // Mock collection exists check that throws exception
        SettableFuture<Boolean> existsFuture = SettableFuture.create();
        existsFuture.setException(
                new ExecutionException(new RuntimeException("Connection failed")));
        ListenableFuture<Boolean> existsListenableFuture = existsFuture;

        // Mock QdrantGrpcClient and Builder
        QdrantGrpcClient mockGrpcClient = Mockito.mock(QdrantGrpcClient.class);
        QdrantGrpcClient.Builder mockBuilder = Mockito.mock(QdrantGrpcClient.Builder.class);
        when(mockBuilder.withApiKey(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockGrpcClient);

        try (MockedStatic<QdrantGrpcClient> grpcClientStaticMock =
                        Mockito.mockStatic(QdrantGrpcClient.class);
                MockedConstruction<QdrantClient> clientMock =
                        Mockito.mockConstruction(
                                QdrantClient.class,
                                (mock, context) -> {
                                    when(mock.collectionExistsAsync(anyString()))
                                            .thenReturn(existsListenableFuture);
                                })) {

            grpcClientStaticMock
                    .when(() -> QdrantGrpcClient.newBuilder(anyString(), anyInt()))
                    .thenReturn(mockBuilder);

            QdrantStore store = new QdrantStore(TEST_LOCATION, TEST_COLLECTION, TEST_DIMENSIONS);
            double[] embedding = new double[TEST_DIMENSIONS];

            StepVerifier.create(store.add("doc1", embedding))
                    .expectError(VectorStoreException.class)
                    .verify();
        }
    }
}
