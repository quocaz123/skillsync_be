package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.PresignedUploadRequest;
import com.skillsync.skillsync.dto.response.PresignedUploadResponse;
import com.skillsync.skillsync.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/presigned-url")
    public PresignedUploadResponse generatePresignedUrl(@RequestBody PresignedUploadRequest request) {
        return fileUploadService.generatePresignedUploadUrl(request);
    }
}
