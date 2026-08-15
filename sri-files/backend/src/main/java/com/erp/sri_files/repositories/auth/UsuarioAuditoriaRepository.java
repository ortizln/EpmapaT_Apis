package com.erp.sri_files.repositories.auth;

import com.erp.sri_files.domain.auth.UsuarioAuditoria;
import com.erp.sri_files.domain.auth.UsuarioSistema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioAuditoriaRepository extends JpaRepository<UsuarioAuditoria, Long> {
    List<UsuarioAuditoria> findTop20ByUsuarioOrderByCreatedAtDesc(UsuarioSistema usuario);
}
