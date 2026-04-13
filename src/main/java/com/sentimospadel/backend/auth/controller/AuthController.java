package com.sentimospadel.backend.auth.controller;

import com.sentimospadel.backend.auth.dto.CurrentUserResponse;
import com.sentimospadel.backend.auth.dto.EmailVerificationDispatchResponse;
import com.sentimospadel.backend.auth.dto.EmailVerificationPageResult;
import com.sentimospadel.backend.auth.dto.LoginRequest;
import com.sentimospadel.backend.auth.dto.LoginResponse;
import com.sentimospadel.backend.auth.dto.LogoutResponse;
import com.sentimospadel.backend.auth.dto.RefreshTokenRequest;
import com.sentimospadel.backend.auth.dto.RegisterRequest;
import com.sentimospadel.backend.auth.dto.RegisterResponse;
import com.sentimospadel.backend.auth.dto.ResendEmailVerificationRequest;
import com.sentimospadel.backend.auth.ratelimit.AuthRateLimiter;
import com.sentimospadel.backend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimiter authRateLimiter;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        authRateLimiter.checkRegister(servletRequest, request);
        RegisterResponse response = authService.register(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
    }

    @PostMapping("/verify-email/resend")
    public EmailVerificationDispatchResponse resendEmailVerification(
            @Valid @RequestBody ResendEmailVerificationRequest request,
            HttpServletRequest servletRequest
    ) {
        authRateLimiter.checkResendVerification(servletRequest, request);
        return authService.resendEmailVerification(request);
    }

    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        EmailVerificationPageResult result = authService.verifyEmail(token);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(renderEmailVerificationPage(result));
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        authRateLimiter.checkLogin(servletRequest, request);
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public LogoutResponse logout(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.logout(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        return authService.getCurrentUser(authentication.getName());
    }

    private String renderEmailVerificationPage(EmailVerificationPageResult result) {
        String title = HtmlUtils.htmlEscape(result.title());
        String message = HtmlUtils.htmlEscape(result.message());
        String actionLabel = HtmlUtils.htmlEscape(result.actionLabel());
        String actionMarkup = result.actionUrl() == null || result.actionUrl().isBlank()
                ? ""
                : """
                    <a href="%s" style="display:inline-block;margin-top:24px;padding:12px 18px;border-radius:999px;background:#84cc16;color:#04130a;text-decoration:none;font-weight:700;">
                        %s
                    </a>
                    """.formatted(HtmlUtils.htmlEscape(result.actionUrl()), actionLabel);

        String accent = result.success() ? "#84cc16" : "#f59e0b";

        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                </head>
                <body style="margin:0;font-family:Arial,sans-serif;background:#020617;color:#e2e8f0;">
                  <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px;">
                    <div style="max-width:520px;width:100%%;background:#0f172a;border:1px solid #1e293b;border-radius:24px;padding:32px;box-shadow:0 20px 60px rgba(0,0,0,.35);">
                      <div style="width:56px;height:56px;border-radius:16px;background:%s;display:flex;align-items:center;justify-content:center;color:#020617;font-size:28px;font-weight:900;">S</div>
                      <h1 style="margin:20px 0 12px;font-size:28px;line-height:1.1;color:#fff;">%s</h1>
                      <p style="margin:0;color:#cbd5e1;font-size:16px;line-height:1.6;">%s</p>
                      %s
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(title, accent, title, message, actionMarkup);
    }
}
