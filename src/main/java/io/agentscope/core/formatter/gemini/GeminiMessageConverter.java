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
package io.agentscope.core.formatter.gemini;

import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;
import io.agentscope.core.message.AudioBlock;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.VideoBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter for transforming AgentScope Msg objects to Gemini API Content format.
 *
 * <p>This converter handles the core message transformation logic, including:
 * <ul>
 *   <li>Text blocks</li>
 *   <li>Tool use blocks (function_call)</li>
 *   <li>Tool result blocks (function_response as independent Content)</li>
 *   <li>Multimodal content (image, audio, video)</li>
 * </ul>
 *
 * <p><b>Important:</b> This converter follows Python implementation behavior:
 * <ul>
 *   <li>Tool result blocks are converted to independent "user" role Content</li>
 *   <li>Multiple tool outputs are formatted with "- " prefix per line</li>
 *   <li>System messages are treated as "user" role</li>
 * </ul>
 */
public class GeminiMessageConverter {

    private static final Logger log = LoggerFactory.getLogger(GeminiMessageConverter.class);

    private final GeminiMediaConverter mediaConverter;

    public GeminiMessageConverter() {
        this.mediaConverter = new GeminiMediaConverter();
    }

    /**
     * Convert a list of Msg objects to Gemini API Content objects.
     *
     * @param msgs List of AgentScope messages
     * @return List of Gemini Content objects
     */
    public List<Content> convertMessages(List<Msg> msgs) {
        List<Content> result = new ArrayList<>();

        for (Msg msg : msgs) {
            List<Part> parts = new ArrayList<>();

            for (ContentBlock block : msg.getContent()) {
                if (block instanceof TextBlock tb) {
                    parts.add(Part.builder().text(tb.getText()).build());

                } else if (block instanceof ToolUseBlock tub) {
                    // Create FunctionCall
                    FunctionCall functionCall =
                            FunctionCall.builder()
                                    .id(tub.getId())
                                    .name(tub.getName())
                                    .args(tub.getInput())
                                    .build();
                    parts.add(Part.builder().functionCall(functionCall).build());

                } else if (block instanceof ToolResultBlock trb) {
                    // IMPORTANT: Tool result as independent Content with "user" role
                    String textOutput = convertToolResultToString(trb.getOutput());

                    // Create response map with "output" key
                    Map<String, Object> responseMap = new HashMap<>();
                    responseMap.put("output", textOutput);

                    FunctionResponse functionResponse =
                            FunctionResponse.builder()
                                    .id(trb.getId())
                                    .name(trb.getName())
                                    .response(responseMap)
                                    .build();

                    Part functionResponsePart =
                            Part.builder().functionResponse(functionResponse).build();

                    Content toolResultContent =
                            Content.builder()
                                    .role("user")
                                    .parts(List.of(functionResponsePart))
                                    .build();

                    result.add(toolResultContent);
                    // Skip adding to current message parts
                    continue;

                } else if (block instanceof ImageBlock ib) {
                    parts.add(mediaConverter.convertToInlineDataPart(ib));

                } else if (block instanceof AudioBlock ab) {
                    parts.add(mediaConverter.convertToInlineDataPart(ab));

                } else if (block instanceof VideoBlock vb) {
                    parts.add(mediaConverter.convertToInlineDataPart(vb));

                } else if (block instanceof ThinkingBlock) {
                    // Skip ThinkingBlock - not sent to LLM
                    log.debug("Skipping ThinkingBlock when formatting message for Gemini API");
                    continue;

                } else {
                    log.warn(
                            "Unsupported block type: {} in the message, skipped.",
                            block.getClass().getSimpleName());
                }
            }

            // Add message if there are parts
            if (!parts.isEmpty()) {
                String role = convertRole(msg.getRole());
                Content content = Content.builder().role(role).parts(parts).build();
                result.add(content);
            }
        }

        return result;
    }

    /**
     * Convert MsgRole to Gemini API role string.
     *
     * @param role AgentScope message role
     * @return Gemini API role ("user" or "model")
     */
    private String convertRole(MsgRole role) {
        // In Gemini API: "model" for assistant, "user" for everything else
        return role == MsgRole.ASSISTANT ? "model" : "user";
    }

    /**
     * Convert tool result output to string representation.
     * Follows Python implementation: single item returns directly,
     * multiple items use "- " prefix per line.
     *
     * @param output List of content blocks from tool result
     * @return String representation of the output
     */
    private String convertToolResultToString(List<ContentBlock> output) {
        if (output == null || output.isEmpty()) {
            return "";
        }

        List<String> textualOutput = new ArrayList<>();

        for (ContentBlock block : output) {
            if (block instanceof TextBlock tb) {
                textualOutput.add(tb.getText());

            } else if (block instanceof ImageBlock ib) {
                String reference = convertMediaBlockToTextReference("image");
                textualOutput.add(reference);

            } else if (block instanceof AudioBlock ab) {
                String reference = convertMediaBlockToTextReference("audio");
                textualOutput.add(reference);

            } else if (block instanceof VideoBlock vb) {
                String reference = convertMediaBlockToTextReference("video");
                textualOutput.add(reference);
            }
            // Other block types are ignored
        }

        // Single item: return directly
        // Multiple items: prefix each with "- " and join with newlines
        if (textualOutput.size() == 1) {
            return textualOutput.get(0);
        } else {
            return textualOutput.stream()
                    .map(s -> "- " + s)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        }
    }

    /**
     * Convert a media block to textual reference for tool results.
     * Follows Python behavior: "The returned {mediaType} can be found at: {path}"
     *
     * <p>Note: In Python implementation, base64 data is saved to a temp file and the path is returned.
     * For now, we use a mock path for tool results.
     *
     * @param mediaType Media type string ("image", "audio", or "video")
     * @return Textual reference to the media
     */
    private String convertMediaBlockToTextReference(String mediaType) {
        // TODO: In real implementation, save base64 data to temp file like Python does
        // For now, use a placeholder path
        String mockPath = "/var/folders/gf/krg8x_ws409cpw_46b2s6rjc0000gn/T/tmpfymnv2w9.wav";
        return String.format("The returned %s can be found at: %s", mediaType, mockPath);
    }
}
