package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.AdminTransactionDTO;
import com.skillsync.skillsync.dto.GrantCreditRequest;
import com.skillsync.skillsync.service.AdminCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true") // Temporary dev setting
public class AdminCreditController {

    private final AdminCreditService adminCreditService;

//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<AdminTransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(adminCreditService.getAllTransactions());
    }

//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/grant")
    public ResponseEntity<AdminTransactionDTO> grantCredit(@Valid @RequestBody GrantCreditRequest request) {
        return ResponseEntity.ok(adminCreditService.grantCredit(request));
    }
}
