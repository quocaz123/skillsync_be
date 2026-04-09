package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.response.admin.AdminCreditTransactionResponse;
import com.skillsync.skillsync.entity.CreditTransaction;
import com.skillsync.skillsync.enums.TransactionType;
import com.skillsync.skillsync.repository.CreditTransactionRepository;
import lombok.RequiredArgsConstructor;
import com.skillsync.skillsync.dto.AdminTransactionDTO;
import com.skillsync.skillsync.dto.GrantCreditRequest;
import com.skillsync.skillsync.service.AdminCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/credits")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCreditController {

    private final CreditTransactionRepository creditTransactionRepository;
    private final AdminCreditService adminCreditService;

    /**
     * GET /api/admin/credits/transactions?type=SPEND_SESSION
     * Lấy toàn bộ lịch sử giao dịch credit của hệ thống, có thể lọc theo type.
     */
    @GetMapping("/transactions")
    public ApiResponse<List<AdminCreditTransactionResponse>> getAllTransactions(
            @RequestParam(required = false) TransactionType type) {

        List<CreditTransaction> transactions = creditTransactionRepository.findAll();

        if (type != null) {
            transactions = transactions.stream()
                    .filter(t -> type.equals(t.getTransactionType()))
                    .collect(Collectors.toList());
        }

        // Sort by createdAt desc
        transactions.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        List<AdminCreditTransactionResponse> result = transactions.stream()
                .map(t -> AdminCreditTransactionResponse.builder()
                        .id(t.getId())
                        .userName(t.getUser() != null ? t.getUser().getFullName() : "Hệ thống")
                        .userEmail(t.getUser() != null ? t.getUser().getEmail() : null)
                        .userAvatarUrl(t.getUser() != null ? t.getUser().getAvatarUrl() : null)
                        .amount(t.getAmount())
                        .transactionType(t.getTransactionType())
                        .description(t.getDescription())
                        .referenceId(t.getReferenceId())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(result);
    }



    @GetMapping
    public ResponseEntity<List<AdminTransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(adminCreditService.getAllTransactions());
    }

    @PostMapping("/grant")
    public ResponseEntity<AdminTransactionDTO> grantCredit(@Valid @RequestBody GrantCreditRequest request) {
        return ResponseEntity.ok(adminCreditService.grantCredit(request));
    }
}
