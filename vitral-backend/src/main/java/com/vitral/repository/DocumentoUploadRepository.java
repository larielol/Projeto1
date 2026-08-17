package com.vitral.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.vitral.entity.DocumentoUpload;

public interface DocumentoUploadRepository extends JpaRepository<DocumentoUpload, Long> {
    Optional<DocumentoUpload> findByNomeInterno(String nomeInterno);
    boolean existsByNomeInternoAndSeboAccountId(String nomeInterno, Long accountId);
}
