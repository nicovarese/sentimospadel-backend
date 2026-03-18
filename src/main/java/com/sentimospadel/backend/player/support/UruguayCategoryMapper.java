package com.sentimospadel.backend.player.support;

import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;

public final class UruguayCategoryMapper {

    private UruguayCategoryMapper() {
    }

    public static UruguayCategory fromRating(BigDecimal rating) {
        if (rating.compareTo(BigDecimal.valueOf(6.40)) >= 0) {
            return UruguayCategory.PRIMERA;
        }
        if (rating.compareTo(BigDecimal.valueOf(5.50)) >= 0) {
            return UruguayCategory.SEGUNDA;
        }
        if (rating.compareTo(BigDecimal.valueOf(4.80)) >= 0) {
            return UruguayCategory.TERCERA;
        }
        if (rating.compareTo(BigDecimal.valueOf(4.10)) >= 0) {
            return UruguayCategory.CUARTA;
        }
        if (rating.compareTo(BigDecimal.valueOf(3.40)) >= 0) {
            return UruguayCategory.QUINTA;
        }
        if (rating.compareTo(BigDecimal.valueOf(2.60)) >= 0) {
            return UruguayCategory.SEXTA;
        }
        return UruguayCategory.SEPTIMA;
    }
}
