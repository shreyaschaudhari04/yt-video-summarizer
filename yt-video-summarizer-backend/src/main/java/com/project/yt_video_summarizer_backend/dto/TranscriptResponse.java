package com.project.yt_video_summarizer_backend.dto;

public class TranscriptResponse {
    private String transcript;
    private String language;
    private String languageCode;

    public TranscriptResponse() {
    }

    public TranscriptResponse(String transcript, String language, String languageCode) {
        this.transcript = transcript;
        this.language = language;
        this.languageCode = languageCode;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
}
