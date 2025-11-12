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
package io.agentscope.examples;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.rag.embedding.EmbeddingModel;
import io.agentscope.core.rag.knowledge.Knowledge;
import io.agentscope.core.rag.store.VDBStoreBase;
import io.agentscope.core.rag.embedding.DashScopeTextEmbedding;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.ReaderInput;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.rag.registry.KnowledgeRegistry;
import io.agentscope.core.rag.store.InMemoryStore;
import io.agentscope.core.rag.store.QdrantStore;
import io.agentscope.core.tool.Toolkit;
import java.io.IOException;
import java.util.List;

/**
 * RAGExample - Demonstrates Retrieval-Augmented Generation (RAG) capabilities.
 *
 * <p>This example shows:
 *
 * <ul>
 *   <li>Creating a knowledge base with embedding model and vector store
 *   <li>Adding documents to the knowledge base
 *   <li>Using Generic RAG mode (automatic knowledge injection via Hook)
 *   <li>Using Agentic RAG mode (agent decides when to retrieve via Tools)
 *   <li>Managing multiple knowledge bases with KnowledgeRegistry
 * </ul>
 *
 * <p>Run:
 *
 * <pre>
 * mvn exec:java -Dexec.mainClass="io.agentscope.examples.RAGExample"
 * </pre>
 *
 * <p>Requirements:
 *
 * <ul>
 *   <li>DASHSCOPE_API_KEY environment variable (or interactive input)
 * </ul>
 *
 * <p>This example demonstrates two RAG integration modes:
 *
 * <ol>
 *   <li><b>Generic Mode</b>: Knowledge is automatically retrieved and injected before reasoning
 *   <li><b>Agentic Mode</b>: Agent decides when to retrieve knowledge using tools
 * </ol>
 */
public class RAGExample {

    private static final int EMBEDDING_DIMENSIONS = 1024;

    public static void main(String[] args) throws Exception {
        // Print welcome message
        ExampleUtils.printWelcome(
                "RAG (Retrieval-Augmented Generation) Example",
                "This example demonstrates RAG capabilities:\n"
                        + "  - Creating and populating knowledge bases\n"
                        + "  - Generic mode: Automatic knowledge injection\n"
                        + "  - Agentic mode: Agent-controlled knowledge retrieval\n"
                        + "  - Managing multiple knowledge bases");

        // Get API key
        String apiKey = ExampleUtils.getDashScopeApiKey();

        // Create embedding model
        System.out.println("Creating embedding model...");
        EmbeddingModel embeddingModel =
                DashScopeTextEmbedding.builder()
                        .apiKey(apiKey)
                        .modelName("text-embedding-v3")
                        .dimensions(EMBEDDING_DIMENSIONS)
                        .build();
        System.out.println("✓ Embedding model created\n");

        // Create vector store
        System.out.println("Creating vector store...");
        VDBStoreBase vectorStore = QdrantStore.builder().location("http://47.57.181.116:6333")
                .collectionName("test_collection" + System.currentTimeMillis())
                .dimensions(1024)
                .useTransportLayerSecurity(false)
                .build();
        System.out.println("✓ Vector store created\n");

        // Create knowledge base
        System.out.println("Creating knowledge base...");
        Knowledge knowledge = new SimpleKnowledge(embeddingModel, vectorStore);
        System.out.println("✓ Knowledge base created\n");

        // Add documents to knowledge base
        System.out.println("Adding documents to knowledge base...");
        addSampleDocuments(knowledge);
        System.out.println("✓ Documents added\n");

        // Demonstrate Generic Mode
        System.out.println("=== Generic RAG Mode ===");
        System.out.println(
                "In Generic mode, knowledge is automatically retrieved and injected\n"
                        + "before each reasoning step. The agent doesn't need to explicitly\n"
                        + "call retrieval tools.\n");
        demonstrateGenericMode(apiKey, knowledge);

        // Demonstrate Agentic Mode
        System.out.println("\n=== Agentic RAG Mode ===");
        System.out.println(
                "In Agentic mode, the agent decides when to retrieve knowledge\n"
                        + "using the retrieve_knowledge tool. This gives the agent more\n"
                        + "control over when to use knowledge.\n");
        demonstrateAgenticMode(apiKey, knowledge);

        // Demonstrate KnowledgeRegistry
        System.out.println("\n=== Knowledge Registry ===");
        System.out.println(
                "KnowledgeRegistry allows you to manage multiple knowledge bases\n"
                        + "and easily inject them into agents.\n");
        demonstrateKnowledgeRegistry(apiKey, embeddingModel);

        System.out.println("\n=== All examples completed ===");
    }

