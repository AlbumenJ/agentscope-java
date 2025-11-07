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
package io.agentscope.core.rag.reader;

import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.model.ReaderException;
import io.agentscope.core.rag.model.ReaderInput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Word document reader implementation that reads and extracts text from Word files.
 *
 * <p>This reader extracts text content from Word documents (.docx, .doc) and chunks it
 * according to the specified strategy. It supports various text splitting strategies
 * similar to TextReader.
 *
 * <p>Note: This is a basic implementation that reads Word files as text.
 * For full Word document parsing with proper text extraction, additional dependencies
 * (e.g., Apache POI) would be required. This implementation provides a foundation
 * that can be extended.
 *
 * <p>Example usage:
 * <pre>{@code
 * WordReader reader = new WordReader(512, SplitStrategy.PARAGRAPH, 50);
 * ReaderInput input = ReaderInput.fromString("path/to/document.docx");
 * List<Document> documents = reader.read(input).block();
 * }</pre>
 */
public class WordReader implements Reader {

    private final int chunkSize;
    private final SplitStrategy splitStrategy;
    private final int overlapSize;

    /**
     * Creates a new WordReader with the specified configuration.
     *
     * @param chunkSize the target size for each chunk
     * @param splitStrategy the strategy for splitting text
     * @param overlapSize the number of characters to overlap between chunks
     * @throws IllegalArgumentException if parameters are invalid
     */
    public WordReader(int chunkSize, SplitStrategy splitStrategy, int overlapSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (splitStrategy == null) {
            throw new IllegalArgumentException("Split strategy cannot be null");
        }
        if (overlapSize < 0) {
            throw new IllegalArgumentException("Overlap size cannot be negative");
        }
        if (overlapSize >= chunkSize) {
            throw new IllegalArgumentException("Overlap size must be less than chunk size");
        }

        this.chunkSize = chunkSize;
        this.splitStrategy = splitStrategy;
        this.overlapSize = overlapSize;
    }

    /**
     * Creates a new WordReader with default settings.
     *
     * <p>Defaults: chunkSize=512, strategy=PARAGRAPH, overlapSize=50
     */
    public WordReader() {
        this(512, SplitStrategy.PARAGRAPH, 50);
    }

    @Override
    public Mono<List<Document>> read(ReaderInput input) throws ReaderException {
        if (input == null) {
            return Mono.error(new ReaderException("Input cannot be null"));
        }

        return Mono.fromCallable(
                        () -> {
                            try {
                                String wordPath = input.asString();
                                String text = extractTextFromWord(wordPath);
                                List<String> chunks =
                                        TextChunker.chunkText(
                                                text, chunkSize, splitStrategy, overlapSize);
                                return createDocuments(chunks);
                            } catch (Exception e) {
                                throw new ReaderException(
                                        "Failed to read Word document from: " + input, e);
                            }
                        })
                .onErrorMap(ReaderException.class, e -> e);
    }

    @Override
    public List<String> getSupportedFormats() {
        return List.of("doc", "docx");
    }

    /**
     * Extracts text from a Word document file.
     *
     * <p>This is a basic implementation. For full Word document parsing, this would use
     * a library like Apache POI. This implementation provides a foundation that can be
     * extended.
     *
     * @param wordPath the path to the Word document file
     * @return extracted text content
     * @throws IOException if the file cannot be read
     * @throws ReaderException if Word document parsing fails
     */
    private String extractTextFromWord(String wordPath) throws IOException, ReaderException {
        // Basic implementation: Try to read as text file
        // Full implementation would use Apache POI or similar library
        Path path = Paths.get(wordPath);
        if (!Files.exists(path)) {
            throw new ReaderException("Word document file does not exist: " + wordPath);
        }

        // Placeholder: Actual Word document text extraction would be implemented here
        // For now, throw an exception indicating that full Word support requires
        // additional dependencies
        throw new ReaderException(
                "Full Word document text extraction requires additional dependencies (e.g., Apache"
                    + " POI). Please use a Word library to extract text and pass it to TextReader"
                    + " instead.");
    }

    /**
     * Creates Document objects from text chunks.
     *
     * @param chunks the list of text chunks
     * @return a list of Document objects
     */
    private List<Document> createDocuments(List<String> chunks) {
        String docId = UUID.randomUUID().toString();
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            io.agentscope.core.message.TextBlock content =
                    io.agentscope.core.message.TextBlock.builder().text(chunks.get(i)).build();
            DocumentMetadata metadata = new DocumentMetadata(content, docId, i, chunks.size());
            documents.add(new Document(metadata));
        }

        return documents;
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
}
