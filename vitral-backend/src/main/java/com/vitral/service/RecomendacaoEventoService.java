package com.vitral.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.vitral.config.RecomendacaoProperties;
import com.vitral.entity.Account;
import com.vitral.entity.Categoria;
import com.vitral.entity.Pedido;
import com.vitral.entity.Produto;
import com.vitral.entity.RecomendacaoEvento;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.BookGenre;
import com.vitral.enumerations.FaixaPrecoRecomendacao;
import com.vitral.enumerations.TipoEventoRecomendacao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecomendacaoEventoService {
    private static final Logger log = LoggerFactory.getLogger(RecomendacaoEventoService.class);

    private final RecomendacaoEventoPersistenceService persistence;
    private final NormalizadorPesquisaService normalizador;
    private final RecomendacaoProperties properties;

    public void registrarProduto(Account account, TipoEventoRecomendacao tipo, Produto produto) {
        try {
            registrarProdutoInterno(account, tipo, produto, null);
        } catch (RuntimeException exception) {
            log.warn("Falha ao registrar evento de recomendacao do tipo {}", tipo);
        }
    }

    public void registrarCompra(Account account, Pedido pedido, Produto produto) {
        try {
            registrarProdutoInterno(account, TipoEventoRecomendacao.COMPRA_CONCLUIDA, produto, pedido);
        } catch (RuntimeException exception) {
            log.warn("Falha ao registrar evento de compra para recomendacao");
        }
    }

    public void registrarPesquisa(Account account, String termo, Categoria categoria, BookGenre genero,
            BigDecimal precoMin, BigDecimal precoMax) {
        try {
            registrarPesquisaInterno(account, termo, categoria, genero, precoMin, precoMax);
        } catch (RuntimeException exception) {
            log.warn("Falha ao registrar evento de pesquisa para recomendacao");
        }
    }

    private void registrarPesquisaInterno(Account account, String termo, Categoria categoria, BookGenre genero,
            BigDecimal precoMin, BigDecimal precoMax) {
        if (!rastreavel(account)) return;
        String normalizado = normalizador.normalizar(termo);
        if (normalizado == null && categoria == null && genero == null && precoMin == null && precoMax == null) return;
        OffsetDateTime agora = OffsetDateTime.now();
        BigDecimal referencia = precoMin != null && precoMax != null
                ? precoMin.add(precoMax).divide(BigDecimal.valueOf(2)) : (precoMin != null ? precoMin : precoMax);
        RecomendacaoEvento evento = RecomendacaoEvento.builder().account(account)
                .tipo(TipoEventoRecomendacao.PESQUISA)
                .categoria(categoria).tipoProduto(categoria == null ? null : categoria.getSlug())
                .genero(genero == null ? null : genero.name())
                .faixaPreco(referencia == null ? null : FaixaPrecoRecomendacao.de(referencia).name())
                .termoPesquisa(normalizado).build();
        executarSeguro(evento.getTipo(), () -> persistence.salvarPesquisa(evento,
                agora.minusMinutes(properties.janelaVisualizacaoMinutos()), inicioDoDia(agora),
                properties.eventosRepetidosDiaMaximo()));
    }

    private void registrarProdutoInterno(Account account, TipoEventoRecomendacao tipo, Produto produto, Pedido pedido) {
        if (!rastreavel(account) || produto == null) return;
        OffsetDateTime agora = OffsetDateTime.now();
        RecomendacaoEvento evento = RecomendacaoEvento.builder().account(account).tipo(tipo).produto(produto)
                .categoria(produto.getCategoria()).sebo(produto.getSebo()).pedido(pedido)
                .tipoProduto(produto.getCategoria() == null ? null : produto.getCategoria().getSlug())
                .genero(produto.getBookGenre() == null ? null : produto.getBookGenre().name())
                .autorArtista(normalizarTexto(produto.getAutor()))
                .faixaPreco(FaixaPrecoRecomendacao.de(produto.getPreco()).name()).build();
        executarSeguro(tipo, () -> {
            if (tipo == TipoEventoRecomendacao.VISUALIZACAO) {
                persistence.salvarVisualizacao(evento, agora.minusMinutes(properties.janelaVisualizacaoMinutos()),
                        inicioDoDia(agora), properties.eventosRepetidosDiaMaximo());
            } else if (tipo == TipoEventoRecomendacao.COMPRA_CONCLUIDA) {
                persistence.salvarCompra(evento);
            } else {
                persistence.salvar(evento);
            }
        });
    }

    private boolean rastreavel(Account account) {
        return account != null && account.getId() != null && account.getType() == AccountType.USUARIO;
    }

    private void executarSeguro(TipoEventoRecomendacao tipo, Runnable registro) {
        try {
            registro.run();
        } catch (RuntimeException exception) {
            log.warn("Falha ao registrar evento de recomendacao do tipo {}", tipo);
        }
    }

    private String normalizarTexto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase();
    }

    private OffsetDateTime inicioDoDia(OffsetDateTime agora) {
        return agora.toLocalDate().atStartOfDay().atOffset(agora.getOffset());
    }
}
