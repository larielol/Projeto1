package com.vitral.entity;

import java.math.BigDecimal;
import com.vitral.enumerations.TipoMovimentacaoEstoque;
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
@Table(name = "movimentacao_estoque")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoque extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sebo_id", nullable = false)
    private Sebo sebo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operador_id", nullable = false)
    private Account operador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimentacao_origem_id")
    private MovimentacaoEstoque movimentacaoOrigem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "estoque_antes", nullable = false)
    private Integer estoqueAntes;

    @Column(name = "estoque_depois", nullable = false)
    private Integer estoqueDepois;

    @Column(name = "valor_unitario", precision = 12, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total", precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(length = 500)
    private String observacao;
}
