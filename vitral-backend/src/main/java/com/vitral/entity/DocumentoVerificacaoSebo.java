package com.vitral.entity;

import java.time.OffsetDateTime;
import com.vitral.enumerations.StatusDocumentoSebo;
import com.vitral.enumerations.TipoDocumentoSebo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documento_verificacao_sebo")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentoVerificacaoSebo extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sebo_id", nullable = false)
    private Sebo sebo;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 30)
    private TipoDocumentoSebo tipoDocumento;
    @Column(name = "arquivo_url", nullable = false, length = 500)
    private String arquivoUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusDocumentoSebo status = StatusDocumentoSebo.PENDENTE;
    @Column(name = "enviado_em", nullable = false)
    private OffsetDateTime enviadoEm;
    @Column(name = "analisado_em")
    private OffsetDateTime analisadoEm;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analisado_por")
    private Account analisadoPor;
    @Column(name = "motivo_rejeicao", length = 500)
    private String motivoRejeicao;
}
