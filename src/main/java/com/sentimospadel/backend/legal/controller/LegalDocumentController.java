package com.sentimospadel.backend.legal.controller;

import com.sentimospadel.backend.legal.dto.LegalDocumentResponse;
import com.sentimospadel.backend.legal.service.LegalDocumentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/legal")
@RequiredArgsConstructor
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    @GetMapping("/documents")
    public List<LegalDocumentResponse> getDocuments() {
        return legalDocumentService.getDocuments();
    }
}
