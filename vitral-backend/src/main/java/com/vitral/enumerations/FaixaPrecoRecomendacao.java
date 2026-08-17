package com.vitral.enumerations;

import java.math.BigDecimal;

public enum FaixaPrecoRecomendacao {
    ATE_25, DE_25_A_50, DE_50_A_100, DE_100_A_200, ACIMA_200;

    public static FaixaPrecoRecomendacao de(BigDecimal preco) {
        if (preco == null || preco.compareTo(BigDecimal.valueOf(25)) <= 0) return ATE_25;
        if (preco.compareTo(BigDecimal.valueOf(50)) <= 0) return DE_25_A_50;
        if (preco.compareTo(BigDecimal.valueOf(100)) <= 0) return DE_50_A_100;
        if (preco.compareTo(BigDecimal.valueOf(200)) <= 0) return DE_100_A_200;
        return ACIMA_200;
    }
}
