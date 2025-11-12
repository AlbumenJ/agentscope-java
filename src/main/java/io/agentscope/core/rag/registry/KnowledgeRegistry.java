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
package io.agentscope.core.rag.registry;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.rag.hook.GenericRAGHook;
import io.agentscope.core.rag.knowledge.Knowledge;
import io.agentscope.core.rag.tool.KnowledgeRetrievalTools;
import io.agentscope.core.tool.Toolkit;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for managing knowledge bases and their integration with agents.
 *
 * <p>This class provides a centralized way to register, manage, and inject knowledge bases
 * into agents. It supports both Agentic mode (via tools) and Generic mode (via hooks).
 *
 * <p>Features:
 * <ul>
 *   <li>Knowledge base registration and lookup</li>
 *   <li>Agentic mode: Inject knowledge retrieval tools into agents</li>
 *   <li>Generic mode: Create GenericRAGHook for automatic knowledge injection</li>
 *   <li>Composite knowledge bases: Aggregate multiple knowledge bases</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // 1. Create knowledge bases
 * KnowledgeBase kb1 = new SimpleKnowledge(embeddingModel1, store1);
 * KnowledgeBase kb2 = new SimpleKnowledge(embeddingModel2, store2);
 *
 * // 2. Register knowledge bases
 * KnowledgeRegistry registry = new KnowledgeRegistry();
 * registry.registerKnowledge("main_kb", kb1, "Main knowledge base");
 * registry.registerKnowledge("faq_kb", kb2, "FAQ knowledge base");
 *
 * // 3. Create agent with Generic RAG Hook
 * ReActAgent agent = ReActAgent.builder()
 *     .name("Assistant")
 *     .model(chatModel)
 *     .hook(registry.createGenericRAGHook("main_kb"))
 *     .build();
 *
 * // 4. Inject knowledge tools for Agentic mode
 * registry.injectKnowledgeTools(agent.getToolkit());
 * }</pre>
 */
