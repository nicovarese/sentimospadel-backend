package com.sentimospadel.backend.legal.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.legal.dto.LegalDocumentResponse;
import com.sentimospadel.backend.legal.enums.LegalDocumentType;
import com.sentimospadel.backend.legal.service.LegalDocumentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LegalDocumentControllerTest {

    @Mock
    private LegalDocumentService legalDocumentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LegalDocumentController(legalDocumentService)).build();
    }

    @Test
    void getDocumentsIsPublic() throws Exception {
        when(legalDocumentService.getDocuments()).thenReturn(List.of(
                new LegalDocumentResponse(
                        LegalDocumentType.TERMS_AND_CONDITIONS,
                        "Términos y Condiciones",
                        "2026-04-07.1",
                        true,
                        "contenido"
                )
        ));

        mockMvc.perform(get("/api/legal/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TERMS_AND_CONDITIONS"))
                .andExpect(jsonPath("$[0].version").value("2026-04-07.1"))
                .andExpect(jsonPath("$[0].required").value(true));
    }
}
