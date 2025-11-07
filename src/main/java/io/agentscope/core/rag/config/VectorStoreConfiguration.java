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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for vector stores.
 *
 * <p>This class provides configuration options for vector database stores,
 * including provider type, location, collection name, dimensions, search limits,
 * and additional provider-specific settings.
 *
 * <p>Example usage:
 * <pre>{@code
 * VectorStoreConfiguration config = VectorStoreConfiguration.builder()
 *     .provider("qdrant")
 *     .location("./data/qdrant")
 *     .collectionName("my_collection")
 *     .dimensions(1024)
 *     .searchLimit(10)
 *     .scoreThreshold(0.6)
 *     .additionalConfig("hnsw_config", Map.of("M", 16, "ef_construct", 64))
 *     .build();
 * }</pre>
 */
public class VectorStoreConfiguration {

    private String provider = "memory";
    private String location = ":memory:";
    private String collectionName = "agentscope_rag";
    private int dimensions = 1024;
    private int searchLimit = 10;
    private double scoreThreshold = 0.5;
    private final Map<String, Object> additionalConfig = new HashMap<>();

    /**
     * Creates a new builder instance.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a VectorStoreConfiguration with default values.
     *
     * @return a default configuration
     */
    public static VectorStoreConfiguration defaultConfig() {
        return new VectorStoreConfiguration();
    }

    /**
     * Gets the vector store provider name (e.g., "memory", "qdrant", "chromadb").
     *
     * @return the provider name
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Gets the storage location (file path, URL, or ":memory:" for in-memory).
     *
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Gets the collection name for the vector store.
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
     * Gets the default search limit (maximum number of results).
     *
     * @return the search limit
     */
    public int getSearchLimit() {
        return searchLimit;
    }

    /**
     * Gets the default score threshold for filtering results.
     *
     * @return the score threshold (0.0 to 1.0)
     */
    public double getScoreThreshold() {
        return scoreThreshold;
    }

    /**
     * Gets additional provider-specific configuration.
     *
     * @return an unmodifiable map of additional configuration
     */
    public Map<String, Object> getAdditionalConfig() {
        return Collections.unmodifiableMap(additionalConfig);
    }

    /**
     * Builder for VectorStoreConfiguration.
     */
    public static class Builder {

        private final VectorStoreConfiguration config = new VectorStoreConfiguration();

        /**
         * Sets the vector store provider name.
         *
         * @param provider the provider name (e.g., "memory", "qdrant", "chromadb")
         * @return this builder for chaining
         * @throws IllegalArgumentException if provider is null or empty
         */
        public Builder provider(String provider) {
            if (provider == null || provider.trim().isEmpty()) {
                throw new IllegalArgumentException("Provider cannot be null or empty");
            }
            config.provider = provider;
            return this;
        }

        /**
         * Sets the storage location.
         *
         * @param location the location (file path, URL, or ":memory:")
         * @return this builder for chaining
         */
        public Builder location(String location) {
            config.location = location != null ? location : ":memory:";
            return this;
        }

        /**
         * Sets the collection name.
         *
         * @param collectionName the collection name
         * @return this builder for chaining
         */
        public Builder collectionName(String collectionName) {
            config.collectionName = collectionName != null ? collectionName : "agentscope_rag";
            return this;
        }

        /**
         * Sets the vector dimensions.
         *
         * @param dimensions the dimensions (must be positive)
         * @return this builder for chaining
         * @throws IllegalArgumentException if dimensions is not positive
         */
        public Builder dimensions(int dimensions) {
            if (dimensions <= 0) {
                throw new IllegalArgumentException("Dimensions must be positive");
            }
            config.dimensions = dimensions;
            return this;
        }

        /**
         * Sets the default search limit.
         *
         * @param limit the search limit (must be positive)
         * @return this builder for chaining
         * @throws IllegalArgumentException if limit is not positive
         */
        public Builder searchLimit(int limit) {
            if (limit <= 0) {
                throw new IllegalArgumentException("Search limit must be positive");
            }
            config.searchLimit = limit;
            return this;
        }

        /**
         * Sets the default score threshold.
         *
         * @param threshold the score threshold (must be between 0.0 and 1.0)
         * @return this builder for chaining
         * @throws IllegalArgumentException if threshold is out of range
         */
        public Builder scoreThreshold(double threshold) {
            if (threshold < 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException("Score threshold must be between 0.0 and 1.0");
            }
            config.scoreThreshold = threshold;
            return this;
        }

        /**
         * Adds an additional configuration entry.
         *
         * @param key the configuration key
         * @param value the configuration value
         * @return this builder for chaining
         */
        public Builder additionalConfig(String key, Object value) {
            if (key != null) {
                config.additionalConfig.put(key, value);
            }
            return this;
        }

        /**
         * Sets all additional configuration entries.
         *
         * @param additionalConfig the map of additional configuration
         * @return this builder for chaining
         */
        public Builder additionalConfig(Map<String, Object> additionalConfig) {
            if (additionalConfig != null) {
                config.additionalConfig.putAll(additionalConfig);
            }
            return this;
        }

        /**
         * Builds the VectorStoreConfiguration instance.
         *
         * @return the configured VectorStoreConfiguration
         */
        public VectorStoreConfiguration build() {
            return config;
        }
    }
}
