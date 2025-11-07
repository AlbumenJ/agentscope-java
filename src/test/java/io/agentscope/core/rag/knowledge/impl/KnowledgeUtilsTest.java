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
package io.agentscope.core.rag.knowledge.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for KnowledgeUtils.
 */
@Tag("unit")
@DisplayName("KnowledgeUtils Unit Tests")
class KnowledgeUtilsTest {

    @Test
    @DisplayName("Should format documents")
    void testFormatDocuments() {
        Document doc1 = createDocument("doc1", "Content 1", 0.9);
        Document doc2 = createDocument("doc2", "Content 2", 0.8);

        String formatted = KnowledgeUtils.formatDocuments(List.of(doc1, doc2));

        assertTrue(formatted.contains("Retrieved 2 document(s)"));
        assertTrue(formatted.contains("Content 1"));
        assertTrue(formatted.contains("Content 2"));
        assertTrue(formatted.contains("0.900"));
        assertTrue(formatted.contains("0.800"));
    }

    @Test
    @DisplayName("Should format empty document list")
    void testFormatEmptyDocuments() {
        String formatted = KnowledgeUtils.formatDocuments(List.of());
        assertEquals("No documents found.", formatted);
    }

    @Test
    @DisplayName("Should format null document list")
    void testFormatNullDocuments() {
        String formatted = KnowledgeUtils.formatDocuments(null);
        assertEquals("No documents found.", formatted);
    }

    @Test
    @DisplayName("Should extract texts from documents")
    void testExtractTexts() {
        Document doc1 = createDocument("doc1", "Text 1", 0.9);
        Document doc2 = createDocument("doc2", "Text 2", 0.8);

        List<String> texts = KnowledgeUtils.extractTexts(List.of(doc1, doc2));

        assertEquals(2, texts.size());
        assertEquals("Text 1", texts.get(0));
        assertEquals("Text 2", texts.get(1));
    }

    @Test
    @DisplayName("Should extract texts from empty list")
    void testExtractTextsEmpty() {
        List<String> texts = KnowledgeUtils.extractTexts(List.of());
        assertTrue(texts.isEmpty());
    }

    @Test
    @DisplayName("Should extract texts from null list")
    void testExtractTextsNull() {
        List<String> texts = KnowledgeUtils.extractTexts(null);
        assertTrue(texts.isEmpty());
    }

    @Test
    @DisplayName("Should filter documents by score")
    void testFilterByScore() {
        Document doc1 = createDocument("doc1", "Content 1", 0.9);
        Document doc2 = createDocument("doc2", "Content 2", 0.6);
        Document doc3 = createDocument("doc3", "Content 3", 0.4);

        List<Document> filtered = KnowledgeUtils.filterByScore(List.of(doc1, doc2, doc3), 0.5);

        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(doc1));
        assertTrue(filtered.contains(doc2));
    }

    @Test
    @DisplayName("Should filter documents with null scores")
    void testFilterByScoreWithNullScores() {
        Document doc1 = createDocument("doc1", "Content 1", null);
        Document doc2 = createDocument("doc2", "Content 2", 0.6);

        List<Document> filtered = KnowledgeUtils.filterByScore(List.of(doc1, doc2), 0.5);

        assertEquals(1, filtered.size());
        assertTrue(filtered.contains(doc2));
    }

    @Test
    @DisplayName("Should limit documents")
    void testLimitDocuments() {
        Document doc1 = createDocument("doc1", "Content 1", 0.9);
        Document doc2 = createDocument("doc2", "Content 2", 0.8);
        Document doc3 = createDocument("doc3", "Content 3", 0.7);

        List<Document> limited = KnowledgeUtils.limitDocuments(List.of(doc1, doc2, doc3), 2);

        assertEquals(2, limited.size());
        assertEquals(doc1, limited.get(0));
        assertEquals(doc2, limited.get(1));
    }

    @Test
    @DisplayName("Should throw exception for invalid limit")
    void testLimitDocumentsInvalidLimit() {
        Document doc1 = createDocument("doc1", "Content 1", 0.9);

        assertThrows(
                IllegalArgumentException.class,
                () -> KnowledgeUtils.limitDocuments(List.of(doc1), 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> KnowledgeUtils.limitDocuments(List.of(doc1), -1));
    }

    @Test
    @DisplayName("Should combine texts from documents")
    void testCombineTexts() {
        Document doc1 = createDocument("doc1", "Text 1", 0.9);
        Document doc2 = createDocument("doc2", "Text 2", 0.8);

        String combined = KnowledgeUtils.combineTexts(List.of(doc1, doc2));

        assertTrue(combined.contains("Text 1"));
        assertTrue(combined.contains("Text 2"));
        assertTrue(combined.contains("\n\n"));
    }

    @Test
    @DisplayName("Should combine texts with custom separator")
    void testCombineTextsWithSeparator() {
        Document doc1 = createDocument("doc1", "Text 1", 0.9);
        Document doc2 = createDocument("doc2", "Text 2", 0.8);

        String combined = KnowledgeUtils.combineTexts(List.of(doc1, doc2), " | ");

        assertTrue(combined.contains("Text 1"));
        assertTrue(combined.contains("Text 2"));
        assertTrue(combined.contains(" | "));
    }

    @Test
    @DisplayName("Should return empty string for empty document list")
    void testCombineTextsEmpty() {
        String combined = KnowledgeUtils.combineTexts(List.of());
        assertEquals("", combined);
    }

    @Test
    @DisplayName("Should return empty string for null document list")
    void testCombineTextsNull() {
        String combined = KnowledgeUtils.combineTexts(null);
        assertEquals("", combined);
    }

    @Test
    @DisplayName("Should use default separator when null provided")
    void testCombineTextsNullSeparator() {
        Document doc1 = createDocument("doc1", "Text 1", 0.9);
        Document doc2 = createDocument("doc2", "Text 2", 0.8);

        String combined = KnowledgeUtils.combineTexts(List.of(doc1, doc2), null);

        assertTrue(combined.contains("\n\n"));
    }

    /**
     * Creates a test document.
     */
    private Document createDocument(String docId, String content, Double score) {
        Map<String, Object> contentMap = Map.of("text", content);
        DocumentMetadata metadata = new DocumentMetadata(contentMap, docId, 0, 1);
        Document doc = new Document(metadata);
        if (score != null) {
            doc.setScore(score);
        }
        return doc;
    }
}
