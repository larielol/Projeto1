package com.vitral.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categoria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Categoria extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(length = 500)
    private String descricao;
}