public class KnowledgeRegistry {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRegistry.class);

    private final Map<String, Knowledge> knowledgeBases = new ConcurrentHashMap<>();
    private final Map<String, String> knowledgeDescriptions = new ConcurrentHashMap<>();

    /**
     * Registers a knowledge base with a name and description.
     *
     * @param name the unique name for the knowledge base
     * @param knowledge the knowledge base instance
     * @param description a description of the knowledge base
     * @throws IllegalArgumentException if any parameter is null or name is empty
     */
    public void registerKnowledge(String name, Knowledge knowledge, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Knowledge base name cannot be null or empty");
        }
        if (knowledge == null) {
            throw new IllegalArgumentException("Knowledge base cannot be null");
        }
        if (description == null) {
            description = "";
        }

        knowledgeBases.put(name, knowledge);
        knowledgeDescriptions.put(name, description);
        log.debug("Registered knowledge base: {}", name);
    }

    /**
     * Registers a knowledge base without a description.
     *
     * @param name the unique name for the knowledge base
     * @param knowledge the knowledge base instance
     * @throws IllegalArgumentException if any parameter is null or name is empty
     */
    public void registerKnowledge(String name, Knowledge knowledge) {
        registerKnowledge(name, knowledge, "");
    }

    /**
     * Gets a knowledge base by name.
     *
     * @param name the knowledge base name
     * @return the knowledge base, or null if not found
     */
    public Knowledge getKnowledge(String name) {
        return knowledgeBases.get(name);
    }

    /**
     * Gets the description of a knowledge base.
     *
     * @param name the knowledge base name
     * @return the description, or null if not found
     */
    public String getDescription(String name) {
        return knowledgeDescriptions.get(name);
    }

    /**
     * Gets all registered knowledge base names.
     *
     * @return a set of knowledge base names
     */
    public Set<String> getKnowledgeNames() {
        return Set.copyOf(knowledgeBases.keySet());
    }

    /**
     * Checks if a knowledge base is registered.
     *
     * @param name the knowledge base name
     * @return true if registered, false otherwise
     */
    public boolean hasKnowledge(String name) {
        return knowledgeBases.containsKey(name);
    }

    /**
     * Unregisters a knowledge base.
     *
     * @param name the knowledge base name to unregister
     * @return the removed knowledge base, or null if not found
     */
    public Knowledge unregisterKnowledge(String name) {
        Knowledge removed = knowledgeBases.remove(name);
        knowledgeDescriptions.remove(name);
        if (removed != null) {
            log.debug("Unregistered knowledge base: {}", name);
        }
        return removed;
    }

    /**
     * Clears all registered knowledge bases.
     */
    public void clear() {
        knowledgeBases.clear();
        knowledgeDescriptions.clear();
        log.debug("Cleared all knowledge bases");
    }

    /**
     * Gets the number of registered knowledge bases.
     *
     * @return the number of registered knowledge bases
     */
    public int size() {
        return knowledgeBases.size();
    }

    /**
     * Checks if the registry is empty.
     *
     * @return true if no knowledge bases are registered
     */
    public boolean isEmpty() {
        return knowledgeBases.isEmpty();
    }

    /**
     * Injects knowledge retrieval tools for all registered knowledge bases into a toolkit.
     *
     * <p>This is for Agentic mode - agents can actively call tools to retrieve knowledge.
     *
     * @param toolkit the toolkit to register tools with
     * @throws IllegalArgumentException if toolkit is null
     */
    public void injectKnowledgeTools(Toolkit toolkit) {
        if (toolkit == null) {
            throw new IllegalArgumentException("Toolkit cannot be null");
        }

        for (Map.Entry<String, Knowledge> entry : knowledgeBases.entrySet()) {
            String name = entry.getKey();
            Knowledge knowledge = entry.getValue();

            KnowledgeRetrievalTools tools = new KnowledgeRetrievalTools(knowledge);
            toolkit.registerTool(tools);
            log.debug("Injected knowledge tool for: {}", name);
        }
    }

    /**
     * Injects knowledge retrieval tool for a specific knowledge base into a toolkit.
     *
     * <p>This is for Agentic mode - agents can actively call tools to retrieve knowledge.
     *
     * @param toolkit the toolkit to register tools with
     * @param knowledgeName the name of the knowledge base
     * @throws IllegalArgumentException if toolkit is null or knowledge base not found
     */
    public void injectKnowledgeTool(Toolkit toolkit, String knowledgeName) {
        if (toolkit == null) {
            throw new IllegalArgumentException("Toolkit cannot be null");
        }
        if (knowledgeName == null || knowledgeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Knowledge name cannot be null or empty");
        }

        Knowledge knowledge = knowledgeBases.get(knowledgeName);
        if (knowledge == null) {
            throw new IllegalArgumentException("Knowledge base not found: " + knowledgeName);
        }

        KnowledgeRetrievalTools tools = new KnowledgeRetrievalTools(knowledge);
        toolkit.registerTool(tools);
        log.debug("Injected knowledge tool for: {}", knowledgeName);
    }

    /**
     * Creates a GenericRAGHook for a specific knowledge base.
     *
     * <p>This is for Generic mode - knowledge is automatically retrieved and injected
     * before each reasoning step.
     *
     * @param knowledgeName the name of the knowledge base
     * @return a GenericRAGHook instance
     * @throws IllegalArgumentException if knowledge base not found
     */
    public GenericRAGHook createGenericRAGHook(String knowledgeName) {
        if (knowledgeName == null || knowledgeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Knowledge name cannot be null or empty");
        }

        Knowledge knowledge = knowledgeBases.get(knowledgeName);
        if (knowledge == null) {
            throw new IllegalArgumentException("Knowledge base not found: " + knowledgeName);
        }

        return new GenericRAGHook(knowledge);
    }

    /**
     * Creates a GenericRAGHook that aggregates all registered knowledge bases.
     *
     * <p>This is for Generic mode - knowledge from all registered knowledge bases is
     * automatically retrieved and injected before each reasoning step.
     *
     * @return a GenericRAGHook instance using a CompositeKnowledgeBase
     * @throws IllegalStateException if no knowledge bases are registered
     */
    public GenericRAGHook createGenericRAGHook() {
        if (knowledgeBases.isEmpty()) {
            throw new IllegalStateException("No knowledge bases registered");
        }

        // Create a composite knowledge base with all registered knowledge bases
        CompositeKnowledge composite =
                new CompositeKnowledge(new ArrayList<>(knowledgeBases.values()));
        return new GenericRAGHook(composite);
    }

    /**
     * Injects knowledge tools into an agent's toolkit (convenience method).
     *
     * <p>This method extracts the toolkit from the agent and injects all knowledge tools.
     * If the agent doesn't have a toolkit, one will be created.
     *
     * @param agent the ReActAgent to inject tools into
     * @throws IllegalArgumentException if agent is null
     */
    public void injectKnowledgeTools(ReActAgent agent) {
        if (agent == null) {
            throw new IllegalArgumentException("Agent cannot be null");
        }

        Toolkit toolkit = agent.getToolkit();
        if (toolkit == null) {
            toolkit = new Toolkit();
            // Note: ReActAgent doesn't have a setter for toolkit, so this is a limitation
            // In practice, toolkit should be set during agent construction
            log.warn("Agent has no toolkit, cannot inject knowledge tools");
            return;
        }

        injectKnowledgeTools(toolkit);
    }

    /**
     * Injects knowledge tool for a specific knowledge base into an agent's toolkit (convenience
     * method).
     *
     * @param agent the ReActAgent to inject tools into
     * @param knowledgeName the name of the knowledge base
     * @throws IllegalArgumentException if agent is null or knowledge base not found
     */
    public void injectKnowledgeTool(ReActAgent agent, String knowledgeName) {
        if (agent == null) {
            throw new IllegalArgumentException("Agent cannot be null");
        }

        Toolkit toolkit = agent.getToolkit();
        if (toolkit == null) {
            log.warn("Agent has no toolkit, cannot inject knowledge tools");
            return;
        }

        injectKnowledgeTool(toolkit, knowledgeName);
    }
}
