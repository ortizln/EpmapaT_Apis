package com.erp.epmapaApi.repositories;

import com.erp.epmapaApi.models.Clientes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientesR extends JpaRepository<Clientes, Long> {
    Optional<Clientes> findByUsernameAndActivoTrue(String username);
}
