package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vitral.dto.CestaItemResponse;
import com.vitral.dto.MensagemResponse;
import com.vitral.entity.Account;
import com.vitral.entity.CestaItem;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.exception.BusinessException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.CestaItemRepository;
import com.vitral.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class CestaServiceTest {

    @Mock RecomendacaoEventoService recomendacaoEventoService;

    @Mock
    private CestaItemRepository cestaItemRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private PrecoService precoService;

    @InjectMocks
    private CestaService cestaService;

    private Account buildUsuario() {
        return Account.builder()
                .name("Joao")
                .email("joao@email.com")
                .passwordHash("hash")
                .type(AccountType.USUARIO)
                .emailVerificado(true)
                .build();
    }

    private Produto buildProduto() {
        Account seboAccount = Account.builder()
                .name("Sebo Teste")
                .email("sebo@email.com")
                .passwordHash("hash")
                .type(AccountType.SEBO)
                .emailVerificado(true)
                .build();
        Sebo sebo = Sebo.builder().account(seboAccount).build();
        return Produto.builder()
                .sebo(sebo)
                .titulo("Dom Casmurro")
                .autor("Machado de Assis")
                .preco(new BigDecimal("29.90"))
                .estoque(5)
                .condicao(CondicaoProduto.USADO)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("Deve incrementar quantidade quando produto já está na cesta")
    void adicionarItem_produtoJaNaCesta_incrementaQuantidade() {
        Account account = buildUsuario();
        Produto produto = buildProduto();
        CestaItem cestaItem = CestaItem.builder().account(account).produto(produto).quantidade(1).build();
        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));
        when(cestaItemRepository.findByAccountIdAndProdutoId(account.getId(), 1L)).thenReturn(Optional.of(cestaItem));

        MensagemResponse response = cestaService.adicionarItem(account, 1L);

        assertThat(response.mensagem()).isEqualTo("Produto adicionado a cesta");
        assertThat(cestaItem.getQuantidade()).isEqualTo(2);
        verify(cestaItemRepository).save(cestaItem);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando produto não existe ou está inativo")
    void adicionarItem_produtoNaoEncontrado_lancaResourceNotFoundException() {
        Account account = buildUsuario();
        when(produtoRepository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cestaService.adicionarItem(account, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cestaItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve adicionar item à cesta e retornar mensagem de sucesso")
    void adicionarItem_sucesso_salvaCestaItemERetornaMensagem() {
        Account account = buildUsuario();
        Produto produto = buildProduto();
        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));

        MensagemResponse response = cestaService.adicionarItem(account, 1L);

        assertThat(response.mensagem()).isEqualTo("Produto adicionado a cesta");
        verify(cestaItemRepository).save(any(CestaItem.class));
        verify(recomendacaoEventoService).registrarProduto(account,
                com.vitral.enumerations.TipoEventoRecomendacao.CESTA_ADICIONADO, produto);
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando quantidade excede o estoque")
    void adicionarItem_quantidadeMaiorQueEstoque_lancaBusinessException() {
        Account account = buildUsuario();
        Produto produto = buildProduto();
        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> cestaService.adicionarItem(account, 1L, 6))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("estoque");

        verify(cestaItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar lista de itens mapeados para a cesta do usuário")
    void listarCesta_comItens_retornaListaMapeada() {
        Account account = buildUsuario();
        Produto produto = buildProduto();
        CestaItem cestaItem = CestaItem.builder().account(account).produto(produto).quantidade(2).build();
        when(cestaItemRepository.findByAccountIdComProdutoESebo(account.getId()))
                .thenReturn(List.of(cestaItem));
        when(precoService.precoEfetivo(produto)).thenReturn(new BigDecimal("29.90"));

        List<CestaItemResponse> resultado = cestaService.listarCesta(account);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().titulo()).isEqualTo("Dom Casmurro");
        assertThat(resultado.getFirst().preco()).isEqualByComparingTo("29.90");
        assertThat(resultado.getFirst().quantidade()).isEqualTo(2);
        assertThat(resultado.getFirst().subtotal()).isEqualByComparingTo("59.80");
    }

    @Test
    @DisplayName("Deve usar o preco promocional da oferta vigente na cesta")
    void listarCesta_comOfertaVigente_usaPrecoPromocional() {
        Account account = buildUsuario();
        Produto produto = buildProduto();
        CestaItem cestaItem = CestaItem.builder().account(account).produto(produto).quantidade(2).build();
        when(cestaItemRepository.findByAccountIdComProdutoESebo(account.getId()))
                .thenReturn(List.of(cestaItem));
        when(precoService.precoEfetivo(produto)).thenReturn(new BigDecimal("12.00"));

        List<CestaItemResponse> resultado = cestaService.listarCesta(account);

        assertThat(resultado.getFirst().preco()).isEqualByComparingTo("12.00");
        assertThat(resultado.getFirst().subtotal()).isEqualByComparingTo("24.00");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao remover item que não está na cesta")
    void removerItem_itemNaoEncontrado_lancaResourceNotFoundException() {
        Account account = buildUsuario();
        when(cestaItemRepository.findByAccountIdAndProdutoId(account.getId(), 5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cestaService.removerItem(account, 5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cestaItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve remover item da cesta e retornar mensagem de sucesso")
    void removerItem_sucesso_deletaItemERetornaMensagem() {
        Account account = buildUsuario();
        Produto produto = buildProduto();
        CestaItem cestaItem = CestaItem.builder().account(account).produto(produto).build();
        when(cestaItemRepository.findByAccountIdAndProdutoId(account.getId(), 1L))
                .thenReturn(Optional.of(cestaItem));

        MensagemResponse response = cestaService.removerItem(account, 1L);

        assertThat(response.mensagem()).isEqualTo("Produto removido da cesta");
        verify(cestaItemRepository).delete(cestaItem);
        verify(recomendacaoEventoService).registrarProduto(account,
                com.vitral.enumerations.TipoEventoRecomendacao.CESTA_REMOVIDO, produto);
    }
}
