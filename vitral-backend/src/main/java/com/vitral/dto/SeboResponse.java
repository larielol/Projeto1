package com.vitral.dto;

import com.vitral.enumerations.StatusVerificacaoSebo;
import com.vitral.enumerations.StatusConsultaCnpj;
import java.time.OffsetDateTime;

public record SeboResponse(
        Long id,
        Long accountId,
        String nome,
        String email,
        String descricao,
        String telefone,
        String cnpj,
        StatusVerificacaoSebo statusVerificacao,
        String motivoRejeicao,
        String razaoSocialReceita,
        StatusConsultaCnpj statusConsultaCnpj,
        OffsetDateTime cnpjConsultadoEm,
        String mensagemConsultaCnpj,
        String fotoUrl,
        String cep,
        String logradouro,
        String cidade,
        String uf,
        String horarioFuncionamento,
        OffsetDateTime dataCriacao,
        OffsetDateTime ultimaAtividade,
        Boolean confirmado,
        Double distanciaKm) {

    public SeboResponse(
            Long id,
            Long accountId,
            String nome,
            String email,
            String descricao,
            String telefone,
            String cnpj,
            StatusVerificacaoSebo statusVerificacao,
            String motivoRejeicao,
            String razaoSocialReceita,
            StatusConsultaCnpj statusConsultaCnpj,
            OffsetDateTime cnpjConsultadoEm,
            String mensagemConsultaCnpj,
            String fotoUrl,
            String cep,
            String logradouro,
            String cidade,
            String uf,
            String horarioFuncionamento,
            OffsetDateTime dataCriacao,
            OffsetDateTime ultimaAtividade,
            Boolean confirmado) {
        this(id, accountId, nome, email, descricao, telefone, cnpj, statusVerificacao, motivoRejeicao,
                razaoSocialReceita, statusConsultaCnpj, cnpjConsultadoEm, mensagemConsultaCnpj, fotoUrl,
                cep, logradouro, cidade, uf, horarioFuncionamento, dataCriacao, ultimaAtividade,
                confirmado, null);
    }

    public SeboResponse(
            Long id,
            Long accountId,
            String nome,
            String email,
            String descricao,
            String telefone,
            String cnpj,
            StatusVerificacaoSebo statusVerificacao,
            String motivoRejeicao,
            String razaoSocialReceita,
            StatusConsultaCnpj statusConsultaCnpj,
            OffsetDateTime cnpjConsultadoEm,
            String mensagemConsultaCnpj,
            String fotoUrl) {
        this(id, accountId, nome, email, descricao, telefone, cnpj, statusVerificacao, motivoRejeicao,
                razaoSocialReceita, statusConsultaCnpj, cnpjConsultadoEm, mensagemConsultaCnpj, fotoUrl,
                null, null, null, null, null, null, null, Boolean.FALSE);
    }

    public SeboResponse(
            Long id,
            Long accountId,
            String nome,
            String email,
            String descricao,
            String telefone,
            String cnpj,
            StatusVerificacaoSebo statusVerificacao,
            String motivoRejeicao,
            String razaoSocialReceita,
            StatusConsultaCnpj statusConsultaCnpj,
            OffsetDateTime cnpjConsultadoEm,
            String mensagemConsultaCnpj,
            String fotoUrl,
            Boolean confirmado) {
        this(id, accountId, nome, email, descricao, telefone, cnpj, statusVerificacao, motivoRejeicao,
                razaoSocialReceita, statusConsultaCnpj, cnpjConsultadoEm, mensagemConsultaCnpj, fotoUrl,
                null, null, null, null, null, null, null, confirmado);
    }

    public SeboResponse(
            Long id,
            Long accountId,
            String nome,
            String email,
            String descricao,
            String telefone,
            String cnpj,
            StatusVerificacaoSebo statusVerificacao,
            String motivoRejeicao,
            String razaoSocialReceita,
            StatusConsultaCnpj statusConsultaCnpj,
            OffsetDateTime cnpjConsultadoEm,
            String mensagemConsultaCnpj,
            String fotoUrl,
            String cep,
            String logradouro,
            String cidade,
            String uf,
            String horarioFuncionamento,
            Boolean confirmado) {
        this(id, accountId, nome, email, descricao, telefone, cnpj, statusVerificacao, motivoRejeicao,
                razaoSocialReceita, statusConsultaCnpj, cnpjConsultadoEm, mensagemConsultaCnpj, fotoUrl,
                cep, logradouro, cidade, uf, horarioFuncionamento, null, null, confirmado);
    }
}
