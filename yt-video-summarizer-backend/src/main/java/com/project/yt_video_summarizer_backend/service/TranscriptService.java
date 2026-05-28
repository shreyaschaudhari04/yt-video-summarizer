package com.project.yt_video_summarizer_backend.service;

import com.project.yt_video_summarizer_backend.dto.TranscriptResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TranscriptService {

    private final RestTemplate restTemplate;

    @Value("${python.service.url}")
    private String pythonServiceUrl;

    public TranscriptService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TranscriptResponse getTranscript(String url) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of("url", url);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                pythonServiceUrl,
                requestEntity,
                Map.class
        );

        Map body = response.getBody();

        if (body == null || body.get("transcript") == null) {
            throw new RuntimeException("Transcript service returned invalid response");
        }

        String transcript = body.get("transcript").toString();

        String language = body.get("language") != null ? body.get("language").toString() : "Unknown";
        String languageCode = body.get("language_code") != null ? body.get("language_code").toString() : "unknown";

        return new TranscriptResponse(transcript, language, languageCode);
    }
}