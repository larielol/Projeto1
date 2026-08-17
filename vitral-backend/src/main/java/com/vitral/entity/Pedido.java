package com.vitral.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.OffsetDateTime;

import com.vitral.enumerations.FormaPagamento;
import com.vitral.enumerations.StatusPagamento;
import com.vitral.enumerations.StatusPedido;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pedido extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sebo_id", nullable = false)
    private Sebo sebo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusPedido status = StatusPedido.AGUARDANDO_CONFIRMACAO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    @Builder.Default
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Column(name = "confirmado_em")
    private OffsetDateTime confirmadoEm;

    @Column(name = "pago_em")
    private OffsetDateTime pagoEm;

    @Column(name = "cancelado_em")
    private OffsetDateTime canceladoEm;

    @Column(name = "reembolsado_em")
    private OffsetDateTime reembolsadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PedidoItem> itens = new ArrayList<>();
}
