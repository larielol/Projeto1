package com.vitral.mapper;

import com.vitral.dto.SeboResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.StatusConsultaCnpj;
import com.vitral.enumerations.StatusVerificacaoSebo;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-13T08:51:43-0300",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class SeboMapperImpl implements SeboMapper {

    @Override
    public SeboResponse toResponse(Sebo sebo) {
        if ( sebo == null ) {
            return null;
        }

        Long accountId = null;
        String nome = null;
        String email = null;
        Long id = null;
        String descricao = null;
        String telefone = null;
        String cnpj = null;
        StatusVerificacaoSebo statusVerificacao = null;
        String motivoRejeicao = null;
        String razaoSocialReceita = null;
        StatusConsultaCnpj statusConsultaCnpj = null;
        OffsetDateTime cnpjConsultadoEm = null;
        String mensagemConsultaCnpj = null;
        String fotoUrl = null;
        String cep = null;
        String logradouro = null;
        String cidade = null;
        String uf = null;
        String horarioFuncionamento = null;
        OffsetDateTime dataCriacao = null;
        OffsetDateTime ultimaAtividade = null;
        Boolean confirmado = null;

        accountId = seboAccountId( sebo );
        nome = seboAccountName( sebo );
        email = seboAccountEmail( sebo );
        id = sebo.getId();
        descricao = sebo.getDescricao();
        telefone = sebo.getTelefone();
        cnpj = sebo.getCnpj();
        statusVerificacao = sebo.getStatusVerificacao();
        motivoRejeicao = sebo.getMotivoRejeicao();
        razaoSocialReceita = sebo.getRazaoSocialReceita();
        statusConsultaCnpj = sebo.getStatusConsultaCnpj();
        cnpjConsultadoEm = sebo.getCnpjConsultadoEm();
        mensagemConsultaCnpj = sebo.getMensagemConsultaCnpj();
        fotoUrl = sebo.getFotoUrl();
        cep = sebo.getCep();
        logradouro = sebo.getLogradouro();
        cidade = sebo.getCidade();
        uf = sebo.getUf();
        horarioFuncionamento = sebo.getHorarioFuncionamento();
        dataCriacao = sebo.getDataCriacao();
        ultimaAtividade = sebo.getUltimaAtividade();
        confirmado = sebo.getConfirmado();

        Double distanciaKm = null;

        SeboResponse seboResponse = new SeboResponse( id, accountId, nome, email, descricao, telefone, cnpj, statusVerificacao, motivoRejeicao, razaoSocialReceita, statusConsultaCnpj, cnpjConsultadoEm, mensagemConsultaCnpj, fotoUrl, cep, logradouro, cidade, uf, horarioFuncionamento, dataCriacao, ultimaAtividade, confirmado, distanciaKm );

        return seboResponse;
    }

    @Override
    public SeboResponse toResponse(Sebo sebo, Double distanciaKm) {
        if ( sebo == null && distanciaKm == null ) {
            return null;
        }

        Long accountId = null;
        String nome = null;
        String email = null;
        Long id = null;
        String descricao = null;
        String telefone = null;
        String cnpj = null;
        StatusVerificacaoSebo statusVerificacao = null;
        String motivoRejeicao = null;
        String razaoSocialReceita = null;
        StatusConsultaCnpj statusConsultaCnpj = null;
        OffsetDateTime cnpjConsultadoEm = null;
        String mensagemConsultaCnpj = null;
        String fotoUrl = null;
        String cep = null;
        String logradouro = null;
        String cidade = null;
        String uf = null;
        String horarioFuncionamento = null;
        OffsetDateTime dataCriacao = null;
        OffsetDateTime ultimaAtividade = null;
        Boolean confirmado = null;
        if ( sebo != null ) {
            accountId = seboAccountId( sebo );
            nome = seboAccountName( sebo );
            email = seboAccountEmail( sebo );
            id = sebo.getId();
            descricao = sebo.getDescricao();
            telefone = sebo.getTelefone();
            cnpj = sebo.getCnpj();
            statusVerificacao = sebo.getStatusVerificacao();
            motivoRejeicao = sebo.getMotivoRejeicao();
            razaoSocialReceita = sebo.getRazaoSocialReceita();
            statusConsultaCnpj = sebo.getStatusConsultaCnpj();
            cnpjConsultadoEm = sebo.getCnpjConsultadoEm();
            mensagemConsultaCnpj = sebo.getMensagemConsultaCnpj();
            fotoUrl = sebo.getFotoUrl();
            cep = sebo.getCep();
            logradouro = sebo.getLogradouro();
            cidade = sebo.getCidade();
            uf = sebo.getUf();
            horarioFuncionamento = sebo.getHorarioFuncionamento();
            dataCriacao = sebo.getDataCriacao();
            ultimaAtividade = sebo.getUltimaAtividade();
            confirmado = sebo.getConfirmado();
        }
        Double distanciaKm1 = null;
        distanciaKm1 = distanciaKm;

        SeboResponse seboResponse = new SeboResponse( id, accountId, nome, email, descricao, telefone, cnpj, statusVerificacao, motivoRejeicao, razaoSocialReceita, statusConsultaCnpj, cnpjConsultadoEm, mensagemConsultaCnpj, fotoUrl, cep, logradouro, cidade, uf, horarioFuncionamento, dataCriacao, ultimaAtividade, confirmado, distanciaKm1 );

        return seboResponse;
    }

    @Override
    public SeboResponse toPublicResponse(Sebo sebo) {
        if ( sebo == null ) {
            return null;
        }

        Long accountId = null;
        String nome = null;
        String cnpj = null;
        Long id = null;
        String descricao = null;
        StatusVerificacaoSebo statusVerificacao = null;
        StatusConsultaCnpj statusConsultaCnpj = null;
        OffsetDateTime cnpjConsultadoEm = null;
        String fotoUrl = null;
        OffsetDateTime dataCriacao = null;
        OffsetDateTime ultimaAtividade = null;
        Boolean confirmado = null;

        accountId = seboAccountId( sebo );
        nome = seboAccountName( sebo );
        cnpj = maskCnpj( sebo.getCnpj() );
        id = sebo.getId();
        descricao = sebo.getDescricao();
        statusVerificacao = sebo.getStatusVerificacao();
        statusConsultaCnpj = sebo.getStatusConsultaCnpj();
        cnpjConsultadoEm = sebo.getCnpjConsultadoEm();
        fotoUrl = sebo.getFotoUrl();
        dataCriacao = sebo.getDataCriacao();
        ultimaAtividade = sebo.getUltimaAtividade();
        confirmado = sebo.getConfirmado();

        String email = null;
        String telefone = null;
        String cep = null;
        String logradouro = null;
        String cidade = null;
        String uf = null;
        String horarioFuncionamento = null;
        String motivoRejeicao = null;
        String razaoSocialReceita = null;
        String mensagemConsultaCnpj = null;
        Double distanciaKm = null;

        SeboResponse seboResponse = new SeboResponse( id, accountId, nome, email, descricao, telefone, cnpj, statusVerificacao, motivoRejeicao, razaoSocialReceita, statusConsultaCnpj, cnpjConsultadoEm, mensagemConsultaCnpj, fotoUrl, cep, logradouro, cidade, uf, horarioFuncionamento, dataCriacao, ultimaAtividade, confirmado, distanciaKm );

        return seboResponse;
    }

    @Override
    public SeboResponse toPublicResponse(Sebo sebo, Double distanciaKm) {
        if ( sebo == null && distanciaKm == null ) {
            return null;
        }

        Long accountId = null;
        String nome = null;
        String cnpj = null;
        Long id = null;
        String descricao = null;
        StatusVerificacaoSebo statusVerificacao = null;
        StatusConsultaCnpj statusConsultaCnpj = null;
        OffsetDateTime cnpjConsultadoEm = null;
        String fotoUrl = null;
        OffsetDateTime dataCriacao = null;
        OffsetDateTime ultimaAtividade = null;
        Boolean confirmado = null;
        if ( sebo != null ) {
            accountId = seboAccountId( sebo );
            nome = seboAccountName( sebo );
            cnpj = maskCnpj( sebo.getCnpj() );
            id = sebo.getId();
            descricao = sebo.getDescricao();
            statusVerificacao = sebo.getStatusVerificacao();
            statusConsultaCnpj = sebo.getStatusConsultaCnpj();
            cnpjConsultadoEm = sebo.getCnpjConsultadoEm();
            fotoUrl = sebo.getFotoUrl();
            dataCriacao = sebo.getDataCriacao();
            ultimaAtividade = sebo.getUltimaAtividade();
            confirmado = sebo.getConfirmado();
        }
        Double distanciaKm1 = null;
        distanciaKm1 = distanciaKm;

        String email = null;
        String telefone = null;
        String cep = null;
        String logradouro = null;
        String cidade = null;
        String uf = null;
        String horarioFuncionamento = null;
        String motivoRejeicao = null;
        String razaoSocialReceita = null;
        String mensagemConsultaCnpj = null;

        SeboResponse seboResponse = new SeboResponse( id, accountId, nome, email, descricao, telefone, cnpj, statusVerificacao, motivoRejeicao, razaoSocialReceita, statusConsultaCnpj, cnpjConsultadoEm, mensagemConsultaCnpj, fotoUrl, cep, logradouro, cidade, uf, horarioFuncionamento, dataCriacao, ultimaAtividade, confirmado, distanciaKm1 );

        return seboResponse;
    }

    private Long seboAccountId(Sebo sebo) {
        Account account = sebo.getAccount();
        if ( account == null ) {
            return null;
        }
        return account.getId();
    }

    private String seboAccountName(Sebo sebo) {
        Account account = sebo.getAccount();
        if ( account == null ) {
            return null;
        }
        return account.getName();
    }

    private String seboAccountEmail(Sebo sebo) {
        Account account = sebo.getAccount();
        if ( account == null ) {
            return null;
        }
        return account.getEmail();
    }
}
