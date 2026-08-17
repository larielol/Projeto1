package com.vitral.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mensagem")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mensagem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Account remetente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Account destinatario;

    @Column(nullable = false, length = 2000)
    private String conteudo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean lida = Boolean.FALSE;
}
