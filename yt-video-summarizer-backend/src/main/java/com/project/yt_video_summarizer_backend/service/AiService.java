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
                "content", "You are a professional educational AI assistant that creates highly detailed, structured, and visually organized study notes from YouTube transcripts.\n" +
                        "\n" +
                        "Your notes should feel like premium educational material, not short summaries.\n" +
                        "\n" +
                        "Always:\n" +
                        "- explain concepts clearly\n" +
                        "- organize ideas properly\n" +
                        "- use markdown formatting\n" +
                        "- use headings/subheadings\n" +
                        "- create revision-friendly notes\n" +
                        "- keep notes descriptive and educational\n" +
                        "- avoid shallow summaries\n" +
                        "\"\"\";"
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
                        You are creating PROFESSIONAL STUDY NOTES from a YouTube video transcript.
                        
                        IMPORTANT:
                        These are NOT short summaries.
                        These should feel like real educational notes that a student can study from later.
                        
                        VERY IMPORTANT REQUIREMENTS:
                        - Create DETAILED notes
                        - Explain concepts clearly
                        - Expand important ideas
                        - Add educational explanations
                        - Use proper markdown formatting
                        - Use large section headings
                        - Use subheadings where needed
                        - Use bullet points extensively
                        - Keep each section descriptive
                        - Separate ideas properly
                        - DO NOT compress everything
                        - DO NOT create short summaries
                        - DO NOT create giant paragraphs
                        - Every important topic should have multiple bullet points
                        - Add examples if useful
                        - Make notes revision-friendly
                        
                        FORMAT RULES:
                        - Use markdown headings (#)
                        - Use markdown subheadings (##)
                        - Use bullet points
                        - Add blank lines between sections
                        
                        EXAMPLE STRUCTURE:
                        
                        # Introduction
                        
                        - Detailed explanation point
                        - Important concept explanation
                        - Additional learning insight
                        
                        # Main Concept
                        
                        - Concept explanation
                        - Important detail
                        - Example or application
                        
                        ## Subtopic
                        
                        - Detailed subtopic explanation
                        - Important learning point
                        
                        Generate detailed professional study notes only.
                        """;


            case "key_takeaways":
                return """
                        Extract important insights into structured markdown notes.
                        
                        Formatting Rules:
                        - Use markdown headings
                        - Add blank lines between sections
                        - Use bullet points
                        - Keep each point readable
                        - Keep formatting clean
                        - NEVER output one giant paragraph
                        
                        Correct Example:
                        
                        # Key Learnings
                        
                        - Important insight
                        - Important insight
                        
                        # Practical Ideas
                        
                        - Practical takeaway
                        """;

            case "exam_prep":
                return """
                        Create DETAILED EXAM PREPARATION NOTES from this transcript.
                        
                        IMPORTANT:
                        These are not short summaries.
                        These should feel like actual revision notes students study before exams.
                        
                        REQUIREMENTS:
                        - Use markdown formatting
                        - Use detailed bullet points
                        - Explain concepts clearly
                        - Highlight important topics
                        - Expand key ideas
                        - Add definitions where useful
                        - Organize content by topics
                        - Keep sections educational
                        - Avoid short shallow points
                        - Avoid giant paragraphs
                        - Use headings and subheadings
                        - Add revision-friendly structure
                        
                        FORMAT:
                        
                        # Important Topic
                        
                        - Detailed explanation
                        - Important concept
                        - Key revision point
                        
                        ## Definition
                        
                        - Important definition
                        
                        ## Key Takeaways
                        
                        - Important learning point
                        
                        Generate detailed revision notes only.
                        """;

            case "summary":
            default:
                return """
                        Create detailed and structured markdown study notes.
                        
                        VERY IMPORTANT FORMATTING RULES:
                        - Use ONLY valid markdown formatting
                        - Add a blank line after every heading
                        - Add a blank line before every heading
                        - Use proper bullet points
                        - Keep sections separated clearly
                        - NEVER write everything in one paragraph
                        - NEVER combine headings and bullets in same line
                        
                        Correct Example:
                        
                        # Introduction
                        
                        - Point one
                        - Point two
                        
                        # Key Concepts
                        
                        - Concept one
                        - Concept two
                        
                        Generate clean markdown notes only.
                        """;
        }
    }
}