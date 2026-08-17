package com.vitral.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.vitral.entity.DocumentoVerificacaoSebo;

public interface DocumentoVerificacaoSeboRepository extends JpaRepository<DocumentoVerificacaoSebo, Long> {
    List<DocumentoVerificacaoSebo> findBySeboIdOrderByEnviadoEmDesc(Long seboId);
}
