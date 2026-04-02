package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.review.ReviewRequest;
import com.skillsync.skillsync.dto.response.ApiResponse;
import com.skillsync.skillsync.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<Void> createReview(@RequestBody @Valid ReviewRequest request) {
        reviewService.createReview(request);
        return ApiResponse.<Void>builder()
                .message("Review submitted successfully")
                .build();
    }

    @GetMapping("/user/{id}")
    public ApiResponse<java.util.List<com.skillsync.skillsync.dto.response.review.ReviewResponse>> getReviewsByUser(@PathVariable java.util.UUID id) {
        return ApiResponse.<java.util.List<com.skillsync.skillsync.dto.response.review.ReviewResponse>>builder()
                .result(reviewService.getReviewsByReviewee(id))
                .build();
    }
}
