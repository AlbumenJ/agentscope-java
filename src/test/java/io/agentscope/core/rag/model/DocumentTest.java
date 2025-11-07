/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Document.
 */
@Tag("unit")
@DisplayName("Document Unit Tests")
class DocumentTest {

    @Test
    @DisplayName("Should create Document with metadata")
    void testCreateDocument() {
        Map<String, Object> content = Map.of("text", "Test content");
        DocumentMetadata metadata = new DocumentMetadata(content, "doc-1", 0, 1);

        Document document = new Document(metadata);

        assertNotNull(document.getId());
        assertEquals(metadata, document.getMetadata());
        assertNull(document.getEmbedding());
        assertNull(document.getScore());
    }

    @Test
    @DisplayName("Should throw exception when metadata is null")
    void testCreateDocumentNullMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new Document(null));
    }

    @Test
    @DisplayName("Should generate consistent ID for same content")
    void testDocumentIdConsistency() {
        Map<String, Object> content = Map.of("text", "Test content");
        DocumentMetadata metadata1 = new DocumentMetadata(content, "doc-1", 0, 1);
        DocumentMetadata metadata2 = new DocumentMetadata(content, "doc-1", 0, 1);

        Document doc1 = new Document(metadata1);
        Document doc2 = new Document(metadata2);

        assertEquals(doc1.getId(), doc2.getId());
    }

    @Test
    @DisplayName("Should generate different IDs for different content")
    void testDocumentIdUniqueness() {
        Map<String, Object> content1 = Map.of("text", "Test content 1");
        Map<String, Object> content2 = Map.of("text", "Test content 2");
        DocumentMetadata metadata1 = new DocumentMetadata(content1, "doc-1", 0, 1);
        DocumentMetadata metadata2 = new DocumentMetadata(content2, "doc-1", 0, 1);

        Document doc1 = new Document(metadata1);
        Document doc2 = new Document(metadata2);

        // IDs should be different for different content
        // Note: This test might occasionally fail if hash collision occurs (extremely rare)
        // In practice, SHA-256 collisions are virtually impossible
        // Verify that IDs are different
        assertEquals(false, doc1.getId().equals(doc2.getId()));
    }

    @Test
    @DisplayName("Should set and get embedding")
    void testSetGetEmbedding() {
        Map<String, Object> content = Map.of("text", "Test content");
        DocumentMetadata metadata = new DocumentMetadata(content, "doc-1", 0, 1);
        Document document = new Document(metadata);

        double[] embedding = new double[] {0.1, 0.2, 0.3};
        document.setEmbedding(embedding);

        assertEquals(embedding, document.getEmbedding());
    }

    @Test
    @DisplayName("Should set and get score")
    void testSetGetScore() {
        Map<String, Object> content = Map.of("text", "Test content");
        DocumentMetadata metadata = new DocumentMetadata(content, "doc-1", 0, 1);
        Document document = new Document(metadata);

        Double score = 0.95;
        document.setScore(score);

        assertEquals(score, document.getScore());
    }

    @Test
    @DisplayName("Should generate valid SHA-256 hash ID")
    void testDocumentIdFormat() {
        Map<String, Object> content = Map.of("text", "Test content");
        DocumentMetadata metadata = new DocumentMetadata(content, "doc-1", 0, 1);
        Document document = new Document(metadata);

        // SHA-256 produces 64-character hex string
        assertEquals(64, document.getId().length());
        // Should only contain hex characters
        assertEquals(true, document.getId().matches("[0-9a-f]{64}"));
    }

    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        Map<String, Object> content = Map.of("text", "Test content");
        DocumentMetadata metadata = new DocumentMetadata(content, "doc-1", 0, 1);
        Document document = new Document(metadata);
        document.setScore(0.95);

        String str = document.toString();
        assertEquals(true, str.contains("Document"));
        assertEquals(true, str.contains("Test content"));
        assertEquals(true, str.contains("0.950"));
    }
}
