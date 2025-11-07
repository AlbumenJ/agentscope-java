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
package io.agentscope.core.rag.config;

/**
 * Main configuration class for RAG system.
 *
 * <p>This class aggregates all RAG-related configurations into a single object,
 * providing a unified way to configure the entire RAG pipeline including readers,
 * embedding models, vector stores, and retrieval settings.
 *
 * <p>Example usage:
 * <pre>{@code
 * RAGConfiguration config = RAGConfiguration.builder()
 *     .reader(ReaderConfiguration.builder()
 *         .chunkSize(1024)
 *         .splitStrategy(SplitStrategy.TOKEN)
 *         .overlapSize(100)
 *         .build())
 *     .embedding(EmbeddingConfiguration.builder()
 *         .provider("dashscope")
 *         .model("text-embedding-v3")
 *         .dimensions(1024)
 *         .apiKey(System.getenv("DASHSCOPE_API_KEY"))
 *         .build())
 *     .vectorStore(VectorStoreConfiguration.builder()
 *         .provider("memory")
 *         .dimensions(1024)
 *         .build())
 *     .defaultRetrieve(RetrieveConfiguration.builder()
 *         .limit(5)
 *         .scoreThreshold(0.5)
 *         .build())
 *     .build();
 * }</pre>
 */
public class RAGConfiguration {

    private final ReaderConfiguration reader;
    private final EmbeddingConfiguration embedding;
    private final VectorStoreConfiguration vectorStore;
    private final RetrieveConfiguration defaultRetrieve;

    private RAGConfiguration(Builder builder) {
        this.reader = builder.reader != null ? builder.reader : ReaderConfiguration.defaultConfig();
        this.embedding =
                builder.embedding != null
                        ? builder.embedding
                        : EmbeddingConfiguration.defaultConfig();
        this.vectorStore =
                builder.vectorStore != null
                        ? builder.vectorStore
                        : VectorStoreConfiguration.defaultConfig();
        this.defaultRetrieve =
                builder.defaultRetrieve != null
                        ? builder.defaultRetrieve
                        : RetrieveConfiguration.defaultConfig();
    }

    /**
     * Gets the reader configuration.
     *
     * @return the reader configuration
     */
    public ReaderConfiguration getReader() {
        return reader;
    }

    /**
     * Gets the embedding configuration.
     *
     * @return the embedding configuration
     */
    public EmbeddingConfiguration getEmbedding() {
        return embedding;
    }

    /**
     * Gets the vector store configuration.
     *
     * @return the vector store configuration
     */
    public VectorStoreConfiguration getVectorStore() {
        return vectorStore;
    }

    /**
     * Gets the default retrieval configuration.
     *
     * @return the default retrieval configuration
     */
    public RetrieveConfiguration getDefaultRetrieve() {
        return defaultRetrieve;
    }

    /**
     * Creates a new builder instance.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a RAGConfiguration with default values for all components.
     *
     * @return a default configuration
     */
    public static RAGConfiguration defaultConfig() {
        return builder().build();
    }

    /**
     * Builder for RAGConfiguration.
     */
    public static class Builder {

        private ReaderConfiguration reader;
        private EmbeddingConfiguration embedding;
        private VectorStoreConfiguration vectorStore;
        private RetrieveConfiguration defaultRetrieve;

        /**
         * Sets the reader configuration.
         *
         * @param reader the reader configuration
         * @return this builder for chaining
         */
        public Builder reader(ReaderConfiguration reader) {
            this.reader = reader;
            return this;
        }

        /**
         * Sets the embedding configuration.
         *
         * @param embedding the embedding configuration
         * @return this builder for chaining
         */
        public Builder embedding(EmbeddingConfiguration embedding) {
            this.embedding = embedding;
            return this;
        }

        /**
         * Sets the vector store configuration.
         *
         * @param vectorStore the vector store configuration
         * @return this builder for chaining
         */
        public Builder vectorStore(VectorStoreConfiguration vectorStore) {
            this.vectorStore = vectorStore;
            return this;
        }

        /**
         * Sets the default retrieval configuration.
         *
         * @param defaultRetrieve the default retrieval configuration
         * @return this builder for chaining
         */
        public Builder defaultRetrieve(RetrieveConfiguration defaultRetrieve) {
            this.defaultRetrieve = defaultRetrieve;
            return this;
        }

        /**
         * Builds the RAGConfiguration instance.
         *
         * <p>If any configuration is not set, default values will be used.
         *
         * @return the configured RAGConfiguration
         */
        public RAGConfiguration build() {
            return new RAGConfiguration(this);
        }
    }
}
