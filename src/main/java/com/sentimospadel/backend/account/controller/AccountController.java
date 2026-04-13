package com.sentimospadel.backend.account.controller;

import com.sentimospadel.backend.account.dto.AccountDeletionRequest;
import com.sentimospadel.backend.account.dto.AccountDeletionResponse;
import com.sentimospadel.backend.account.service.AccountDeletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountDeletionService accountDeletionService;

    @GetMapping("/deletion-request")
    public AccountDeletionResponse getDeletionRequest(Authentication authentication) {
        return accountDeletionService.getDeletionRequest(authentication.getName());
    }

    @PostMapping("/deletion-request")
    public AccountDeletionResponse requestDeletion(
            Authentication authentication,
            @Valid @RequestBody(required = false) AccountDeletionRequest request
    ) {
        return accountDeletionService.requestDeletion(authentication.getName(), request);
    }
}
