package com.vitral.entity;

import com.vitral.enumerations.AccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(length = 11)
    private String cpf;

    @Column(length = 8)
    private String cep;

    @Column(length = 255)
    private String logradouro;

    @Column(length = 30)
    private String numero;

    @Column(length = 255)
    private String complemento;

    @Column(length = 120)
    private String bairro;

    @Column(length = 120)
    private String cidade;

    @Column(length = 2)
    private String estado;

    @Builder.Default
    @Column(name = "auth_version", nullable = false, columnDefinition = "integer default 0")
    private Integer authVersion = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativo = Boolean.TRUE;
}
