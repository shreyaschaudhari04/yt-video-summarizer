package com.project.yt_video_summarizer_backend.dto;

public class SummaryResponse {

    private String summary;
    private String originalTranscript;
    private String translatedTranscript;
    private String language;
    private String languageCode;

    public SummaryResponse() {}

    public SummaryResponse(String summary, String originalTranscript, String translatedTranscript, String language, String languageCode) {
        this.summary = summary;
        this.originalTranscript = originalTranscript;
        this.translatedTranscript = translatedTranscript;
        this.language = language;
        this.languageCode = languageCode;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getOriginalTranscript() {
        return originalTranscript;
    }

    public void setOriginalTranscript(String originalTranscript) {
        this.originalTranscript = originalTranscript;
    }

    public String getTranslatedTranscript() {
        return translatedTranscript;
    }

    public void setTranslatedTranscript(String translatedTranscript) {
        this.translatedTranscript = translatedTranscript;
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