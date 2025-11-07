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
package io.agentscope.core.rag.reader.impl;

import io.agentscope.core.rag.Reader;
import io.agentscope.core.rag.model.ReaderException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Factory class for creating appropriate Reader instances based on file format.
 *
 * <p>This factory automatically selects the correct Reader implementation based on
 * file extension or format. It supports text files, images, PDFs, and Word documents.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create reader based on file extension
 * Reader reader = ReaderFactory.createReader("document.pdf");
 *
 * // Create reader with custom configuration
 * Reader reader = ReaderFactory.createReader(
 *     "document.pdf",
 *     ReaderFactory.ReaderConfig.builder()
 *         .chunkSize(1024)
 *         .splitStrategy(SplitStrategy.TOKEN)
 *         .overlapSize(100)
 *         .build()
 * );
 * }</pre>
 */
public class ReaderFactory {

    private static final Map<String, ReaderType> FORMAT_TO_TYPE = new HashMap<>();

    static {
        // Text formats
        FORMAT_TO_TYPE.put("txt", ReaderType.TEXT);
        FORMAT_TO_TYPE.put("md", ReaderType.TEXT);
        FORMAT_TO_TYPE.put("rst", ReaderType.TEXT);
        FORMAT_TO_TYPE.put("markdown", ReaderType.TEXT);

        // Image formats
        FORMAT_TO_TYPE.put("jpg", ReaderType.IMAGE);
        FORMAT_TO_TYPE.put("jpeg", ReaderType.IMAGE);
        FORMAT_TO_TYPE.put("png", ReaderType.IMAGE);
        FORMAT_TO_TYPE.put("gif", ReaderType.IMAGE);
        FORMAT_TO_TYPE.put("bmp", ReaderType.IMAGE);
        FORMAT_TO_TYPE.put("tiff", ReaderType.IMAGE);
        FORMAT_TO_TYPE.put("webp", ReaderType.IMAGE);

        // PDF format
        FORMAT_TO_TYPE.put("pdf", ReaderType.PDF);

        // Word formats
        FORMAT_TO_TYPE.put("doc", ReaderType.WORD);
        FORMAT_TO_TYPE.put("docx", ReaderType.WORD);
    }

    /**
     * Creates a Reader instance based on file extension.
     *
     * @param filePath the path to the file (can be just filename or full path)
     * @return a Reader instance appropriate for the file format
     * @throws ReaderException if the file format is not supported
     */
    public static Reader createReader(String filePath) throws ReaderException {
        return createReader(filePath, ReaderConfig.defaultConfig());
    }

    /**
     * Creates a Reader instance based on file extension with custom configuration.
     *
     * @param filePath the path to the file (can be just filename or full path)
     * @param config the reader configuration
     * @return a Reader instance appropriate for the file format
     * @throws ReaderException if the file format is not supported
     */
    public static Reader createReader(String filePath, ReaderConfig config) throws ReaderException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (config == null) {
            config = ReaderConfig.defaultConfig();
        }

        String extension = extractExtension(filePath);
        ReaderType type = FORMAT_TO_TYPE.get(extension.toLowerCase(Locale.ROOT));

        if (type == null) {
            throw new ReaderException("Unsupported file format: " + extension);
        }

