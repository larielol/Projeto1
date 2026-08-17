package com.vitral.specification;

import org.springframework.data.jpa.domain.Specification;

import com.vitral.entity.Sebo;

import jakarta.persistence.criteria.JoinType;

public class SeboSpecification {

    private SeboSpecification() {
    }

    public static Specification<Sebo> ativo() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.join("account", JoinType.INNER).get("ativo")),
                cb.equal(root.get("statusVerificacao"),
                        com.vitral.enumerations.StatusVerificacaoSebo.VERIFICADO));
    }

    public static Specification<Sebo> nomeContem(String termo) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.join("account", JoinType.INNER).get("name")),
                "%" + termo.toLowerCase() + "%");
    }

    public static Specification<Sebo> cidadeContem(String cidade) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("cidade")),
                "%" + cidade.trim().toLowerCase() + "%");
    }

    public static Specification<Sebo> ufIgual(String uf) {
        return (root, query, cb) -> cb.equal(root.get("uf"), uf.trim().toUpperCase());
    }

}
