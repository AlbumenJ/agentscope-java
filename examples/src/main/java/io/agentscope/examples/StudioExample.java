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
import io.agentscope.core.studio.StudioUserAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.studio.StudioManager;
import io.agentscope.core.studio.StudioMessageHook;
import io.agentscope.core.tool.Toolkit;

/**
 * StudioExample - Demonstrates AgentScope Studio integration.
 *
 * <p>This example shows how to:
 *
 * <ul>
 *   <li>Initialize Studio connection
 *   <li>Create an agent with automatic message forwarding to Studio
 *   <li>Visualize agent conversations in real-time via Studio web UI
 *   <li>Clean up Studio resources
 * </ul>
 *
 * <p>Prerequisites:
 *
 * <ol>
 *   <li>Start Studio server:
 *       <pre>cd agentscope-studio && npm install && npm run dev</pre>
 *   <li>Studio will run at http://localhost:8000
 * </ol>
 *
 * <p>Run:
 *
 * <pre>
 * mvn exec:java -Dexec.mainClass="io.agentscope.examples.StudioExample"
 * </pre>
 *
 * <p>After starting, open http://localhost:8000 in your browser to see the conversation in
 * real-time!
 *
 * <p>Requirements:
 *
 * <ul>
 *   <li>DASHSCOPE_API_KEY environment variable (or interactive input)
 *   <li>Studio server running at http://localhost:8000
 * </ul>
 */
public class StudioExample {

    private static final String STUDIO_URL = "http://localhost:3000";

    public static void main(String[] args) throws Exception {
        // Print welcome message
        ExampleUtils.printWelcome(
                "Studio Integration Example",
                "This example demonstrates real-time visualization of agent conversations.\n"
                        + "Messages will be automatically forwarded to Studio web interface.\n"
                        + "\n"
                        + "Make sure Studio is running at: "
                        + STUDIO_URL
                        + "\n"
                        + "Open it in your browser to see the conversation!");

        // Get API key
        String apiKey = ExampleUtils.getDashScopeApiKey();

        // Initialize Studio integration
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📡 Initializing Studio Connection...");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        try {
            StudioManager.init()
                    .studioUrl(STUDIO_URL)
                    .project("JavaExamples")
                    .runName("studio_demo_" + System.currentTimeMillis())
                    .maxRetries(3)
                    .reconnectAttempts(3)
                    .initialize()
                    .block();

            System.out.println("✓ Studio connection established");
            System.out.println("✓ WebSocket connected");
            System.out.println(
                    "\n🌐 Open Studio in your browser: "
                            + STUDIO_URL
                            + "\n   Navigate to 'JavaExamples' project to see this run\n");

        } catch (Exception e) {
            System.err.println("✗ Failed to connect to Studio: " + e.getMessage());
            System.err.println("\nPlease ensure Studio is running:");
            System.err.println("  cd agentscope-studio");
            System.err.println("  npm install");
            System.err.println("  npm run dev");
            System.err.println("\nContinuing without Studio...\n");
        }

        // Create Agent with Studio hook
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🤖 Creating Agent with Studio Integration...");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        ReActAgent.Builder agentBuilder =
                ReActAgent.builder()
                        .name("StudioAssistant")
                        .sysPrompt(
                                "You are a helpful AI assistant demonstrating Studio integration. "
                                        + "Be friendly, informative, and engaging. "
                                        + "Explain what you're doing in your responses.")
                        .model(
                                DashScopeChatModel.builder()
                                        .apiKey(apiKey)
                                        .modelName("qwen-plus")
                                        .stream(true)
                                        .enableThinking(true)
                                        .formatter(new DashScopeChatFormatter())
                                        .defaultOptions(
                                                GenerateOptions.builder()
                                                        .thinkingBudget(1024)
                                                        .build())
                                        .build())
                        .memory(new InMemoryMemory())
                        .toolkit(new Toolkit());

        // Add Studio hook only if Studio is initialized
        if (StudioManager.isInitialized()) {
            agentBuilder.hook(new StudioMessageHook(StudioManager.getClient()));
            System.out.println("✓ Agent created with Studio message hook");
            System.out.println("  All agent responses will be automatically sent to Studio\n");
        } else {
            System.out.println("✓ Agent created without Studio integration");
            System.out.println("  Studio is not available, running in standalone mode\n");
        }

        ReActAgent agent = agentBuilder.build();

        // Create UserProxyAgent with Studio integration
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("👤 Creating User Agent...");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        StudioUserAgent.Builder userBuilder = StudioUserAgent.builder().name("User");

        // Add Studio integration only if Studio is initialized
        if (StudioManager.isInitialized()) {
            userBuilder
                    .studioClient(StudioManager.getClient())
                    .webSocketClient(StudioManager.getWebSocketClient());
            System.out.println("✓ User agent created with Studio integration");
            System.out.println("  User input will be requested through Studio web interface\n");
        } else {
            System.out.println("✓ User agent created for terminal input");
            System.out.println("  User input will be requested through terminal/console\n");
        }

        StudioUserAgent user = userBuilder.build();

        // Start interactive conversation
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("💬 Starting Interactive Conversation");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Open Studio UI to send messages to the agent!");
        System.out.println("Type 'exit' or 'quit' in Studio to end the conversation\n");

        try {
            // Conversation loop: User -> Agent -> User -> Agent ...
            Msg userMsg = null;
            int turn = 1;

            while (true) {
                System.out.println("\n--- Turn " + turn + " ---");

                // Get user input from Studio
                System.out.println("Waiting for user input from Studio...");
                userMsg = user.call(userMsg).block();

                if (userMsg == null) {
                    System.out.println("No input received, ending conversation.");
                    break;
                }

                String userInput = userMsg.getTextContent();
                System.out.println("User: " + userInput);

                // Check for exit commands
                if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                    System.out.println("\nGoodbye! 👋");
                    break;
                }

                // Get agent response
                System.out.println("Agent is thinking...");
                Msg agentMsg = agent.call(userMsg).block();

                if (agentMsg != null) {
                    System.out.println(agent.getName() + ": " + agentMsg.getTextContent());
                }

                turn++;
            }
        } catch (Exception e) {
            System.err.println("Error during conversation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up Studio resources
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("🧹 Cleaning up...");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            StudioManager.shutdown();
            System.out.println("✓ Studio connection closed");
            System.out.println("✓ Resources released\n");
            System.out.println("Thank you for using AgentScope! 👋");
        }
    }
}
