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
 * PDF reader implementation that reads and extracts text from PDF files.
 *
 * <p>This reader extracts text content from PDF files and chunks it according
 * to the specified strategy. It supports various text splitting strategies
 * similar to TextReader.
 *
 * <p>Note: This is a basic implementation that reads PDF files as text.
 * For full PDF parsing with proper text extraction, additional dependencies
 * (e.g., Apache PDFBox) would be required. This implementation provides a
 * foundation that can be extended.
 *
 * <p>Example usage:
 * <pre>{@code
 * PDFReader reader = new PDFReader(512, SplitStrategy.PARAGRAPH, 50);
 * ReaderInput input = ReaderInput.fromString("path/to/document.pdf");
 * List<Document> documents = reader.read(input).block();
 * }</pre>
 */
public class PDFReader implements Reader {

    private final int chunkSize;
    private final SplitStrategy splitStrategy;
    private final int overlapSize;
    private final boolean extractImages;

    /**
     * Creates a new PDFReader with the specified configuration.
     *
     * @param chunkSize the target size for each chunk
     * @param splitStrategy the strategy for splitting text
     * @param overlapSize the number of characters to overlap between chunks
     * @throws IllegalArgumentException if parameters are invalid
     */
    public PDFReader(int chunkSize, SplitStrategy splitStrategy, int overlapSize) {
        this(chunkSize, splitStrategy, overlapSize, false);
    }

    /**
     * Creates a new PDFReader with the specified configuration.
     *
     * @param chunkSize the target size for each chunk
     * @param splitStrategy the strategy for splitting text
     * @param overlapSize the number of characters to overlap between chunks
     * @param extractImages true to extract images from PDF (placeholder for future
     *     implementation)
     * @throws IllegalArgumentException if parameters are invalid
     */
    public PDFReader(
            int chunkSize, SplitStrategy splitStrategy, int overlapSize, boolean extractImages) {
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
        this.extractImages = extractImages;
    }

    /**
     * Creates a new PDFReader with default settings.
     *
     * <p>Defaults: chunkSize=512, strategy=PARAGRAPH, overlapSize=50
     */
    public PDFReader() {
        this(512, SplitStrategy.PARAGRAPH, 50, false);
    }

    @Override
    public Mono<List<Document>> read(ReaderInput input) throws ReaderException {
        if (input == null) {
            return Mono.error(new ReaderException("Input cannot be null"));
        }

        return Mono.fromCallable(
                        () -> {
                            try {
                                String pdfPath = input.asString();
                                String text = extractTextFromPDF(pdfPath);
                                List<String> chunks =
                                        TextChunker.chunkText(
                                                text, chunkSize, splitStrategy, overlapSize);
                                return createDocuments(chunks);
                            } catch (Exception e) {
                                throw new ReaderException("Failed to read PDF from: " + input, e);
                            }
                        })
                .onErrorMap(ReaderException.class, e -> e);
    }

    @Override
    public List<String> getSupportedFormats() {
        return List.of("pdf");
    }

    /**
     * Extracts text from a PDF file.
     *
     * <p>This is a basic implementation. For full PDF parsing, this would use
     * a library like Apache PDFBox. This implementation provides a foundation
     * that can be extended.
     *
     * @param pdfPath the path to the PDF file
     * @return extracted text content
     * @throws IOException if the file cannot be read
     * @throws ReaderException if PDF parsing fails
     */
    private String extractTextFromPDF(String pdfPath) throws IOException, ReaderException {
        // Basic implementation: Try to read as text file
        // Full implementation would use Apache PDFBox or similar library
        Path path = Paths.get(pdfPath);
        if (!Files.exists(path)) {
            throw new ReaderException("PDF file does not exist: " + pdfPath);
        }

        // Placeholder: Actual PDF text extraction would be implemented here
        // For now, throw an exception indicating that full PDF support requires
        // additional dependencies
        throw new ReaderException(
                "Full PDF text extraction requires additional dependencies (e.g., Apache PDFBox)."
                        + " Please use a PDF library to extract text and pass it to TextReader"
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

    /**
     * Checks if image extraction is enabled.
     *
     * @return true if image extraction is enabled
     */
    public boolean isExtractImages() {
        return extractImages;
    }
}
