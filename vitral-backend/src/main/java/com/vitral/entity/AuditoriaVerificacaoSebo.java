package com.vitral.entity;

import com.vitral.enumerations.StatusVerificacaoSebo;
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

@Entity
@Table(name = "auditoria_verificacao_sebo")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditoriaVerificacaoSebo extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sebo_id", nullable = false)
    private Sebo sebo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analisado_por", nullable = false)
    private Account analisadoPor;
    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", nullable = false, length = 20)
    private StatusVerificacaoSebo statusAnterior;
    @Enumerated(EnumType.STRING)
    @Column(name = "novo_status", nullable = false, length = 20)
    private StatusVerificacaoSebo novoStatus;
    @Column(length = 500)
    private String motivo;
}
