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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Document class representing a document chunk in the RAG system.
 *
 * <p>This is the core data structure for RAG operations. Each document contains
 * metadata, an optional embedding vector, and an optional similarity score.
 */
public class Document {

    private final String id;
    private final DocumentMetadata metadata;
    private double[] embedding;
    private Double score;

    /**
     * Creates a new Document instance.
     *
     * <p>The document ID is automatically generated based on the content hash of the metadata.
     *
     * @param metadata the document metadata
     */
    public Document(DocumentMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        this.metadata = metadata;
        this.id = generateDocumentId(metadata);
    }

    /**
     * Gets the document ID.
     *
     * @return the document ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the document metadata.
     *
     * @return the document metadata
     */
    public DocumentMetadata getMetadata() {
        return metadata;
    }

    /**
     * Gets the embedding vector.
     *
     * @return the embedding vector, or null if not set
     */
    public double[] getEmbedding() {
        return embedding;
    }

    /**
     * Sets the embedding vector.
     *
     * @param embedding the embedding vector
     */
    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
    }

    /**
     * Gets the similarity score.
     *
     * @return the similarity score, or null if not set
     */
    public Double getScore() {
        return score;
    }

    /**
     * Sets the similarity score.
     *
     * @param score the similarity score
     */
    public void setScore(Double score) {
        this.score = score;
    }

    /**
     * Generates a document ID based on the content hash.
     *
     * <p>Uses SHA-256 hash of the document text content to generate a unique ID.
     *
     * @param metadata the document metadata
     * @return the generated document ID
     */
    private String generateDocumentId(DocumentMetadata metadata) {
        try {
            String contentText = metadata.getContentText();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contentText.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 should always be available
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes the byte array
     * @return the hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    public String toString() {
        return String.format(
                "Document(id=%s, score=%s, content=%s)",
                id,
                score != null ? String.format("%.3f", score) : "null",
                metadata.getContentText());
    }
}
