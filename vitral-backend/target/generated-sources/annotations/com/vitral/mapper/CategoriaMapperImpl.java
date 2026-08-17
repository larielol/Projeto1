package com.vitral.mapper;

import com.vitral.dto.CategoriaResponse;
import com.vitral.entity.Categoria;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-13T08:51:43-0300",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class CategoriaMapperImpl implements CategoriaMapper {

    @Override
    public CategoriaResponse toResponse(Categoria categoria) {
        if ( categoria == null ) {
            return null;
        }

        Long id = null;
        String nome = null;
        String slug = null;
        String descricao = null;

        id = categoria.getId();
        nome = categoria.getNome();
        slug = categoria.getSlug();
        descricao = categoria.getDescricao();

        CategoriaResponse categoriaResponse = new CategoriaResponse( id, nome, slug, descricao );

        return categoriaResponse;
    }
}
