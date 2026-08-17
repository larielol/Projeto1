package com.vitral.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import com.vitral.enumerations.StatusVerificacaoSebo;
import com.vitral.enumerations.StatusConsultaCnpj;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sebo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sebo extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @Column(length = 2000)
    private String descricao;

    @Column(length = 20)
    private String telefone;

    @Column(length = 14, unique = true)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_verificacao", nullable = false, length = 20)
    @Builder.Default
    private StatusVerificacaoSebo statusVerificacao = StatusVerificacaoSebo.PENDENTE;

    @Column(name = "verificado_em")
    private OffsetDateTime verificadoEm;

    @Column(name = "motivo_rejeicao", length = 500)
    private String motivoRejeicao;

    @Column(name = "razao_social_receita", length = 255)
    private String razaoSocialReceita;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_consulta_cnpj", nullable = false, length = 20)
    @Builder.Default
    private StatusConsultaCnpj statusConsultaCnpj = StatusConsultaCnpj.NAO_CONSULTADO;

    @Column(name = "cnpj_consultado_em")
    private OffsetDateTime cnpjConsultadoEm;

    @Column(name = "mensagem_consulta_cnpj", length = 500)
    private String mensagemConsultaCnpj;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(length = 8)
    private String cep;

    @Column(length = 255)
    private String logradouro;

    @Column(length = 120)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(name = "horario_funcionamento", length = 255)
    private String horarioFuncionamento;

    private Double latitude;

    private Double longitude;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    private OffsetDateTime dataCriacao;

    @Column(name = "ultima_atividade")
    private OffsetDateTime ultimaAtividade;

    @Column(name = "confirmado", nullable = false)
    @Builder.Default
    private Boolean confirmado = Boolean.FALSE;
}
