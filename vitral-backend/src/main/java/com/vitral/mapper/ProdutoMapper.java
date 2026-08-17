package com.vitral.mapper;

import java.math.BigDecimal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.vitral.dto.ProdutoResponse;
import com.vitral.entity.Produto;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    @Mapping(target = "seboId", source = "produto.sebo.id")
    @Mapping(target = "seboNome", source = "produto.sebo.account.name")
    @Mapping(target = "categoriaId", source = "produto.categoria.id")
    @Mapping(target = "categoriaNome", source = "produto.categoria.nome")
    @Mapping(target = "precoPromocional", source = "precoPromocional")
    @Mapping(target = "dataPublicacao", source = "produto.createdAt")
    @Mapping(target = "disponivel", expression = "java(Boolean.TRUE.equals(produto.getAtivo()) && produto.getEstoque() != null && produto.getEstoque() > 0)")
    ProdutoResponse toResponse(Produto produto, BigDecimal precoPromocional);
}
