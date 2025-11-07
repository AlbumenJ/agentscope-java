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

import io.agentscope.core.rag.reader.impl.SplitStrategy;

/**
 * Configuration for document readers.
 *
 * <p>This class provides configuration options for document reading and chunking,
 * including chunk size, split strategy, overlap size, and image OCR settings.
 *
 * <p>Example usage:
 * <pre>{@code
 * ReaderConfiguration config = ReaderConfiguration.builder()
 *     .chunkSize(1024)
 *     .splitStrategy(SplitStrategy.TOKEN)
 *     .overlapSize(100)
 *     .enableImageOCR(true)
 *     .build();
 * }</pre>
 */
public class ReaderConfiguration {

    private int defaultChunkSize = 512;
    private SplitStrategy defaultSplitStrategy = SplitStrategy.PARAGRAPH;
    private int defaultOverlapSize = 50;
    private boolean enableImageOCR = false;

    /**
     * Creates a new builder instance.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a ReaderConfiguration with default values.
     *
     * @return a default configuration
     */
    public static ReaderConfiguration defaultConfig() {
        return new ReaderConfiguration();
    }

    /**
     * Gets the default chunk size for text splitting.
     *
     * @return the chunk size in characters
     */
    public int getDefaultChunkSize() {
        return defaultChunkSize;
    }

    /**
     * Gets the default split strategy.
     *
     * @return the split strategy
     */
    public SplitStrategy getDefaultSplitStrategy() {
        return defaultSplitStrategy;
    }

    /**
     * Gets the default overlap size between chunks.
     *
     * @return the overlap size in characters
     */
    public int getDefaultOverlapSize() {
        return defaultOverlapSize;
    }

    /**
     * Checks if image OCR is enabled.
     *
     * @return true if image OCR is enabled
     */
    public boolean isImageOCREnabled() {
        return enableImageOCR;
    }

    /**
     * Builder for ReaderConfiguration.
     */
    public static class Builder {

        private final ReaderConfiguration config = new ReaderConfiguration();

        /**
         * Sets the default chunk size for text splitting.
         *
         * @param chunkSize the chunk size (must be positive)
         * @return this builder for chaining
         * @throws IllegalArgumentException if chunkSize is not positive
         */
        public Builder chunkSize(int chunkSize) {
            if (chunkSize <= 0) {
                throw new IllegalArgumentException("Chunk size must be positive");
            }
            config.defaultChunkSize = chunkSize;
            return this;
        }

        /**
         * Sets the default split strategy.
         *
         * @param strategy the split strategy (must not be null)
         * @return this builder for chaining
         * @throws IllegalArgumentException if strategy is null
         */
        public Builder splitStrategy(SplitStrategy strategy) {
            if (strategy == null) {
                throw new IllegalArgumentException("Split strategy cannot be null");
            }
            config.defaultSplitStrategy = strategy;
            return this;
        }

        /**
         * Sets the default overlap size between chunks.
         *
         * @param overlapSize the overlap size (must be non-negative)
         * @return this builder for chaining
         * @throws IllegalArgumentException if overlapSize is negative
         */
        public Builder overlapSize(int overlapSize) {
            if (overlapSize < 0) {
                throw new IllegalArgumentException("Overlap size cannot be negative");
            }
            config.defaultOverlapSize = overlapSize;
            return this;
        }

        /**
         * Enables or disables image OCR.
         *
         * @param enable true to enable image OCR
         * @return this builder for chaining
         */
        public Builder enableImageOCR(boolean enable) {
            config.enableImageOCR = enable;
            return this;
        }

        /**
         * Builds the ReaderConfiguration instance.
         *
         * @return the configured ReaderConfiguration
         */
        public ReaderConfiguration build() {
            return config;
        }
    }
}