    /**
     * Add sample documents to the knowledge base.
     *
     * @param knowledge the knowledge base to add documents to
     */
    private static void addSampleDocuments(Knowledge knowledge) {
        // Sample documents about AgentScope
        String[] documents = {
            "AgentScope is a multi-agent system framework developed by ModelScope. It provides a"
                    + " unified interface for building and managing multi-agent applications."
                    + " AgentScope supports both synchronous and asynchronous agent communication.",
            "AgentScope supports various agent types including ReActAgent, which implements the "
                    + "ReAct (Reasoning and Acting) algorithm. ReActAgent combines reasoning and "
                    + "acting in an iterative loop to solve complex tasks.",
            "RAG (Retrieval-Augmented Generation) is a technique that enhances language models by"
                    + " retrieving relevant information from a knowledge base before generating"
                    + " responses. This allows models to access up-to-date information and reduce"
                    + " hallucinations.",
            "AgentScope Java is the Java implementation of AgentScope framework. It provides "
                    + "reactive programming support using Project Reactor, making it suitable for "
                    + "building scalable multi-agent systems.",
            "Vector stores are used in RAG systems to store and search document embeddings. "
                    + "AgentScope supports in-memory vector stores and can integrate with external "
                    + "vector databases like Qdrant and ChromaDB."
        };

        // Create reader for text documents
        TextReader reader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);

