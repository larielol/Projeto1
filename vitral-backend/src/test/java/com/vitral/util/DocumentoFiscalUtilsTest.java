package com.vitral.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentoFiscalUtilsTest {
    @Test void aceitaCpfSemMascara() { assertThat(DocumentoFiscalUtils.cpfValido("52998224725")).isTrue(); }
    @Test void aceitaCpfComMascara() { assertThat(DocumentoFiscalUtils.cpfValido("529.982.247-25")).isTrue(); }
    @Test void rejeitaCpfComDigitoIncorreto() { assertThat(DocumentoFiscalUtils.cpfValido("52998224724")).isFalse(); }
    @Test void rejeitaCpfRepetido() { assertThat(DocumentoFiscalUtils.cpfValido("11111111111")).isFalse(); }
    @Test void aceitaCnpjSemMascara() { assertThat(DocumentoFiscalUtils.cnpjValido("11222333000181")).isTrue(); }
    @Test void aceitaCnpjComMascara() { assertThat(DocumentoFiscalUtils.cnpjValido("11.222.333/0001-81")).isTrue(); }
    @Test void rejeitaCnpjComDigitoIncorreto() { assertThat(DocumentoFiscalUtils.cnpjValido("11222333000182")).isFalse(); }
    @Test void rejeitaCnpjRepetido() { assertThat(DocumentoFiscalUtils.cnpjValido("11111111111111")).isFalse(); }
    @Test void mascaraCpf() { assertThat(DocumentoFiscalUtils.mascararCpf("52998224725")).isEqualTo("***.***.***-25"); }
    @Test void mascaraCnpj() { assertThat(DocumentoFiscalUtils.mascararCnpj("11222333000181")).isEqualTo("**.***.***/****-81"); }
}
