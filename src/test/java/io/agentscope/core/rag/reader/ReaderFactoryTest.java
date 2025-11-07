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
package io.agentscope.core.rag.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.rag.model.ReaderException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ReaderFactory.
 */
@Tag("unit")
@DisplayName("ReaderFactory Unit Tests")
class ReaderFactoryTest {

    @Test
    @DisplayName("Should create TextReader for text files")
    void testCreateTextReader() throws ReaderException {
        Reader reader = ReaderFactory.createReader("document.txt");
        assertNotNull(reader);
        assertTrue(reader instanceof TextReader);
        assertEquals(List.of("txt", "md", "rst"), reader.getSupportedFormats());
    }

    @Test
    @DisplayName("Should create ImageReader for image files")
    void testCreateImageReader() throws ReaderException {
        Reader reader = ReaderFactory.createReader("image.jpg");
        assertNotNull(reader);
        assertTrue(reader instanceof ImageReader);
    }

    @Test
    @DisplayName("Should create PDFReader for PDF files")
    void testCreatePDFReader() throws ReaderException {
        Reader reader = ReaderFactory.createReader("document.pdf");
        assertNotNull(reader);
        assertTrue(reader instanceof PDFReader);
    }

    @Test
    @DisplayName("Should create WordReader for Word files")
    void testCreateWordReader() throws ReaderException {
        Reader reader = ReaderFactory.createReader("document.docx");
        assertNotNull(reader);
        assertTrue(reader instanceof WordReader);
    }

    @Test
    @DisplayName("Should handle file paths with directories")
    void testCreateReaderWithPath() throws ReaderException {
        Reader reader = ReaderFactory.createReader("/path/to/document.pdf");
        assertNotNull(reader);
        assertTrue(reader instanceof PDFReader);
    }

    @Test
    @DisplayName("Should throw exception for unsupported format")
    void testUnsupportedFormat() {
        assertThrows(ReaderException.class, () -> ReaderFactory.createReader("document.xyz"));
    }

    @Test
    @DisplayName("Should throw exception for null file path")
    void testNullFilePath() {
        assertThrows(IllegalArgumentException.class, () -> ReaderFactory.createReader(null));
    }

    @Test
    @DisplayName("Should throw exception for empty file path")
    void testEmptyFilePath() {
        assertThrows(IllegalArgumentException.class, () -> ReaderFactory.createReader(""));
    }

    @Test
    @DisplayName("Should create reader with custom configuration")
    void testCreateReaderWithConfig() throws ReaderException {
        ReaderFactory.ReaderConfig config =
                ReaderFactory.ReaderConfig.builder()
                        .chunkSize(1024)
                        .splitStrategy(SplitStrategy.TOKEN)
                        .overlapSize(100)
                        .enableOCR(true)
                        .extractImages(true)
                        .build();

        Reader reader = ReaderFactory.createReader("document.txt", config);
        assertNotNull(reader);
        assertTrue(reader instanceof TextReader);
        TextReader textReader = (TextReader) reader;
        assertEquals(1024, textReader.getChunkSize());
        assertEquals(SplitStrategy.TOKEN, textReader.getSplitStrategy());
        assertEquals(100, textReader.getOverlapSize());
    }

    @Test
    @DisplayName("Should create ImageReader with OCR enabled")
    void testCreateImageReaderWithOCR() throws ReaderException {
        ReaderFactory.ReaderConfig config =
                ReaderFactory.ReaderConfig.builder().enableOCR(true).build();

        Reader reader = ReaderFactory.createReader("image.png", config);
        assertNotNull(reader);
        assertTrue(reader instanceof ImageReader);
        ImageReader imageReader = (ImageReader) reader;
        assertTrue(imageReader.isOcrEnabled());
    }

    @Test
    @DisplayName("Should get reader type for supported formats")
    void testGetReaderType() {
        Optional<ReaderFactory.ReaderType> type = ReaderFactory.getReaderType("txt");
        assertTrue(type.isPresent());
        assertEquals(ReaderFactory.ReaderType.TEXT, type.get());

        type = ReaderFactory.getReaderType("jpg");
        assertTrue(type.isPresent());
        assertEquals(ReaderFactory.ReaderType.IMAGE, type.get());

        type = ReaderFactory.getReaderType("pdf");
        assertTrue(type.isPresent());
        assertEquals(ReaderFactory.ReaderType.PDF, type.get());

        type = ReaderFactory.getReaderType("docx");
        assertTrue(type.isPresent());
        assertEquals(ReaderFactory.ReaderType.WORD, type.get());
    }

    @Test
    @DisplayName("Should return empty for unsupported format")
    void testGetReaderTypeUnsupported() {
        Optional<ReaderFactory.ReaderType> type = ReaderFactory.getReaderType("xyz");
        assertFalse(type.isPresent());
    }

    @Test
    @DisplayName("Should check if format is supported")
    void testIsFormatSupported() {
        assertTrue(ReaderFactory.isFormatSupported("txt"));
        assertTrue(ReaderFactory.isFormatSupported("jpg"));
        assertTrue(ReaderFactory.isFormatSupported("pdf"));
        assertTrue(ReaderFactory.isFormatSupported("docx"));
        assertFalse(ReaderFactory.isFormatSupported("xyz"));
    }

    @Test
    @DisplayName("Should handle case-insensitive extensions")
    void testCaseInsensitiveExtensions() throws ReaderException {
        Reader reader1 = ReaderFactory.createReader("document.TXT");
        Reader reader2 = ReaderFactory.createReader("document.txt");
        assertTrue(reader1 instanceof TextReader);
        assertTrue(reader2 instanceof TextReader);
    }

    @Test
    @DisplayName("Should create ReaderConfig with default values")
    void testDefaultReaderConfig() {
        ReaderFactory.ReaderConfig config = ReaderFactory.ReaderConfig.defaultConfig();
        assertEquals(512, config.getChunkSize());
        assertEquals(SplitStrategy.PARAGRAPH, config.getSplitStrategy());
        assertEquals(50, config.getOverlapSize());
        assertFalse(config.isEnableOCR());
        assertFalse(config.isExtractImages());
    }

    @Test
    @DisplayName("Should validate ReaderConfig builder")
    void testReaderConfigBuilder() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ReaderFactory.ReaderConfig.builder().chunkSize(0).build());

        assertThrows(
                IllegalArgumentException.class,
                () -> ReaderFactory.ReaderConfig.builder().splitStrategy(null).build());

        assertThrows(
                IllegalArgumentException.class,
                () -> ReaderFactory.ReaderConfig.builder().overlapSize(-1).build());
    }
}
