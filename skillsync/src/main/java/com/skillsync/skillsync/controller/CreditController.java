package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.response.user.CreditTransactionResponse;
import com.skillsync.skillsync.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/history")
    public ApiResponse<List<CreditTransactionResponse>> getMyCreditHistory() {
        return ApiResponse.success(creditService.getMyCreditHistory());
    }
}