        return switch (type) {
            case TEXT ->
                    new TextReader(
                            config.getChunkSize(),
                            config.getSplitStrategy(),
                            config.getOverlapSize());
            case IMAGE -> new ImageReader(config.isEnableOCR());
            case PDF ->
                    new PDFReader(
                            config.getChunkSize(),
                            config.getSplitStrategy(),
                            config.getOverlapSize(),
                            config.isExtractImages());
            case WORD ->
                    new WordReader(
                            config.getChunkSize(),
                            config.getSplitStrategy(),
                            config.getOverlapSize());
        };
    }

    /**
     * Extracts the file extension from a file path.
     *
     * @param filePath the file path
     * @return the file extension (without dot), or empty string if no extension
     */
    private static String extractExtension(String filePath) {
        if (filePath == null) {
            return "";
        }

        int lastDot = filePath.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filePath.length() - 1) {
            return "";
        }

        return filePath.substring(lastDot + 1);
    }

    /**
     * Gets the Reader type for a given file extension.
     *
     * @param extension the file extension (without dot)
     * @return the Reader type, or empty if not supported
     */
    public static Optional<ReaderType> getReaderType(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        ReaderType type = FORMAT_TO_TYPE.get(extension.toLowerCase(Locale.ROOT));
        return Optional.ofNullable(type);
    }

    /**
     * Checks if a file format is supported.
     *
     * @param extension the file extension (without dot)
     * @return true if the format is supported
     */
    public static boolean isFormatSupported(String extension) {
        return getReaderType(extension).isPresent();
    }

    /**
     * Reader type enumeration.
     */
    public enum ReaderType {
        TEXT,
        IMAGE,
        PDF,
        WORD
    }

    /**
     * Configuration class for Reader creation.
     */
    public static class ReaderConfig {

        private int chunkSize = 512;
        private SplitStrategy splitStrategy = SplitStrategy.PARAGRAPH;
        private int overlapSize = 50;
        private boolean enableOCR = false;
        private boolean extractImages = false;

        /**
         * Creates a new builder instance.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Creates a ReaderConfig with default values.
         *
         * @return a default configuration
         */
        public static ReaderConfig defaultConfig() {
            return new ReaderConfig();
        }

        /**
         * Gets the chunk size.
         *
         * @return the chunk size
         */
        public int getChunkSize() {
            return chunkSize;
        }

        /**
         * Gets the split strategy.
         *
         * @return the split strategy
         */
        public SplitStrategy getSplitStrategy() {
            return splitStrategy;
        }

        /**
         * Gets the overlap size.
         *
         * @return the overlap size
         */
        public int getOverlapSize() {
            return overlapSize;
        }

        /**
         * Checks if OCR is enabled.
         *
         * @return true if OCR is enabled
         */
        public boolean isEnableOCR() {
            return enableOCR;
        }

        /**
         * Checks if image extraction is enabled.
         *
         * @return true if image extraction is enabled
         */
        public boolean isExtractImages() {
            return extractImages;
        }

        /**
         * Builder for ReaderConfig.
         */
        public static class Builder {

            private final ReaderConfig config = new ReaderConfig();

            /**
             * Sets the chunk size.
             *
             * @param chunkSize the chunk size (must be positive)
             * @return this builder for chaining
             */
            public Builder chunkSize(int chunkSize) {
                if (chunkSize <= 0) {
                    throw new IllegalArgumentException("Chunk size must be positive");
                }
                config.chunkSize = chunkSize;
                return this;
            }

            /**
             * Sets the split strategy.
             *
             * @param splitStrategy the split strategy (must not be null)
             * @return this builder for chaining
             */
            public Builder splitStrategy(SplitStrategy splitStrategy) {
                if (splitStrategy == null) {
                    throw new IllegalArgumentException("Split strategy cannot be null");
                }
                config.splitStrategy = splitStrategy;
                return this;
            }

            /**
             * Sets the overlap size.
             *
             * @param overlapSize the overlap size (must be non-negative)
             * @return this builder for chaining
             */
            public Builder overlapSize(int overlapSize) {
                if (overlapSize < 0) {
                    throw new IllegalArgumentException("Overlap size cannot be negative");
                }
                config.overlapSize = overlapSize;
                return this;
            }

            /**
             * Enables or disables OCR.
             *
             * @param enableOCR true to enable OCR
             * @return this builder for chaining
             */
            public Builder enableOCR(boolean enableOCR) {
                config.enableOCR = enableOCR;
                return this;
            }

            /**
             * Enables or disables image extraction.
             *
             * @param extractImages true to enable image extraction
             * @return this builder for chaining
             */
            public Builder extractImages(boolean extractImages) {
                config.extractImages = extractImages;
                return this;
            }

            /**
             * Builds the ReaderConfig instance.
             *
             * @return the configured ReaderConfig
             */
            public ReaderConfig build() {
                return config;
            }
        }
    }
}
