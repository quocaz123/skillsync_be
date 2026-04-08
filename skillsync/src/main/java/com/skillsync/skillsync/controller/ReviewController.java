package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.request.review.ReviewRequest;
import com.skillsync.skillsync.dto.response.review.ReviewResponse;
import com.skillsync.skillsync.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<Void> createReview(@RequestBody @Valid ReviewRequest request) {
        reviewService.createReview(request);
        return ApiResponse.success(null);
    }

    @GetMapping("/user/{id}")
    public ApiResponse<List<ReviewResponse>> getReviewsByUser(@PathVariable java.util.UUID id) {
        return ApiResponse.success(reviewService.getReviewsByReviewee(id));
    }
}
