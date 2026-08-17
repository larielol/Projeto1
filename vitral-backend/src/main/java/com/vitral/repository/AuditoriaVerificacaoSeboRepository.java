package com.vitral.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.vitral.entity.AuditoriaVerificacaoSebo;

public interface AuditoriaVerificacaoSeboRepository extends JpaRepository<AuditoriaVerificacaoSebo, Long> {
    List<AuditoriaVerificacaoSebo> findBySeboIdOrderByCreatedAtDesc(Long seboId);
}
