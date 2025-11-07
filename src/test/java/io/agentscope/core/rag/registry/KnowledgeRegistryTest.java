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
package io.agentscope.core.rag.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.rag.KnowledgeBase;
import io.agentscope.core.rag.hook.GenericRAGHook;
import io.agentscope.core.rag.knowledge.impl.SimpleKnowledge;
import io.agentscope.core.rag.store.impl.InMemoryStore;
import io.agentscope.core.tool.Toolkit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 * Unit tests for KnowledgeRegistry.
 */
@Tag("unit")
@DisplayName("KnowledgeRegistry Unit Tests")
class KnowledgeRegistryTest {

    private static final int DIMENSIONS = 3;

    private KnowledgeRegistry registry;
    private KnowledgeBase kb1;
    private KnowledgeBase kb2;

    @BeforeEach
    void setUp() {
        registry = new KnowledgeRegistry();
        TestMockEmbeddingModel embeddingModel1 = new TestMockEmbeddingModel(DIMENSIONS);
        TestMockEmbeddingModel embeddingModel2 = new TestMockEmbeddingModel(DIMENSIONS);
        InMemoryStore store1 = new InMemoryStore(DIMENSIONS);
        InMemoryStore store2 = new InMemoryStore(DIMENSIONS);
        kb1 = new SimpleKnowledge(embeddingModel1, store1);
        kb2 = new SimpleKnowledge(embeddingModel2, store2);
    }

    @Test
    @DisplayName("Should create empty registry")
    void testCreate() {
        KnowledgeRegistry newRegistry = new KnowledgeRegistry();
        assertNotNull(newRegistry);
        assertTrue(newRegistry.isEmpty());
        assertEquals(0, newRegistry.size());
    }

    @Test
    @DisplayName("Should register knowledge base")
    void testRegisterKnowledge() {
        registry.registerKnowledge("kb1", kb1, "Knowledge base 1");

        assertTrue(registry.hasKnowledge("kb1"));
        assertEquals(1, registry.size());
        assertEquals("Knowledge base 1", registry.getDescription("kb1"));
    }

    @Test
    @DisplayName("Should register knowledge base without description")
    void testRegisterKnowledgeWithoutDescription() {
        registry.registerKnowledge("kb1", kb1);

        assertTrue(registry.hasKnowledge("kb1"));
        assertEquals("", registry.getDescription("kb1"));
    }

