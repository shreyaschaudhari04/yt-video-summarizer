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
                        Create detailed, well-structured notes.
                        - Use headings and subheadings
                        - Explain clearly
                        - Keep it student-friendly
                        - Use bullet points where needed
                        """;

            case "key_takeaways":
                return """
                        Extract only the most important takeaways.
                        - Keep it concise
                        - Focus only on the core learnings
                        - Use bullet points
                        """;

            case "exam_prep":
                return """
                        Convert this into exam preparation format.
                        - Highlight important concepts
                        - Mention possible exam-relevant points
                        - Make it easy for revision
                        - Use bullet points and mini headings
                        """;

            case "summary":
            default:
                return """
                        Summarize into short, clear bullet points.
                        - Keep it concise
                        - Focus on the main ideas only
                        - Use bullet points
                        """;
        }
    }
}