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

import java.time.Duration;

/**
 * Configuration for embedding models.
 *
 * <p>This class provides configuration options for embedding model providers,
 * including provider type, model name, dimensions, timeout, caching, and API keys.
 *
 * <p>Example usage:
 * <pre>{@code
 * EmbeddingConfiguration config = EmbeddingConfiguration.builder()
 *     .provider("dashscope")
 *     .model("text-embedding-v3")
 *     .dimensions(1024)
 *     .timeout(Duration.ofSeconds(30))
 *     .enableCache(true)
 *     .cacheDirectory("./cache/embeddings")
 *     .apiKey(System.getenv("DASHSCOPE_API_KEY"))
 *     .build();
 * }</pre>
 */
public class EmbeddingConfiguration {

    private String provider = "dashscope";
    private String model = "text-embedding-v3";
    private int dimensions = 1024;
    private Duration timeout = Duration.ofSeconds(30);
    private boolean enableCache = false;
    private String cacheDirectory = "./cache/embeddings";
    private String apiKey;

    /**
     * Creates a new builder instance.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an EmbeddingConfiguration with default values.
     *
     * @return a default configuration
     */
    public static EmbeddingConfiguration defaultConfig() {
        return new EmbeddingConfiguration();
    }

    /**
     * Gets the embedding provider name (e.g., "dashscope", "openai").
     *
     * @return the provider name
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Gets the embedding model name.
     *
     * @return the model name
     */
    public String getModel() {
        return model;
    }

    /**
     * Gets the embedding vector dimensions.
     *
     * @return the dimensions
     */
    public int getDimensions() {
        return dimensions;
    }

    /**
     * Gets the timeout for embedding requests.
     *
     * @return the timeout duration
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Checks if embedding cache is enabled.
     *
     * @return true if cache is enabled
     */
    public boolean isCacheEnabled() {
        return enableCache;
    }

    /**
     * Gets the cache directory path.
     *
     * @return the cache directory path
     */
    public String getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Gets the API key for the embedding provider.
     *
     * @return the API key, or null if not set
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Builder for EmbeddingConfiguration.
     */
    public static class Builder {

        private final EmbeddingConfiguration config = new EmbeddingConfiguration();

        /**
         * Sets the embedding provider name.
         *
         * @param provider the provider name (e.g., "dashscope", "openai")
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
         * Sets the embedding model name.
         *
         * @param model the model name
         * @return this builder for chaining
         * @throws IllegalArgumentException if model is null or empty
         */
        public Builder model(String model) {
            if (model == null || model.trim().isEmpty()) {
                throw new IllegalArgumentException("Model cannot be null or empty");
            }
            config.model = model;
            return this;
        }

        /**
         * Sets the embedding vector dimensions.
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
         * Sets the timeout for embedding requests.
         *
         * @param timeout the timeout duration (must not be null)
         * @return this builder for chaining
         * @throws IllegalArgumentException if timeout is null
         */
        public Builder timeout(Duration timeout) {
            if (timeout == null) {
                throw new IllegalArgumentException("Timeout cannot be null");
            }
            config.timeout = timeout;
            return this;
        }

        /**
         * Enables or disables embedding cache.
         *
         * @param enable true to enable cache
         * @return this builder for chaining
         */
        public Builder enableCache(boolean enable) {
            config.enableCache = enable;
            return this;
        }

        /**
         * Sets the cache directory path.
         *
         * @param directory the cache directory path
         * @return this builder for chaining
         */
        public Builder cacheDirectory(String directory) {
            config.cacheDirectory = directory != null ? directory : "./cache/embeddings";
            return this;
        }

        /**
         * Sets the API key for the embedding provider.
         *
         * @param apiKey the API key
         * @return this builder for chaining
         */
        public Builder apiKey(String apiKey) {
            config.apiKey = apiKey;
            return this;
        }

        /**
         * Builds the EmbeddingConfiguration instance.
         *
         * @return the configured EmbeddingConfiguration
         */
        public EmbeddingConfiguration build() {
            return config;
        }
    }
}
