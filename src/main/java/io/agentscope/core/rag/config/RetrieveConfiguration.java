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
 * Configuration for document retrieval operations.
 *
 * <p>This class provides default configuration for retrieval operations,
 * including result limits, score thresholds, and reranking settings.
 *
 * <p>Note: This is different from {@link io.agentscope.core.rag.model.RetrieveConfig},
 * which is used for individual retrieval requests. This class provides
 * default values for the configuration system.
 *
 * <p>Example usage:
 * <pre>{@code
 * RetrieveConfiguration config = RetrieveConfiguration.builder()
 *     .limit(5)
 *     .scoreThreshold(0.5)
 *     .enableReranking(false)
 *     .build();
 * }</pre>
 */
public class RetrieveConfiguration {

    private int limit = 5;
    private double scoreThreshold = 0.5;
    private boolean enableReranking = false;
    private String reranker;

    /**
     * Creates a new builder instance.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a RetrieveConfiguration with default values.
     *
     * @return a default configuration
     */
    public static RetrieveConfiguration defaultConfig() {
        return new RetrieveConfiguration();
    }

    /**
     * Gets the default limit for retrieval results.
     *
     * @return the limit
     */
    public int getLimit() {
        return limit;
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
     * Checks if reranking is enabled.
     *
     * @return true if reranking is enabled
     */
    public boolean isRerankingEnabled() {
        return enableReranking;
    }

    /**
     * Gets the reranker name (if reranking is enabled).
     *
     * @return the reranker name, or null if not set
     */
    public String getReranker() {
        return reranker;
    }

    /**
     * Builder for RetrieveConfiguration.
     */
    public static class Builder {

        private final RetrieveConfiguration config = new RetrieveConfiguration();

        /**
         * Sets the default limit for retrieval results.
         *
         * @param limit the limit (must be positive)
         * @return this builder for chaining
         * @throws IllegalArgumentException if limit is not positive
         */
        public Builder limit(int limit) {
            if (limit <= 0) {
                throw new IllegalArgumentException("Limit must be positive");
            }
            config.limit = limit;
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
         * Enables or disables reranking.
         *
         * @param enable true to enable reranking
         * @return this builder for chaining
         */
        public Builder enableReranking(boolean enable) {
            config.enableReranking = enable;
            return this;
        }

        /**
         * Sets the reranker name.
         *
         * @param reranker the reranker name
         * @return this builder for chaining
         */
        public Builder reranker(String reranker) {
            config.reranker = reranker;
            return this;
        }

        /**
         * Builds the RetrieveConfiguration instance.
         *
         * @return the configured RetrieveConfiguration
         */
        public RetrieveConfiguration build() {
            return config;
        }
    }
}
