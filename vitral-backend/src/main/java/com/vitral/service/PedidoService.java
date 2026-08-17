package com.vitral.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.dto.ConfirmarPedidoRequest;
import com.vitral.dto.FaturamentoMensalResponse;
import com.vitral.dto.PedidoItemResponse;
import com.vitral.dto.PedidoResponse;
import com.vitral.entity.Account;
import com.vitral.entity.CestaItem;
import com.vitral.entity.Pedido;
import com.vitral.entity.PedidoItem;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.FormaPagamento;
import com.vitral.enumerations.StatusPagamento;
import com.vitral.enumerations.StatusPedido;
import com.vitral.enumerations.TipoMovimentacaoEstoque;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.CestaItemRepository;
import com.vitral.repository.PedidoRepository;
import com.vitral.repository.SeboRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final CestaItemRepository cestaItemRepository;
    private final SeboRepository seboRepository;
    private final EmailService emailService;
    private final PrecoService precoService;
    private final MovimentacaoEstoqueService movimentacaoEstoqueService;
    private final PerfilCompraValidator perfilCompraValidator;
    private final RecomendacaoEventoService recomendacaoEventoService;

    @Transactional
    public PedidoResponse confirmarPedido(Account account, ConfirmarPedidoRequest pagamento) {
        perfilCompraValidator.validar(account);
        List<CestaItem> itens = cestaItemRepository.findByAccountIdComProdutoESebo(account.getId());
        if (itens.isEmpty()) {
            throw new BusinessException("A cesta esta vazia", HttpStatus.BAD_REQUEST);
        }

        boolean possuiProdutoInativo = itens.stream()
                .anyMatch(i -> !Boolean.TRUE.equals(i.getProduto().getAtivo()) || i.getProduto().getEstoque() <= 0);
        if (possuiProdutoInativo) {
            throw new BusinessException(
                    "A cesta contem produtos que nao estao mais disponiveis",
                    HttpStatus.BAD_REQUEST);
        }

        Long seboId = itens.getFirst().getProduto().getSebo().getId();
        boolean sebosDistintos = itens.stream()
                .anyMatch(i -> !i.getProduto().getSebo().getId().equals(seboId));
        if (sebosDistintos) {
            throw new BusinessException(
                    "Todos os produtos da cesta devem pertencer ao mesmo sebo para realizar um pedido",
                    HttpStatus.BAD_REQUEST);
        }

        itens.forEach(this::validarQuantidadeDisponivel);

        if (!pagamentoAprovado(pagamento)) {
            throw new BusinessException(
                    "Pagamento recusado. Verifique os dados do cartao e tente novamente.",
                    HttpStatus.PAYMENT_REQUIRED);
        }

        Sebo sebo = seboRepository.findById(seboId)
                .orElseThrow(() -> new ResourceNotFoundException("Sebo nao encontrado"));

        Map<Long, BigDecimal> precoEfetivoPorProduto = itens.stream()
                .map(CestaItem::getProduto)
                .collect(Collectors.toMap(Produto::getId, precoService::precoEfetivo, (a, b) -> a));

        BigDecimal total = itens.stream()
                .map(i -> precoEfetivoPorProduto.get(i.getProduto().getId())
                        .multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido pedido = Pedido.builder()
                .account(account)
                .sebo(sebo)
                .status(StatusPedido.AGUARDANDO_CONFIRMACAO)
                .formaPagamento(pagamento.formaPagamento())
                .statusPagamento(StatusPagamento.APROVADO)
                .pagoEm(OffsetDateTime.now())
                .total(total)
                .build();

        List<PedidoItem> pedidoItens = itens.stream()
                .map(i -> PedidoItem.builder()
                        .pedido(pedido)
                        .produto(i.getProduto())
                        .tituloSnapshot(i.getProduto().getTitulo())
                        .precoSnapshot(precoEfetivoPorProduto.get(i.getProduto().getId()))
                        .quantidade(i.getQuantidade())
                        .build())
                .toList();

        pedido.getItens().addAll(pedidoItens);
        Pedido salvo = pedidoRepository.save(pedido);
        cestaItemRepository.deleteByAccountId(account.getId());
        notificarSeboNovoPedido(salvo);
        return toResponse(salvo);
    }

    private boolean pagamentoAprovado(ConfirmarPedidoRequest pagamento) {
        if (pagamento.formaPagamento() == FormaPagamento.CARTAO) {
            String numero = pagamento.numeroCartao() == null
                    ? ""
                    : pagamento.numeroCartao().replaceAll("\\D", "");
            return !numero.endsWith("0000");
        }
        return true;
    }

    private void notificarSeboNovoPedido(Pedido pedido) {
        try {
            emailService.enviarNotificacaoNovoPedido(
                    pedido.getSebo().getAccount().getEmail(),
                    pedido.getId(),
                    pedido.getAccount().getName(),
                    pedido.getTotal());
        } catch (RuntimeException exception) {
            log.warn("Falha ao notificar o sebo sobre o novo pedido {}", pedido.getId(), exception);
        }
    }

    @Transactional
    public PedidoResponse confirmarCompra(Account account, Long pedidoId, StatusPedido novoStatus) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido nao encontrado"));

        if (!pedido.getSebo().getAccount().getId().equals(account.getId())) {
            throw new BusinessException("Pedido nao pertence ao sebo da conta autenticada", HttpStatus.FORBIDDEN);
        }
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_CONFIRMACAO) {
            throw new BusinessException("Apenas pedidos com status AGUARDANDO_CONFIRMACAO podem ser atualizados",
                    HttpStatus.BAD_REQUEST);
        }
        if (novoStatus != StatusPedido.CONFIRMADO && novoStatus != StatusPedido.CANCELADO) {
            throw new BusinessException("Status invalido. Use CONFIRMADO ou CANCELADO", HttpStatus.BAD_REQUEST);
        }

        pedido.setStatus(novoStatus);
        if (novoStatus == StatusPedido.CONFIRMADO) {
            pedido.setConfirmadoEm(OffsetDateTime.now());
            pedido.getItens().forEach(item -> {
                int estoqueAtual = item.getProduto().getEstoque();
                if (estoqueAtual < item.getQuantidade()) {
                    throw new BusinessException("Estoque insuficiente para confirmar o pedido", HttpStatus.BAD_REQUEST);
                }
                int novoEstoque = estoqueAtual - item.getQuantidade();
                item.getProduto().setEstoque(novoEstoque);
                item.getProduto().setAtivo(novoEstoque > 0);
                movimentacaoEstoqueService.registrarAlteracao(item.getProduto(), account,
                        TipoMovimentacaoEstoque.VENDA_ONLINE, item.getQuantidade(), estoqueAtual, novoEstoque,
                        item.getPrecoSnapshot(), "Pedido online #" + pedido.getId());
            });
            registrarComprasAposCommit(pedido);
        } else {
            pedido.setCanceladoEm(OffsetDateTime.now());
        }
        return toResponse(pedido);
    }

    private void registrarComprasAposCommit(Pedido pedido) {
        Runnable registrar = () -> pedido.getItens().forEach(item -> recomendacaoEventoService.registrarCompra(
                pedido.getAccount(), pedido, item.getProduto()));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { registrar.run(); }
            });
        } else {
            registrar.run();
        }
    }

    @Transactional
    public PedidoResponse cancelarPedidoUsuario(Account account, Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido nao encontrado"));

        if (!pedido.getAccount().getId().equals(account.getId())) {
            throw new BusinessException("Pedido nao pertence ao usuario autenticado", HttpStatus.FORBIDDEN);
        }

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_CONFIRMACAO) {
            throw new BusinessException("Apenas pedidos aguardando confirmacao podem ser cancelados pelo usuario",
                    HttpStatus.BAD_REQUEST);
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setCanceladoEm(OffsetDateTime.now());
        return toResponse(pedido);
    }

    @Transactional
    public PedidoResponse cancelarPedido(Account account, Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido nao encontrado"));

        if (!pedido.getAccount().getId().equals(account.getId())) {
            throw new BusinessException("Pedido nao pertence ao usuario autenticado", HttpStatus.FORBIDDEN);
        }
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_CONFIRMACAO) {
            throw new BusinessException(
                    "Apenas pedidos com status AGUARDANDO_CONFIRMACAO podem ser cancelados",
                    HttpStatus.BAD_REQUEST);
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setCanceladoEm(OffsetDateTime.now());
        return toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponse> historicoPedidosUsuario(Account account, StatusPedido status, Pageable pageable) {
        Page<Pedido> pedidos = status == null
                ? pedidoRepository.findByAccountIdOrderByCreatedAtDesc(account.getId(), pageable)
                : pedidoRepository.findByAccountIdAndStatusOrderByCreatedAtDesc(account.getId(), status, pageable);
        return pedidos
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponse> historicoVendasSebo(Account account, StatusPedido status, Pageable pageable) {
        Sebo sebo = seboRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sebo nao encontrado para esta conta"));
        Page<Pedido> pedidos = status == null
                ? pedidoRepository.findBySeboIdOrderByCreatedAtDesc(sebo.getId(), pageable)
                : pedidoRepository.findBySeboIdAndStatusOrderByCreatedAtDesc(sebo.getId(), status, pageable);
        return pedidos
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<FaturamentoMensalResponse> faturamentoMensal(Account account, int ano) {
        if (ano < 2000 || ano > 2100) {
            throw new BusinessException("Ano invalido", HttpStatus.BAD_REQUEST);
        }
        Sebo sebo = seboRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sebo nao encontrado para esta conta"));
        Map<Integer, BigDecimal> online = porMes(pedidoRepository.vendasOnlineMensais(sebo.getId(), ano));
        Map<Integer, BigDecimal> reembolsos = porMes(pedidoRepository.reembolsosMensais(sebo.getId(), ano));
        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(mes -> {
                    BigDecimal vendasOnline = online.getOrDefault(mes, BigDecimal.ZERO);
                    BigDecimal valorReembolsos = reembolsos.getOrDefault(mes, BigDecimal.ZERO);
                    BigDecimal liquido = vendasOnline.subtract(valorReembolsos);
                    return new FaturamentoMensalResponse(ano, mes, vendasOnline, valorReembolsos, liquido, liquido);
                })
                .toList();
    }

    private Map<Integer, BigDecimal> porMes(List<Object[]> valores) {
        return valores.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).intValue(), row -> (BigDecimal) row[1], BigDecimal::add));
    }

    private PedidoResponse toResponse(Pedido pedido) {
        List<PedidoItemResponse> itensResponse = pedido.getItens().stream()
                .map(i -> new PedidoItemResponse(
                        i.getId(),
                        i.getProduto().getId(),
                        i.getTituloSnapshot(),
                        i.getPrecoSnapshot(),
                        i.getQuantidade()))
                .toList();
        return new PedidoResponse(
                pedido.getId(),
                pedido.getAccount().getId(),
                pedido.getSebo().getId(),
                pedido.getStatus(),
                pedido.getFormaPagamento(),
                pedido.getStatusPagamento(),
                pedido.getTotal(),
                pedido.getCreatedAt(),
                pedido.getConfirmadoEm(),
                pedido.getPagoEm(),
                pedido.getCanceladoEm(),
                pedido.getReembolsadoEm(),
                itensResponse);
    }

    @Transactional
    public PedidoResponse reembolsarPedido(Account account, Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido nao encontrado"));
        if (!pedido.getSebo().getAccount().getId().equals(account.getId())) {
            throw new BusinessException("Pedido nao pertence ao sebo da conta autenticada", HttpStatus.FORBIDDEN);
        }
        if (pedido.getStatus() != StatusPedido.CONFIRMADO || pedido.getReembolsadoEm() != null) {
            throw new BusinessException("Apenas pedidos confirmados podem ser reembolsados", HttpStatus.BAD_REQUEST);
        }
        pedido.getItens().forEach(item -> {
            Produto produto = item.getProduto();
            int antes = produto.getEstoque();
            int depois = antes + item.getQuantidade();
            produto.setEstoque(depois);
            produto.setAtivo(true);
            movimentacaoEstoqueService.registrarAlteracao(produto, account, TipoMovimentacaoEstoque.ESTORNO,
                    item.getQuantidade(), antes, depois, item.getPrecoSnapshot(),
                    "Reembolso do pedido online #" + pedido.getId());
        });
        pedido.setStatus(StatusPedido.REEMBOLSADO);
        pedido.setReembolsadoEm(OffsetDateTime.now());
        return toResponse(pedido);
    }

    private void validarQuantidadeDisponivel(CestaItem item) {
        Long quantidadeReservada = pedidoRepository
                .sumQuantidadeReservadaEmPedidosPendentes(item.getProduto().getId());
        int disponivel = item.getProduto().getEstoque() - quantidadeReservada.intValue();
        if (item.getQuantidade() > disponivel) {
            throw new BusinessException(
                    "A cesta contem produtos sem estoque suficiente",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
