package com.vitral.mapper;

import org.mapstruct.Mapper;

import com.vitral.dto.CategoriaResponse;
import com.vitral.entity.Categoria;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    CategoriaResponse toResponse(Categoria categoria);
}