    @Test
    @DisplayName("Should throw exception for null name")
    void testRegisterKnowledgeNullName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.registerKnowledge(null, kb1, "description"));
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.registerKnowledge("", kb1, "description"));
    }

    @Test
    @DisplayName("Should throw exception for null knowledge base")
    void testRegisterKnowledgeNullKB() {
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.registerKnowledge("kb1", null, "description"));
    }

    @Test
    @DisplayName("Should get knowledge base by name")
    void testGetKnowledge() {
        registry.registerKnowledge("kb1", kb1, "Description");

        KnowledgeBase retrieved = registry.getKnowledge("kb1");
        assertEquals(kb1, retrieved);
    }

    @Test
    @DisplayName("Should return null for non-existent knowledge base")
    void testGetKnowledgeNotFound() {
        assertNull(registry.getKnowledge("nonexistent"));
    }

    @Test
    @DisplayName("Should get all knowledge base names")
    void testGetKnowledgeNames() {
        registry.registerKnowledge("kb1", kb1, "Description 1");
        registry.registerKnowledge("kb2", kb2, "Description 2");

        Set<String> names = registry.getKnowledgeNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("kb1"));
        assertTrue(names.contains("kb2"));
    }

    @Test
    @DisplayName("Should unregister knowledge base")
    void testUnregisterKnowledge() {
        registry.registerKnowledge("kb1", kb1, "Description");
        assertEquals(1, registry.size());

        KnowledgeBase removed = registry.unregisterKnowledge("kb1");
        assertEquals(kb1, removed);
        assertEquals(0, registry.size());
        assertFalse(registry.hasKnowledge("kb1"));
    }

    @Test
    @DisplayName("Should return null when unregistering non-existent knowledge base")
    void testUnregisterKnowledgeNotFound() {
        assertNull(registry.unregisterKnowledge("nonexistent"));
    }

    @Test
    @DisplayName("Should clear all knowledge bases")
    void testClear() {
        registry.registerKnowledge("kb1", kb1, "Description 1");
        registry.registerKnowledge("kb2", kb2, "Description 2");
        assertEquals(2, registry.size());

        registry.clear();

        assertTrue(registry.isEmpty());
        assertEquals(0, registry.size());
    }

    @Test
    @DisplayName("Should inject knowledge tools into toolkit")
    void testInjectKnowledgeTools() {
        registry.registerKnowledge("kb1", kb1, "Description 1");
        registry.registerKnowledge("kb2", kb2, "Description 2");

        Toolkit toolkit = new Toolkit();
        registry.injectKnowledgeTools(toolkit);

        // Verify tools are registered
        Set<String> toolNames = toolkit.getToolNames();
        assertTrue(toolNames.contains("retrieve_knowledge"));
    }

    @Test
    @DisplayName("Should throw exception for null toolkit")
    void testInjectKnowledgeToolsNullToolkit() {
        registry.registerKnowledge("kb1", kb1, "Description");

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.injectKnowledgeTools((Toolkit) null));
    }

    @Test
    @DisplayName("Should inject specific knowledge tool")
    void testInjectKnowledgeTool() {
        registry.registerKnowledge("kb1", kb1, "Description 1");
        registry.registerKnowledge("kb2", kb2, "Description 2");

        Toolkit toolkit = new Toolkit();
        registry.injectKnowledgeTool(toolkit, "kb1");

        // Verify tool is registered
        Set<String> toolNames = toolkit.getToolNames();
        assertTrue(toolNames.contains("retrieve_knowledge"));
    }

    @Test
    @DisplayName("Should throw exception for non-existent knowledge base when injecting tool")
    void testInjectKnowledgeToolNotFound() {
        Toolkit toolkit = new Toolkit();

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.injectKnowledgeTool(toolkit, "nonexistent"));
    }

    @Test
    @DisplayName("Should create GenericRAGHook for specific knowledge base")
    void testCreateGenericRAGHook() {
        registry.registerKnowledge("kb1", kb1, "Description");

        GenericRAGHook hook = registry.createGenericRAGHook("kb1");

        assertNotNull(hook);
        assertEquals(kb1, hook.getKnowledgeBase());
    }

    @Test
    @DisplayName("Should throw exception for non-existent knowledge base when creating hook")
    void testCreateGenericRAGHookNotFound() {
        assertThrows(
                IllegalArgumentException.class, () -> registry.createGenericRAGHook("nonexistent"));
    }

    @Test
    @DisplayName("Should create GenericRAGHook with composite knowledge base")
    void testCreateGenericRAGHookComposite() {
        registry.registerKnowledge("kb1", kb1, "Description 1");
        registry.registerKnowledge("kb2", kb2, "Description 2");

        GenericRAGHook hook = registry.createGenericRAGHook();

        assertNotNull(hook);
        // Hook should use CompositeKnowledgeBase
        assertNotNull(hook.getKnowledgeBase());
    }

    @Test
    @DisplayName("Should throw exception when creating composite hook with no knowledge bases")
    void testCreateGenericRAGHookEmpty() {
        assertThrows(IllegalStateException.class, () -> registry.createGenericRAGHook());
    }

    @Test
    @DisplayName("Should inject knowledge tools into agent")
    void testInjectKnowledgeToolsAgent() {
        registry.registerKnowledge("kb1", kb1, "Description");

        ReActAgent agent =
                ReActAgent.builder()
                        .name("TestAgent")
                        .model(new io.agentscope.core.agent.test.MockModel("Response"))
                        .toolkit(new Toolkit())
                        .build();

        registry.injectKnowledgeTools(agent);

        // Verify tools are registered
        Toolkit toolkit = agent.getToolkit();
        assertNotNull(toolkit);
        Set<String> toolNames = toolkit.getToolNames();
        assertTrue(toolNames.contains("retrieve_knowledge"));
    }

    @Test
    @DisplayName("Should throw exception for null agent")
    void testInjectKnowledgeToolsNullAgent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.injectKnowledgeTools((ReActAgent) null));
    }

    @Test
    @DisplayName("Should inject specific knowledge tool into agent")
    void testInjectKnowledgeToolAgent() {
        registry.registerKnowledge("kb1", kb1, "Description");

        ReActAgent agent =
                ReActAgent.builder()
                        .name("TestAgent")
                        .model(new io.agentscope.core.agent.test.MockModel("Response"))
                        .toolkit(new Toolkit())
                        .build();

        registry.injectKnowledgeTool(agent, "kb1");

        // Verify tool is registered
        Toolkit toolkit = agent.getToolkit();
        assertNotNull(toolkit);
        Set<String> toolNames = toolkit.getToolNames();
        assertTrue(toolNames.contains("retrieve_knowledge"));
    }

    /**
     * Mock EmbeddingModel for testing.
     */
    private static class TestMockEmbeddingModel implements io.agentscope.core.rag.EmbeddingModel {
        private final int dimensions;
        private final Map<String, double[]> embeddings = new HashMap<>();

        TestMockEmbeddingModel(int dimensions) {
            this.dimensions = dimensions;
        }

        @Override
        public Mono<double[]> embed(io.agentscope.core.message.ContentBlock block) {
            if (block instanceof io.agentscope.core.message.TextBlock) {
                String text = ((io.agentscope.core.message.TextBlock) block).getText();
                return Mono.fromCallable(
                        () -> {
                            double[] embedding =
                                    embeddings.computeIfAbsent(text, k -> generateEmbedding(text));
                            return embedding.clone();
                        });
            }
            return Mono.error(new UnsupportedOperationException("Unsupported content block type"));
        }

        @Override
        public String getModelName() {
            return "mock-embedding-model";
        }

        @Override
        public int getDimensions() {
            return dimensions;
        }

        private double[] generateEmbedding(String text) {
            double[] embedding = new double[dimensions];
            int hash = text.hashCode();
            for (int i = 0; i < dimensions; i++) {
                embedding[i] = (double) ((hash + i) % 100) / 100.0;
            }
            double norm = 0.0;
            for (double v : embedding) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < dimensions; i++) {
                    embedding[i] /= norm;
                }
            }
            return embedding;
        }
    }
}
