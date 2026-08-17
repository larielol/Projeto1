package com.vitral.util;

public final class DocumentoFiscalUtils {
    private DocumentoFiscalUtils() {}

    public static String somenteDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    public static boolean cpfValido(String valor) {
        String cpf = somenteDigitos(valor);
        if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) return false;
        int soma = 0;
        for (int i = 0; i < 9; i++) soma += (cpf.charAt(i) - '0') * (10 - i);
        int d1 = 11 - soma % 11;
        if (d1 >= 10) d1 = 0;
        soma = 0;
        for (int i = 0; i < 10; i++) soma += (cpf.charAt(i) - '0') * (11 - i);
        int d2 = 11 - soma % 11;
        if (d2 >= 10) d2 = 0;
        return cpf.charAt(9) - '0' == d1 && cpf.charAt(10) - '0' == d2;
    }

    public static boolean cnpjValido(String valor) {
        String cnpj = somenteDigitos(valor);
        if (cnpj == null || !cnpj.matches("\\d{14}") || cnpj.chars().distinct().count() == 1) return false;
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int d1 = digito(cnpj, pesos1);
        int d2 = digito(cnpj.substring(0, 12) + d1, pesos2);
        return cnpj.endsWith("" + d1 + d2);
    }

    private static int digito(String numero, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) soma += (numero.charAt(i) - '0') * pesos[i];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    public static String mascararCpf(String valor) {
        String cpf = somenteDigitos(valor);
        return cpf == null || cpf.length() != 11 ? null : "***.***.***-" + cpf.substring(9);
    }

    public static String mascararCnpj(String valor) {
        String cnpj = somenteDigitos(valor);
        return cnpj == null || cnpj.length() != 14 ? null : "**.***.***/****-" + cnpj.substring(12);
    }
}
