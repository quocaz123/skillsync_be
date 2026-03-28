package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.upload.PresignedUploadRequest;
import com.skillsync.skillsync.dto.response.upload.PresignedUploadResponse;
import com.skillsync.skillsync.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import com.skillsync.skillsync.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUploadResponse> generatePresignedUrl(@RequestBody PresignedUploadRequest request) {
        return ApiResponse.success(fileUploadService.generatePresignedUploadUrl(request));
    }
}
