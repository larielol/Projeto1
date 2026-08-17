package com.vitral.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "oferta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Oferta extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoPromocional;

    @Column(length = 500)
    private String descricao;

    @Column(name = "inicio_em")
    private OffsetDateTime inicioEm;

    @Column(name = "fim_em")
    private OffsetDateTime fimEm;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativa = Boolean.TRUE;
}
