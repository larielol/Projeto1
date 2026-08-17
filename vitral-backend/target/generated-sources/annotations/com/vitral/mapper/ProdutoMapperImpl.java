package com.vitral.mapper;

import com.vitral.dto.ProdutoResponse;
import com.vitral.entity.Account;
import com.vitral.entity.Categoria;
import com.vitral.entity.Produto;
import com.vitral.entity.Sebo;
import com.vitral.enumerations.BookGenre;
import com.vitral.enumerations.CondicaoProduto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-13T08:51:42-0300",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class ProdutoMapperImpl implements ProdutoMapper {

    @Override
    public ProdutoResponse toResponse(Produto produto, BigDecimal precoPromocional) {
        if ( produto == null && precoPromocional == null ) {
            return null;
        }

        Long seboId = null;
        String seboNome = null;
        Long categoriaId = null;
        String categoriaNome = null;
        OffsetDateTime dataPublicacao = null;
        Long id = null;
        BookGenre bookGenre = null;
        String titulo = null;
        String autor = null;
        String descricao = null;
        Integer ano = null;
        BigDecimal preco = null;
        Integer estoque = null;
        CondicaoProduto condicao = null;
        String fotoUrl = null;
        Boolean ativo = null;
        Boolean classico = null;
        Boolean lancamento = null;
        if ( produto != null ) {
            seboId = produtoSeboId( produto );
            seboNome = produtoSeboAccountName( produto );
            categoriaId = produtoCategoriaId( produto );
            categoriaNome = produtoCategoriaNome( produto );
            dataPublicacao = produto.getCreatedAt();
            id = produto.getId();
            bookGenre = produto.getBookGenre();
            titulo = produto.getTitulo();
            autor = produto.getAutor();
            descricao = produto.getDescricao();
            ano = produto.getAno();
            preco = produto.getPreco();
            estoque = produto.getEstoque();
            condicao = produto.getCondicao();
            fotoUrl = produto.getFotoUrl();
            ativo = produto.getAtivo();
            classico = produto.getClassico();
            lancamento = produto.getLancamento();
        }
        BigDecimal precoPromocional1 = null;
        precoPromocional1 = precoPromocional;

        Boolean disponivel = Boolean.TRUE.equals(produto.getAtivo()) && produto.getEstoque() != null && produto.getEstoque() > 0;

        ProdutoResponse produtoResponse = new ProdutoResponse( id, seboId, seboNome, categoriaId, categoriaNome, bookGenre, titulo, autor, descricao, ano, preco, precoPromocional1, estoque, condicao, fotoUrl, ativo, disponivel, classico, lancamento, dataPublicacao );

        return produtoResponse;
    }

    private Long produtoSeboId(Produto produto) {
        Sebo sebo = produto.getSebo();
        if ( sebo == null ) {
            return null;
        }
        return sebo.getId();
    }

    private String produtoSeboAccountName(Produto produto) {
        Sebo sebo = produto.getSebo();
        if ( sebo == null ) {
            return null;
        }
        Account account = sebo.getAccount();
        if ( account == null ) {
            return null;
        }
        return account.getName();
    }

    private Long produtoCategoriaId(Produto produto) {
        Categoria categoria = produto.getCategoria();
        if ( categoria == null ) {
            return null;
        }
        return categoria.getId();
    }

    private String produtoCategoriaNome(Produto produto) {
        Categoria categoria = produto.getCategoria();
        if ( categoria == null ) {
            return null;
        }
        return categoria.getNome();
    }
}
