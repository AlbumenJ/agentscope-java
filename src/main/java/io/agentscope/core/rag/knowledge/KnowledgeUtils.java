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
package io.agentscope.core.rag.knowledge;

import io.agentscope.core.rag.model.Document;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for knowledge base operations.
 *
 * <p>This class provides helper methods for common operations on documents and
 * knowledge bases, such as formatting, filtering, and transformation.
 */
public final class KnowledgeUtils {

    private KnowledgeUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Formats a list of documents into a human-readable string.
     *
     * <p>This is useful for displaying retrieval results or logging.
     *
     * @param documents the documents to format
     * @return a formatted string representation
     */
    public static String formatDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "No documents found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Retrieved ").append(documents.size()).append(" document(s):\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            sb.append("Document ").append(i + 1);
            if (doc.getScore() != null) {
                sb.append(" (Score: ").append(String.format("%.3f", doc.getScore())).append(")");
            }
            sb.append(":\n");
            sb.append(doc.getMetadata().getContentText()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Extracts text content from a list of documents.
     *
     * @param documents the documents to extract text from
     * @return a list of text strings, one for each document
     */
    public static List<String> extractTexts(List<Document> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                .map(doc -> doc.getMetadata().getContentText())
                .collect(Collectors.toList());
    }

    /**
     * Filters documents by score threshold.
     *
     * @param documents the documents to filter
     * @param threshold the minimum score threshold
     * @return a filtered list of documents
     */
    public static List<Document> filterByScore(List<Document> documents, double threshold) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                .filter(doc -> doc.getScore() != null && doc.getScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * Limits the number of documents in a list.
     *
     * @param documents the documents to limit
     * @param limit the maximum number of documents to return
     * @return a limited list of documents
     */
    public static List<Document> limitDocuments(List<Document> documents, int limit) {
        if (documents == null) {
            return List.of();
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        return documents.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Combines text content from multiple documents into a single string.
     *
     * <p>Useful for creating context strings for LLM prompts.
     *
     * @param documents the documents to combine
     * @param separator the separator between document texts (default: "\n\n")
     * @return a combined text string
     */
    public static String combineTexts(List<Document> documents, String separator) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }
        String sep = separator != null ? separator : "\n\n";
        return documents.stream()
                .map(doc -> doc.getMetadata().getContentText())
                .collect(Collectors.joining(sep));
    }

    /**
     * Combines text content from multiple documents with default separator.
     *
     * @param documents the documents to combine
     * @return a combined text string separated by "\n\n"
     */
    public static String combineTexts(List<Document> documents) {
        return combineTexts(documents, "\n\n");
    }
}
