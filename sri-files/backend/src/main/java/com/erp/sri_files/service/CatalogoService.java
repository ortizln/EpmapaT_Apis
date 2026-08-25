package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.DocumentoEstado;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.dto.response.CatalogoItemResponse;
import com.erp.sri_files.repositories.Tabla15R;
import com.erp.sri_files.repositories.auth.PermisoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogoService {

    private final Tabla15R tabla15R;
    private final PermisoRepository permisoRepository;

    public CatalogoService(Tabla15R tabla15R, PermisoRepository permisoRepository) {
        this.tabla15R = tabla15R;
        this.permisoRepository = permisoRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> tiposDocumento() {
        return List.of(TipoDocumento.values()).stream()
                .map(tipo -> new CatalogoItemResponse(tipo.name(), tipo.name(), "TIPO_DOCUMENTO"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> estadosDocumento() {
        return List.of(DocumentoEstado.values()).stream()
                .map(estado -> new CatalogoItemResponse(estado.name(), estado.name(), "ESTADO_DOCUMENTO"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> tiposIdentificacion() {
        return List.of(
                new CatalogoItemResponse("04", "RUC", "TIPO_IDENTIFICACION"),
                new CatalogoItemResponse("05", "CEDULA", "TIPO_IDENTIFICACION"),
                new CatalogoItemResponse("06", "PASAPORTE", "TIPO_IDENTIFICACION"),
                new CatalogoItemResponse("07", "CONSUMIDOR_FINAL", "TIPO_IDENTIFICACION"),
                new CatalogoItemResponse("08", "IDENTIFICACION_EXTERIOR", "TIPO_IDENTIFICACION")
        );
    }

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> formasPago() {
        return tabla15R.findAll().stream()
                .map(item -> new CatalogoItemResponse(item.getCodtabla15(), item.getNomtabla15(), "FORMA_PAGO"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> impuestos() {
        return List.of(
                new CatalogoItemResponse("2", "IVA", "IMPUESTO"),
                new CatalogoItemResponse("3", "ICE", "IMPUESTO"),
                new CatalogoItemResponse("5", "IRBPNR", "IMPUESTO")
        );
    }

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> codigosRetencion() {
        return permisoRepository.findAllByOrderByCategoriaAscCodigoAsc().stream()
                .filter(item -> item.getCategoria() != null && item.getCategoria().toUpperCase().contains("RET"))
                .map(item -> new CatalogoItemResponse(item.getCodigo(), item.getNombre(), "CODIGO_RETENCION"))
                .toList();
    }
}
