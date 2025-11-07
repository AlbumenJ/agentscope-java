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
package io.agentscope.core.rag.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.rag.reader.impl.SplitStrategy;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RAGConfiguration.
 */
@Tag("unit")
@DisplayName("RAGConfiguration Unit Tests")
class RAGConfigurationTest {

    @Test
    @DisplayName("Should create RAGConfiguration with default values")
    void testDefaultConfig() {
        RAGConfiguration config = RAGConfiguration.defaultConfig();

        assertNotNull(config.getReader());
        assertNotNull(config.getEmbedding());
        assertNotNull(config.getVectorStore());
        assertNotNull(config.getDefaultRetrieve());
    }

    @Test
    @DisplayName("Should create RAGConfiguration with builder")
    void testBuilder() {
        ReaderConfiguration readerConfig =
                ReaderConfiguration.builder()
                        .chunkSize(1024)
                        .splitStrategy(SplitStrategy.TOKEN)
                        .overlapSize(100)
                        .build();

        EmbeddingConfiguration embeddingConfig =
                EmbeddingConfiguration.builder()
                        .provider("dashscope")
                        .model("text-embedding-v3")
                        .dimensions(1024)
                        .apiKey("test-key")
                        .build();

        VectorStoreConfiguration vectorStoreConfig =
                VectorStoreConfiguration.builder().provider("memory").dimensions(1024).build();

        RetrieveConfiguration retrieveConfig =
                RetrieveConfiguration.builder().limit(5).scoreThreshold(0.5).build();

        RAGConfiguration config =
                RAGConfiguration.builder()
                        .reader(readerConfig)
                        .embedding(embeddingConfig)
                        .vectorStore(vectorStoreConfig)
                        .defaultRetrieve(retrieveConfig)
                        .build();

        assertEquals(readerConfig, config.getReader());
        assertEquals(embeddingConfig, config.getEmbedding());
        assertEquals(vectorStoreConfig, config.getVectorStore());
        assertEquals(retrieveConfig, config.getDefaultRetrieve());
    }

    @Test
    @DisplayName("Should use default configurations when not provided")
    void testPartialConfiguration() {
        RAGConfiguration config = RAGConfiguration.builder().build();

        assertNotNull(config.getReader());
        assertNotNull(config.getEmbedding());
        assertNotNull(config.getVectorStore());
        assertNotNull(config.getDefaultRetrieve());

        // Verify default values
        assertEquals(512, config.getReader().getDefaultChunkSize());
        assertEquals("dashscope", config.getEmbedding().getProvider());
        assertEquals("memory", config.getVectorStore().getProvider());
        assertEquals(5, config.getDefaultRetrieve().getLimit());
    }

    @Test
    @DisplayName("Should allow partial configuration")
    void testPartialBuilder() {
        ReaderConfiguration readerConfig = ReaderConfiguration.builder().chunkSize(2048).build();

        RAGConfiguration config = RAGConfiguration.builder().reader(readerConfig).build();

        assertEquals(2048, config.getReader().getDefaultChunkSize());
        // Other configs should use defaults
        assertNotNull(config.getEmbedding());
        assertNotNull(config.getVectorStore());
        assertNotNull(config.getDefaultRetrieve());
    }

    @Test
    @DisplayName("Should create complete configuration")
    void testCompleteConfiguration() {
        RAGConfiguration config =
                RAGConfiguration.builder()
                        .reader(
                                ReaderConfiguration.builder()
                                        .chunkSize(1024)
                                        .splitStrategy(SplitStrategy.TOKEN)
                                        .overlapSize(100)
                                        .enableImageOCR(true)
                                        .build())
                        .embedding(
                                EmbeddingConfiguration.builder()
                                        .provider("openai")
                                        .model("text-embedding-ada-002")
                                        .dimensions(1536)
                                        .timeout(Duration.ofMinutes(2))
                                        .enableCache(true)
                                        .cacheDirectory("./cache")
                                        .apiKey("api-key")
                                        .build())
                        .vectorStore(
                                VectorStoreConfiguration.builder()
                                        .provider("qdrant")
                                        .location("./data/qdrant")
                                        .collectionName("my_collection")
                                        .dimensions(1536)
                                        .searchLimit(20)
                                        .scoreThreshold(0.7)
                                        .build())
                        .defaultRetrieve(
                                RetrieveConfiguration.builder()
                                        .limit(10)
                                        .scoreThreshold(0.6)
                                        .enableReranking(true)
                                        .reranker("cross-encoder")
                                        .build())
                        .build();

        // Verify all configurations are set correctly
        assertEquals(1024, config.getReader().getDefaultChunkSize());
        assertEquals(SplitStrategy.TOKEN, config.getReader().getDefaultSplitStrategy());
        assertTrue(config.getReader().isImageOCREnabled());

        assertEquals("openai", config.getEmbedding().getProvider());
        assertEquals(1536, config.getEmbedding().getDimensions());
        assertTrue(config.getEmbedding().isCacheEnabled());

        assertEquals("qdrant", config.getVectorStore().getProvider());
        assertEquals(1536, config.getVectorStore().getDimensions());
        assertEquals(20, config.getVectorStore().getSearchLimit());

        assertEquals(10, config.getDefaultRetrieve().getLimit());
        assertTrue(config.getDefaultRetrieve().isRerankingEnabled());
    }
}
