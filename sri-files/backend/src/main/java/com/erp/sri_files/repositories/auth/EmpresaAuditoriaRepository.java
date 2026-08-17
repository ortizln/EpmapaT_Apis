package com.erp.sri_files.repositories.auth;

import com.erp.sri_files.domain.auth.EmpresaAuditoria;
import com.erp.sri_files.domain.documento.Empresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaAuditoriaRepository extends JpaRepository<EmpresaAuditoria, Long> {
    List<EmpresaAuditoria> findTop20ByEmpresaOrderByCreatedAtDesc(Empresa empresa);
    Page<EmpresaAuditoria> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
