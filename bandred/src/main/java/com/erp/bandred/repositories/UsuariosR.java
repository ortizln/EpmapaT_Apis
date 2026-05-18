package com.erp.bandred.repositories;

import com.erp.bandred.interfaces.UsuarioLogin_int;
import com.erp.bandred.models.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuariosR extends JpaRepository<Usuarios, Long> {
    @Query(value = "select u.idusuario, u.nomusu as username from usuarios u where u.nomusu = ?1 and u.codusu = ?2", nativeQuery = true)
    UsuarioLogin_int getUsuario(String username, String pass);
    @Query(value = "select * from usuarios u where u.nomusu = ?1", nativeQuery = true)
    Usuarios getByUserName(String username);
    @Query(value = "select * from usuarios u where idusuario = ?1", nativeQuery = true)
    Usuarios getByIdusuario(Long idusuario);
}
