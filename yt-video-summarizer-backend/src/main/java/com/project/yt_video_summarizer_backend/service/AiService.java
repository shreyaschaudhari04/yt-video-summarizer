package com.project.yt_video_summarizer_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiService {

    private final RestTemplate restTemplate;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.model}")
    private String groqModel;

    public AiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, String> generateContent(String transcript, String outputLanguage, String mode) {

        System.out.println("Output language received: " + outputLanguage);
        System.out.println("Mode received: " + mode);

        if (transcript.length() > 8000) {
            transcript = transcript.substring(0, 8000);
        }

        if (mode == null || mode.isBlank()) {
            mode = "summary";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqModel);

        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", "You are an AI assistant that helps students understand YouTube videos clearly."
        ));

        String modeInstruction = getModeInstruction(mode);

        String targetLanguage;

        if (outputLanguage != null && !outputLanguage.isBlank()) {
            targetLanguage = outputLanguage;
        } else {
            targetLanguage = "same as transcript";
        }

        String prompt = """
                Read the following YouTube transcript carefully.

                TASKS:
                1. Translate the transcript fully into %s.
                2. Generate the output in %s mode.

                IMPORTANT RULES:
                - Final output must be ONLY in %s.
                - Do NOT use any other language.
                - Keep the translated transcript natural and easy to understand.
                - Follow the requested mode strictly.
                - Do NOT skip important meaning while translating.

                MODE INSTRUCTIONS:
                %s

                RETURN THE OUTPUT IN EXACTLY THIS FORMAT:

                TRANSLATED_TRANSCRIPT:
                <translated transcript here>

                SUMMARY:
                <final output here>

                Transcript:
                %s
                """.formatted(
                targetLanguage,
                mode,
                targetLanguage,
                modeInstruction,
                transcript
        );

        messages.add(Map.of(
                "role", "user",
                "content", prompt
        ));

        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> requestEntity =
                new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                groqApiUrl,
                requestEntity,
                Map.class
        );

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.getBody().get("choices");

        Map<String, Object> firstChoice = choices.get(0);

        Map<String, Object> message =
                (Map<String, Object>) firstChoice.get("message");

        String content = message.get("content").toString();

        String translatedTranscript = "";
        String summary = "";

        if (content.contains("TRANSLATED_TRANSCRIPT:")
                && content.contains("SUMMARY:")) {

            String[] parts = content.split("SUMMARY:", 2);

            translatedTranscript = parts[0]
                    .replace("TRANSLATED_TRANSCRIPT:", "")
                    .trim();

            summary = parts[1].trim();

        } else {

            translatedTranscript = transcript;
            summary = content;
        }

        Map<String, String> result = new HashMap<>();

        result.put("translatedTranscript", translatedTranscript);
        result.put("summary", summary);

        return result;
    }

    private String getModeInstruction(String mode) {

        String normalizedMode;

        if (mode == null || mode.isBlank()) {
            normalizedMode = "summary";
        } else {
            normalizedMode = mode.toLowerCase();
        }

        switch (normalizedMode) {

            case "notes":
                return """
                        Convert the transcript into professional study notes.
                        
                        Requirements:
                        - Use markdown headings and subheadings
                        - Use descriptive bullet points
                        - Explain concepts clearly
                        - Add structure and logical grouping
                        - Avoid giant paragraphs
                        - Keep notes educational and revision-friendly
                        - Expand important ideas when needed
                        - Make it visually organized
                        
                        Example:
                        
                        # Topic Name
                        - Detailed explanation
                        - Important concept
                        
                        ## Subtopic
                        - Important detail
                        - Important detail
                        """;

            case "key_takeaways":
                return """
                        Extract the most important insights from the transcript.
                        
                        Requirements:
                        - Use markdown headings
                        - Use meaningful bullet points
                        - Keep points concise but informative
                        - Focus on practical learning
                        - Organize takeaways by topic
                        - Avoid vague statements
                        
                        Example:
                        
                        # Key Learnings
                        - Important insight
                        - Important insight
                        
                        # Practical Ideas
                        - Important takeaway
                        """;

            case "exam_prep":
                return """
                        Create detailed exam preparation notes.
                        
                        Requirements:
                        - Use markdown headings
                        - Organize by concepts/topics
                        - Use descriptive bullet points
                        - Highlight important concepts
                        - Make content revision-friendly
                        - Include important definitions where useful
                        - Expand key ideas clearly
                        - Avoid giant paragraphs
                        
                        Example:
                        
                        # Important Concepts
                        - Detailed explanation
                        - Key revision point
                        
                        # Definitions
                        - Important term explanation
                        
                        # Key Points
                        - Important exam-oriented note
                        """;

            case "summary":
            default:
                return """
                        Create detailed but concise study notes.
                        
                        Requirements:
                        - Use proper markdown headings
                        - Use bullet points
                        - Explain ideas clearly
                        - Keep information descriptive
                        - Avoid one-line shallow points
                        - Make it easy to revise later
                        - Organize content into sections
                        
                        Example format:
                        
                        # Introduction
                        - Detailed explanation point
                        - Another important point
                        
                        # Main Concepts
                        - Explanation
                        - Explanation
                        """;
        }
    }
}