        // Add each document
        for (int i = 0; i < documents.length; i++) {
            String docText = documents[i];
            ReaderInput input = ReaderInput.fromString(docText);

            try {
                List<Document> docs = reader.read(input).block();
                if (docs != null && !docs.isEmpty()) {
                    knowledge.addDocuments(docs).block();
                    System.out.println(
                            "  Added document "
                                    + (i + 1)
                                    + ": "
                                    + docText.substring(0, Math.min(50, docText.length()))
                                    + "...");
                }
            } catch (Exception e) {
                System.err.println("  Error adding document " + (i + 1) + ": " + e.getMessage());
            }
        }
    }

    /**
     * Demonstrate Generic RAG mode using GenericRAGHook.
     *
     * @param apiKey the API key for the chat model
     * @param knowledge the knowledge base to use
     */
    private static void demonstrateGenericMode(String apiKey, Knowledge knowledge)
            throws IOException {
        // Create Generic RAG Hook
        io.agentscope.core.rag.hook.GenericRAGHook ragHook =
                new io.agentscope.core.rag.hook.GenericRAGHook(
                        knowledge,
                        RetrieveConfig.builder().limit(3).scoreThreshold(0.3).build(),
                        true);

        // Create agent with Generic RAG Hook
        ReActAgent agent =
                ReActAgent.builder()
                        .name("RAGAssistant")
                        .sysPrompt(
                                "You are a helpful assistant with access to a knowledge base. Use"
                                    + " the provided knowledge to answer questions accurately. If"
                                    + " the knowledge doesn't contain relevant information, say so"
                                    + " clearly.")
                        .model(
                                DashScopeChatModel.builder()
                                        .apiKey(apiKey)
                                        .modelName("qwen-max")
                                        .stream(true)
                                        .enableThinking(false)
                                        .formatter(new DashScopeChatFormatter())
                                        .build())
                        .hook(ragHook)
                        .memory(new InMemoryMemory())
                        .toolkit(new Toolkit())
                        .build();

        System.out.println("Generic mode agent created. Try asking:");
        System.out.println("  - 'What is AgentScope?'");
        System.out.println("  - 'What is RAG?'");
        System.out.println("  - 'What vector stores does AgentScope support?'\n");

        // Start interactive chat
        ExampleUtils.startChat(agent);
    }

    /**
     * Demonstrate Agentic RAG mode using KnowledgeRetrievalTools.
     *
     * @param apiKey the API key for the chat model
     * @param knowledge the knowledge base to use
     */
    private static void demonstrateAgenticMode(String apiKey, Knowledge knowledge)
            throws IOException {
        // Create toolkit and register knowledge retrieval tools
        Toolkit toolkit = new Toolkit();
        io.agentscope.core.rag.tool.KnowledgeRetrievalTools retrievalTools =
                new io.agentscope.core.rag.tool.KnowledgeRetrievalTools(knowledge);
        toolkit.registerTool(retrievalTools);

        // Create agent with knowledge retrieval tools
        ReActAgent agent =
                ReActAgent.builder()
                        .name("RAGAgent")
                        .sysPrompt(
                                "You are a helpful assistant with access to a knowledge retrieval"
                                    + " tool. When you need information from the knowledge base,"
                                    + " use the retrieve_knowledge tool. Always explain what you're"
                                    + " doing.")
                        .model(
                                DashScopeChatModel.builder()
                                        .apiKey(apiKey)
                                        .modelName("qwen-max")
                                        .stream(true)
                                        .enableThinking(false)
                                        .formatter(new DashScopeChatFormatter())
                                        .build())
                        .toolkit(toolkit)
                        .memory(new InMemoryMemory())
                        .build();

        System.out.println("Agentic mode agent created. Try asking:");
        System.out.println("  - 'What is AgentScope?'");
        System.out.println("  - 'Tell me about RAG'");
        System.out.println("  - 'What is ReActAgent?'\n");

        // Start interactive chat
        ExampleUtils.startChat(agent);
    }

    /**
     * Demonstrate KnowledgeRegistry for managing multiple knowledge bases.
     *
     * @param apiKey the API key for the chat model
     * @param embeddingModel the embedding model to use
     */
    private static void demonstrateKnowledgeRegistry(String apiKey, EmbeddingModel embeddingModel)
            throws IOException {
        // Create multiple knowledge bases
        InMemoryStore store1 = new InMemoryStore(EMBEDDING_DIMENSIONS);
        Knowledge kb1 = new SimpleKnowledge(embeddingModel, store1);

        InMemoryStore store2 = new InMemoryStore(EMBEDDING_DIMENSIONS);
        Knowledge kb2 = new SimpleKnowledge(embeddingModel, store2);

        // Add different documents to each knowledge base
        addDocumentsToKB(
                kb1,
                "AgentScope is a multi-agent framework.",
                "ReActAgent implements the ReAct algorithm.");

        addDocumentsToKB(
                kb2,
                "RAG enhances language models with retrieval.",
                "Vector stores store document embeddings.");

        // Create registry and register knowledge bases
        KnowledgeRegistry registry = new KnowledgeRegistry();
        registry.registerKnowledge("agent_info", kb1, "Information about AgentScope agents");
        registry.registerKnowledge("rag_info", kb2, "Information about RAG technology");

        System.out.println("Registered knowledge bases:");
        System.out.println("  - agent_info: Information about AgentScope agents");
        System.out.println("  - rag_info: Information about RAG technology\n");

        // Create agent with Generic RAG Hook from registry
        ReActAgent agent =
                ReActAgent.builder()
                        .name("MultiKBAssistant")
                        .sysPrompt(
                                "You are a helpful assistant with access to multiple knowledge"
                                        + " bases. Use the provided knowledge to answer questions"
                                        + " accurately.")
                        .model(
                                DashScopeChatModel.builder()
                                        .apiKey(apiKey)
                                        .modelName("qwen-max")
                                        .stream(true)
                                        .enableThinking(false)
                                        .formatter(new DashScopeChatFormatter())
                                        .build())
                        .hook(registry.createGenericRAGHook()) // Uses all registered knowledge
                        // bases
                        .memory(new InMemoryMemory())
                        .toolkit(new Toolkit())
                        .build();

        System.out.println("Multi-knowledge base agent created. Try asking:");
        System.out.println("  - 'What is AgentScope?'");
        System.out.println("  - 'What is RAG?'");
        System.out.println("  - 'Tell me about ReActAgent'\n");

        // Start interactive chat
        ExampleUtils.startChat(agent);
    }

    /**
     * Helper method to add documents to a knowledge base.
     *
     * @param knowledge the knowledge base
     * @param documents the document texts
     */
    private static void addDocumentsToKB(Knowledge knowledge, String... documents) {
        TextReader reader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);

        for (String docText : documents) {
            ReaderInput input = ReaderInput.fromString(docText);
            try {
                List<Document> docs = reader.read(input).block();
                if (docs != null && !docs.isEmpty()) {
                    knowledge.addDocuments(docs).block();
                }
            } catch (Exception e) {
                System.err.println("Error adding document: " + e.getMessage());
            }
        }
    }
}

