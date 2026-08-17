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

@Entity
@Table(name = "documento_upload")
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentoUpload extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sebo_id", nullable = false)
    private Sebo sebo;
    @Column(name = "nome_interno", nullable = false, unique = true, length = 80)
    private String nomeInterno;
    @Column(name = "nome_original", nullable = false, length = 255)
    private String nomeOriginal;
    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;
    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;
}
