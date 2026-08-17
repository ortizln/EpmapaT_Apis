package com.erp.sri_files.repositories.auth;

import com.erp.sri_files.domain.auth.Rol;
import com.erp.sri_files.domain.auth.RolAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolAuditoriaRepository extends JpaRepository<RolAuditoria, Long> {
    List<RolAuditoria> findTop20ByRolOrderByCreatedAtDesc(Rol rol);
    Page<RolAuditoria> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
