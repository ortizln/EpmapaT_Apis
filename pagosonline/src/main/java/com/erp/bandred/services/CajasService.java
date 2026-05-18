package com.erp.bandred.services;

import com.erp.bandred.interfaces.LastConection_int;
import com.erp.bandred.repositories.CajasR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CajasService {
    @Autowired
    private CajasR c_dao;
    @Autowired
    private UsuarioService s_usuario;
    @Autowired
    private RecaudaxcajaService s_recaudaxcaja;
    public Object cajasSingIn(String username, String password){
        String pass = s_usuario.convertPassword(password);
        LastConection_int l_conecction = c_dao.getLConectionByUserPass(username, pass);
        Map<String, Object> respuesta = new HashMap<>();
        if (l_conecction == null){
            respuesta.put("mensaje", "NOMBRE DE USUARIO INCORRECTO, CAJA NO REGISTRADA");
        } else {
            if (l_conecction.getEstado() == 1) {
                Long usuario = s_usuario.userLogin(username, password);
                if (usuario != null){
                    respuesta.put("mensaje", "exito");
                } else {
                    respuesta.put("mensaje", "no hay exito");
                }
                respuesta.put("mensaje", "LA CAJA ESTA ABIERTA");
                respuesta.put("Link: ", "/cajas/singout?username=" + username);
                respuesta.put("usuario", s_usuario.codificar(l_conecction.getUsuario().toString()));
                respuesta.put("n", l_conecction.getUsuario());
            }
            if (l_conecction.getEstado() == 0) {
                try {
                    Long usuario = s_usuario.userLogin(username, password);
                    s_recaudaxcaja.abrirCaja(l_conecction.getIdrecaudaxcaja(), usuario);
                    respuesta.put("mensaje", "Acceso aprobado");
                    respuesta.put("autentification", s_usuario.codificar(usuario.toString()));
                    respuesta.put("autentification", usuario);
                } catch (Exception e) {
                    System.out.println("Error al iniciar caja " + e);
                    respuesta.put("mensaje", "Acceso denegado " + e);
                    respuesta.put("autentification", false);
                }
            }
        }
        return respuesta;
    }

    public Object cajasSingOut(String username){
        LastConection_int l_conecction = c_dao.getLConectionByUsername(username);
        Map<String, Object> respuesta = new HashMap<>();
        if(l_conecction.getEstado() == 1){
            s_recaudaxcaja.cerrarCaja(l_conecction.getIdrecaudaxcaja());
            respuesta.put("mensaje", "Cerrando la ultima conexión");
        }
        return respuesta;
    }

    public LastConection_int getLastConectionByUduario(Long idusuario){
        return c_dao.getLastConection(idusuario);
    }

    public Object testIfLogin(Long idusuario){
        LastConection_int lconection = c_dao.getLastConection(idusuario);
        Map<String, Object> respuesta = new HashMap<>();

        if(lconection != null && lconection.getEstado() == 1){
            respuesta.put("estado", lconection.getEstado());
            respuesta.put("username", lconection.getNomusu());

            // ✅ Usamos directamente los métodos estáticos de FacturasService
            respuesta.put("codigo", FacturasService.fPemiCaja(lconection.getCodigo()));
            respuesta.put("establecimiento", FacturasService.fPemiCaja(lconection.getEstablecimiento()));
            respuesta.put("secuencial", FacturasService.fSecuencial(lconection.getSecuencial()));

            respuesta.put("iniciolabor", lconection.getFechainiciolabor());
            respuesta.put("finlabor", lconection.getFechafinlabor());
        } else {
            respuesta.put("estado", 0);
            respuesta.put("mensaje", "Caja no iniciada");
        }
        return respuesta;
    }
}
