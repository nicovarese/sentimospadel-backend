package com.sentimospadel.backend.club.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateClubCourtRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal hourlyRateUyu
) {
}
