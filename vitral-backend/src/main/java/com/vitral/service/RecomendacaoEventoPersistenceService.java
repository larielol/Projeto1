package com.vitral.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.entity.RecomendacaoEvento;
import com.vitral.enumerations.TipoEventoRecomendacao;
import com.vitral.repository.RecomendacaoEventoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecomendacaoEventoPersistenceService {
    private final RecomendacaoEventoRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvar(RecomendacaoEvento evento) {
        repository.save(evento);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvarVisualizacao(RecomendacaoEvento evento, OffsetDateTime inicioDaJanela,
            OffsetDateTime inicioDoDia, int maximoPorDia) {
        Long accountId = evento.getAccount().getId();
        Long produtoId = evento.getProduto().getId();
        if (repository.existsByAccountIdAndProdutoIdAndTipoAndCreatedAtAfter(
                accountId, produtoId, TipoEventoRecomendacao.VISUALIZACAO, inicioDaJanela)) {
            return;
        }
        if (repository.countByAccountIdAndProdutoIdAndTipoAndCreatedAtAfter(
                accountId, produtoId, TipoEventoRecomendacao.VISUALIZACAO, inicioDoDia) >= maximoPorDia) {
            return;
        }
        repository.save(evento);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvarPesquisa(RecomendacaoEvento evento, OffsetDateTime inicioDaJanela,
            OffsetDateTime inicioDoDia, int maximoPorDia) {
        String termo = evento.getTermoPesquisa();
        Long accountId = evento.getAccount().getId();
        if (termo != null && (repository.existsByAccountIdAndTipoAndTermoPesquisaAndCreatedAtAfter(
                accountId, TipoEventoRecomendacao.PESQUISA, termo, inicioDaJanela)
                || repository.countByAccountIdAndTipoAndTermoPesquisaAndCreatedAtAfter(
                        accountId, TipoEventoRecomendacao.PESQUISA, termo, inicioDoDia) >= maximoPorDia)) {
            return;
        }
        repository.save(evento);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void salvarCompra(RecomendacaoEvento evento) {
        if (repository.existsByPedidoIdAndProdutoIdAndTipo(evento.getPedido().getId(),
                evento.getProduto().getId(), TipoEventoRecomendacao.COMPRA_CONCLUIDA)) {
            return;
        }
        repository.save(evento);
    }
}
