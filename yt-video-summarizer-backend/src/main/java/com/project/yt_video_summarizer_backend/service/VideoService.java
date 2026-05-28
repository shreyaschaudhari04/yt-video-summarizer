package com.project.yt_video_summarizer_backend.service;

import com.project.yt_video_summarizer_backend.dto.SummaryResponse;
import com.project.yt_video_summarizer_backend.dto.TranscriptResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VideoService {

    private final TranscriptService transcriptService;
    private final AiService aiService;

    public VideoService(TranscriptService transcriptService, AiService aiService) {
        this.transcriptService = transcriptService;
        this.aiService = aiService;
    }

    public SummaryResponse summarizeVideo(String url, String outputLanguage, String mode) {

        TranscriptResponse transcriptData = transcriptService.getTranscript(url);

        String originalTranscript = transcriptData.getTranscript();

        Map<String, String> aiResult = aiService.generateContent(originalTranscript, outputLanguage, mode);

        String translatedTranscript = aiResult.get("translatedTranscript");
        String summary = aiResult.get("summary");

        return new SummaryResponse(
                summary,
                originalTranscript,
                translatedTranscript,
                transcriptData.getLanguage(),
                transcriptData.getLanguageCode()
        );
    }
}