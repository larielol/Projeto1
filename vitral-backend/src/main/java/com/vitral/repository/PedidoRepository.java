package com.vitral.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vitral.entity.Pedido;
import com.vitral.enumerations.StatusPedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Histórico de pedidos do usuário (paginado)
    Page<Pedido> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
    Page<Pedido> findByAccountIdAndStatusOrderByCreatedAtDesc(Long accountId, StatusPedido status, Pageable pageable);

    // Histórico de vendas do sebo (paginado)
    Page<Pedido> findBySeboIdOrderByCreatedAtDesc(Long seboId, Pageable pageable);
    Page<Pedido> findBySeboIdAndStatusOrderByCreatedAtDesc(Long seboId, StatusPedido status, Pageable pageable);

    // Para verificar se há pedido ativo com aquele produto (evitar duplicidade)
    List<Pedido> findByAccountId(Long accountId);

    @Query("""
            select case when count(p) > 0 then true else false end
            from Pedido p
            join p.itens i
            where i.produto.id = :produtoId
              and p.status = com.vitral.enumerations.StatusPedido.AGUARDANDO_CONFIRMACAO
            """)
    boolean existsPedidoPendenteComProduto(@Param("produtoId") Long produtoId);

    @Query("""
            select coalesce(sum(i.quantidade), 0)
            from Pedido p
            join p.itens i
            where i.produto.id = :produtoId
              and p.status = com.vitral.enumerations.StatusPedido.AGUARDANDO_CONFIRMACAO
            """)
    Long sumQuantidadeReservadaEmPedidosPendentes(@Param("produtoId") Long produtoId);

    @Query("""
            select month(coalesce(p.pagoEm, p.confirmadoEm)), coalesce(sum(p.total), 0)
            from Pedido p
            where p.sebo.id = :seboId
              and year(coalesce(p.pagoEm, p.confirmadoEm)) = :ano
              and p.status in (com.vitral.enumerations.StatusPedido.CONFIRMADO,
                               com.vitral.enumerations.StatusPedido.REEMBOLSADO)
              and p.statusPagamento = com.vitral.enumerations.StatusPagamento.APROVADO
            group by month(coalesce(p.pagoEm, p.confirmadoEm))
            order by month(coalesce(p.pagoEm, p.confirmadoEm))
            """)
    List<Object[]> vendasOnlineMensais(@Param("seboId") Long seboId, @Param("ano") int ano);

    @Query("""
            select month(p.reembolsadoEm), coalesce(sum(p.total), 0)
            from Pedido p
            where p.sebo.id = :seboId
              and year(p.reembolsadoEm) = :ano
              and p.status = com.vitral.enumerations.StatusPedido.REEMBOLSADO
            group by month(p.reembolsadoEm)
            order by month(p.reembolsadoEm)
            """)
    List<Object[]> reembolsosMensais(@Param("seboId") Long seboId, @Param("ano") int ano);
}
