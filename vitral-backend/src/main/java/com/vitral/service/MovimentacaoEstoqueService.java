package com.vitral.service;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vitral.dto.MovimentacaoEstoqueResponse;
import com.vitral.entity.Account;
import com.vitral.entity.MovimentacaoEstoque;
import com.vitral.entity.Produto;
import com.vitral.enumerations.TipoMovimentacaoEstoque;
import com.vitral.repository.MovimentacaoEstoqueRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository repository;
    private final SeboService seboService;

    @Transactional
    public MovimentacaoEstoque registrarAlteracao(Produto produto, Account operador,
            TipoMovimentacaoEstoque tipo, int quantidade, int antes, int depois,
            BigDecimal valorUnitario, String observacao) {
        return salvar(produto, operador, tipo, quantidade, antes, depois, valorUnitario, observacao, null);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoqueResponse> listar(Account account, Pageable pageable) {
        return repository.findBySeboAccountIdOrderByCreatedAtDesc(account.getId(), pageable).map(this::toResponse);
    }

    private MovimentacaoEstoque salvar(Produto produto, Account operador, TipoMovimentacaoEstoque tipo,
            int quantidade, int antes, int depois, BigDecimal valorUnitario, String observacao,
            MovimentacaoEstoque origem) {
        BigDecimal total = valorUnitario == null ? null : valorUnitario.multiply(BigDecimal.valueOf(quantidade));
        return repository.save(MovimentacaoEstoque.builder()
                .produto(produto).sebo(produto.getSebo()).operador(operador).movimentacaoOrigem(origem)
                .tipo(tipo).quantidade(quantidade).estoqueAntes(antes).estoqueDepois(depois)
                .valorUnitario(valorUnitario).valorTotal(total).observacao(observacao).build());
    }

    private MovimentacaoEstoqueResponse toResponse(MovimentacaoEstoque m) {
        return new MovimentacaoEstoqueResponse(m.getId(), m.getProduto().getId(), m.getSebo().getId(),
                m.getOperador().getId(), m.getMovimentacaoOrigem() == null ? null : m.getMovimentacaoOrigem().getId(),
                m.getTipo(), m.getQuantidade(), m.getEstoqueAntes(), m.getEstoqueDepois(), m.getValorUnitario(),
                m.getValorTotal(), m.getObservacao(), m.getCreatedAt());
    }
}
