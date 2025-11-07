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
package io.agentscope.core.rag.model;

import java.util.Map;

/**
 * Document metadata containing content and chunking information.
 *
 * <p>This class stores metadata about a document chunk, including the content
 * (which can be text, image, or other types), document ID, chunk ID, and total
 * number of chunks.
 */
public class DocumentMetadata {

    private final Map<String, Object> content;
    private final String docId;
    private final int chunkId;
    private final int totalChunks;

    /**
     * Creates a new DocumentMetadata instance.
     *
     * @param content the content map (supports text, image, etc.)
     * @param docId the document ID
     * @param chunkId the chunk ID within the document
     * @param totalChunks the total number of chunks in the document
     */
    public DocumentMetadata(
            Map<String, Object> content, String docId, int chunkId, int totalChunks) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (docId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        if (chunkId < 0) {
            throw new IllegalArgumentException("Chunk ID must be non-negative");
        }
        if (totalChunks <= 0) {
            throw new IllegalArgumentException("Total chunks must be positive");
        }
        if (chunkId >= totalChunks) {
            throw new IllegalArgumentException("Chunk ID must be less than total chunks");
        }
        this.content = content;
        this.docId = docId;
        this.chunkId = chunkId;
        this.totalChunks = totalChunks;
    }

    /**
     * Gets the content map.
     *
     * @return the content map
     */
    public Map<String, Object> getContent() {
        return content;
    }

    /**
     * Gets the document ID.
     *
     * @return the document ID
     */
    public String getDocId() {
        return docId;
    }

    /**
     * Gets the chunk ID.
     *
     * @return the chunk ID
     */
    public int getChunkId() {
        return chunkId;
    }

    /**
     * Gets the total number of chunks.
     *
     * @return the total number of chunks
     */
    public int getTotalChunks() {
        return totalChunks;
    }

    /**
     * Gets the text content from the content map.
     *
     * <p>This is a convenience method that extracts the "text" key from the content map.
     *
     * @return the text content, or empty string if not present
     */
    public String getContentText() {
        Object textObj = content.get("text");
        return textObj != null ? textObj.toString() : "";
    }
}
