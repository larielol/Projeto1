package com.vitral.entity;

import com.vitral.enumerations.SuporteStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "suporte")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Suporte extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String assunto;

    @Column(nullable = false, length = 2000)
    private String mensagem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Account remetente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SuporteStatus status = SuporteStatus.ABERTO;
}
