package com.project.yt_video_summarizer_backend.controller;

import com.project.yt_video_summarizer_backend.dto.SummaryResponse;
import com.project.yt_video_summarizer_backend.dto.VideoRequest;
import com.project.yt_video_summarizer_backend.service.VideoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video")
@CrossOrigin("*")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/summarize")
    public SummaryResponse summarize(@RequestBody VideoRequest request) {
        return videoService.summarizeVideo(
                request.getUrl(),
                request.getOutputLanguage(),
                request.getMode()
        );
    }
}