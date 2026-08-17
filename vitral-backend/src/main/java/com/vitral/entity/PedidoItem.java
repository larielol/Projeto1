package com.vitral.entity;

import java.math.BigDecimal;

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
@Table(name = "pedido_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "titulo_snapshot", nullable = false, length = 255)
    private String tituloSnapshot;

    @Column(name = "preco_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoSnapshot;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantidade = 1;
}
