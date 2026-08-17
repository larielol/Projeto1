package com.vitral.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.vitral.dto.FavoritoResponse;
import com.vitral.dto.MensagemResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Favorito;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.AccountType;
import com.vitral.enumerations.CondicaoProduto;
import com.vitral.exception.ConflictException;
import com.vitral.exception.ResourceNotFoundException;
import com.vitral.repository.FavoritoRepository;
import com.vitral.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock RecomendacaoEventoService recomendacaoEventoService;

    @Mock
    private FavoritoRepository favoritoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private PrecoService precoService;

    @InjectMocks
    private FavoritoService favoritoService;

    private Account buildUsuario() {
        return Account.builder()
                .name("Ana")
                .email("ana@email.com")
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
                .titulo("Grande Sertão: Veredas")
                .autor("Guimarães Rosa")
                .preco(new BigDecimal("45.00"))
                .condicao(CondicaoProduto.SEMINOVO)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("Deve lançar ConflictException quando produto já está nos favoritos")
    void favoritar_produtoJaFavoritado_lancaConflictException() {
        Account account = buildUsuario();
        when(favoritoRepository.existsByAccountIdAndProdutoId(account.getId(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> favoritoService.favoritar(account, 1L))
                .isInstanceOf(ConflictException.class);

        verify(produtoRepository, never()).findByIdAndAtivoTrue(any());
        verify(favoritoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando produto não existe ou está inativo")
    void favoritar_produtoNaoEncontrado_lancaResourceNotFoundException() {
        Account account = buildUsuario();
        when(favoritoRepository.existsByAccountIdAndProdutoId(account.getId(), 99L)).thenReturn(false);
        when(produtoRepository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritoService.favoritar(account, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(favoritoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar favorito e retornar mensagem de sucesso")
    void favoritar_sucesso_salvaFavoritoERetornaMensagem() {
        Account account = buildUsuario();
        Produto produto = buildProduto();
        when(favoritoRepository.existsByAccountIdAndProdutoId(account.getId(), 1L)).thenReturn(false);
        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));

        MensagemResponse response = favoritoService.favoritar(account, 1L);

        assertThat(response.mensagem()).isEqualTo("Produto favoritado com sucesso");
        verify(favoritoRepository).save(any(Favorito.class));
        verify(recomendacaoEventoService).registrarProduto(account,
                com.vitral.enumerations.TipoEventoRecomendacao.FAVORITO_ADICIONADO, produto);
    }

    @Test
    @DisplayName("Deve retornar lista de favoritos mapeados do usuário")
    void listarFavoritos_comFavoritos_retornaListaMapeada() {
        Account account = buildUsuario();
        Produto produto = buildProduto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        Favorito favorito = Favorito.builder().account(account).produto(produto).build();
        when(favoritoRepository.findByAccountId(account.getId())).thenReturn(List.of(favorito));
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of());

        List<FavoritoResponse> resultado = favoritoService.listarFavoritos(account);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().titulo()).isEqualTo("Grande Sertão: Veredas");
        assertThat(resultado.getFirst().preco()).isEqualByComparingTo("45.00");
        assertThat(resultado.getFirst().precoPromocional()).isNull();
    }

    @Test
    @DisplayName("Deve usar o preco promocional da oferta vigente no favorito")
    void listarFavoritos_comOfertaVigente_usaPrecoPromocional() {
        Account account = buildUsuario();
        Produto produto = buildProduto();
        ReflectionTestUtils.setField(produto, "id", 1L);
        Favorito favorito = Favorito.builder().account(account).produto(produto).build();
        when(favoritoRepository.findByAccountId(account.getId())).thenReturn(List.of(favorito));
        when(precoService.precosPromocionaisVigentes(any())).thenReturn(Map.of(1L, new BigDecimal("30.00")));

        List<FavoritoResponse> resultado = favoritoService.listarFavoritos(account);

        assertThat(resultado.getFirst().preco()).isEqualByComparingTo("45.00");
        assertThat(resultado.getFirst().precoPromocional()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao remover favorito inexistente")
    void removerFavorito_naoEncontrado_lancaResourceNotFoundException() {
        Account account = buildUsuario();
        when(favoritoRepository.findByAccountIdAndProdutoId(account.getId(), 5L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritoService.removerFavorito(account, 5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(favoritoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve remover favorito e retornar mensagem de sucesso")
    void removerFavorito_sucesso_deletaFavoritoERetornaMensagem() {
        Account account = buildUsuario();
        Produto produto = buildProduto();
        Favorito favorito = Favorito.builder().account(account).produto(produto).build();
        when(favoritoRepository.findByAccountIdAndProdutoId(account.getId(), 1L))
                .thenReturn(Optional.of(favorito));

        MensagemResponse response = favoritoService.removerFavorito(account, 1L);

        assertThat(response.mensagem()).isEqualTo("Favorito removido com sucesso");
        verify(favoritoRepository).delete(favorito);
        verify(recomendacaoEventoService).registrarProduto(account,
                com.vitral.enumerations.TipoEventoRecomendacao.FAVORITO_REMOVIDO, produto);
    }
}
