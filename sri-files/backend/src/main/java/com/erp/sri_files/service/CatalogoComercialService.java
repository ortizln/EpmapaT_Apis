package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.ClienteCatalogo;
import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.FormaPagoCatalogo;
import com.erp.sri_files.domain.documento.IvaTarifaCatalogo;
import com.erp.sri_files.domain.documento.ProductoCatalogo;
import com.erp.sri_files.dto.request.CatalogoActivoRequest;
import com.erp.sri_files.dto.request.ClienteCatalogoRequest;
import com.erp.sri_files.dto.request.FormaPagoCatalogoRequest;
import com.erp.sri_files.dto.request.IvaTarifaCatalogoRequest;
import com.erp.sri_files.dto.request.ProductoCatalogoRequest;
import com.erp.sri_files.dto.response.ClienteCatalogoResponse;
import com.erp.sri_files.dto.response.FormaPagoCatalogoResponse;
import com.erp.sri_files.dto.response.IvaTarifaCatalogoResponse;
import com.erp.sri_files.dto.response.ProductoCatalogoResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.ClienteCatalogoRepository;
import com.erp.sri_files.repositories.documento.EmpresaRepository;
import com.erp.sri_files.repositories.documento.FormaPagoCatalogoRepository;
import com.erp.sri_files.repositories.documento.IvaTarifaCatalogoRepository;
import com.erp.sri_files.repositories.documento.ProductoCatalogoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CatalogoComercialService {

    private final EmpresaRepository empresaRepository;
    private final ClienteCatalogoRepository clienteRepository;
    private final ProductoCatalogoRepository productoRepository;
    private final FormaPagoCatalogoRepository formaPagoRepository;
    private final IvaTarifaCatalogoRepository ivaTarifaRepository;

    public CatalogoComercialService(
            EmpresaRepository empresaRepository,
            ClienteCatalogoRepository clienteRepository,
            ProductoCatalogoRepository productoRepository,
            FormaPagoCatalogoRepository formaPagoRepository,
            IvaTarifaCatalogoRepository ivaTarifaRepository
    ) {
        this.empresaRepository = empresaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.formaPagoRepository = formaPagoRepository;
        this.ivaTarifaRepository = ivaTarifaRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteCatalogoResponse> listarClientes(UUID empresaUuid) {
        return clienteRepository.findByEmpresa_UuidOrderByRazonSocialAsc(empresaUuid).stream().map(this::mapearCliente).toList();
    }

    @Transactional(readOnly = true)
    public ClienteCatalogoResponse obtenerCliente(UUID uuid) {
        return mapearCliente(buscarCliente(uuid));
    }

    @Transactional
    public ClienteCatalogoResponse crearCliente(ClienteCatalogoRequest request) {
        UUID empresaUuid = UUID.fromString(request.empresaId().trim());
        String identificacion = request.identificacion().trim();
        clienteRepository.findByEmpresa_UuidAndIdentificacion(empresaUuid, identificacion)
                .ifPresent(item -> { throw new DocumentoRecepcionException("Ya existe un cliente/beneficiario con la identificacion " + identificacion); });

        ClienteCatalogo entity = new ClienteCatalogo();
        entity.setUuid(UUID.randomUUID());
        entity.setEmpresa(buscarEmpresa(empresaUuid));
        aplicarCliente(entity, request);
        entity.setActivo(true);
        return mapearCliente(clienteRepository.save(entity));
    }

    @Transactional
    public ClienteCatalogoResponse actualizarCliente(UUID uuid, ClienteCatalogoRequest request) {
        ClienteCatalogo entity = buscarCliente(uuid);
        String identificacion = request.identificacion().trim();
        clienteRepository.findByEmpresa_UuidAndIdentificacion(entity.getEmpresa().getUuid(), identificacion)
                .filter(item -> !item.getUuid().equals(uuid))
                .ifPresent(item -> { throw new DocumentoRecepcionException("Ya existe otro cliente/beneficiario con la identificacion " + identificacion); });
        aplicarCliente(entity, request);
        return mapearCliente(clienteRepository.save(entity));
    }

    @Transactional
    public ClienteCatalogoResponse actualizarEstadoCliente(UUID uuid, CatalogoActivoRequest request) {
        ClienteCatalogo entity = buscarCliente(uuid);
        entity.setActivo(request.activo());
        return mapearCliente(clienteRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ProductoCatalogoResponse> listarProductos(UUID empresaUuid) {
        return productoRepository.findByEmpresa_UuidOrderByNombreAsc(empresaUuid).stream().map(this::mapearProducto).toList();
    }

    @Transactional(readOnly = true)
    public ProductoCatalogoResponse obtenerProducto(UUID uuid) {
        return mapearProducto(buscarProducto(uuid));
    }

    @Transactional
    public ProductoCatalogoResponse crearProducto(ProductoCatalogoRequest request) {
        UUID empresaUuid = UUID.fromString(request.empresaId().trim());
        String codigo = request.codigo().trim();
        productoRepository.findByEmpresa_UuidAndCodigo(empresaUuid, codigo)
                .ifPresent(item -> { throw new DocumentoRecepcionException("Ya existe un producto con el codigo " + codigo); });

        ProductoCatalogo entity = new ProductoCatalogo();
        entity.setUuid(UUID.randomUUID());
        entity.setEmpresa(buscarEmpresa(empresaUuid));
        aplicarProducto(entity, request);
        entity.setActivo(true);
        return mapearProducto(productoRepository.save(entity));
    }

    @Transactional
    public ProductoCatalogoResponse actualizarProducto(UUID uuid, ProductoCatalogoRequest request) {
        ProductoCatalogo entity = buscarProducto(uuid);
        String codigo = request.codigo().trim();
        productoRepository.findByEmpresa_UuidAndCodigo(entity.getEmpresa().getUuid(), codigo)
                .filter(item -> !item.getUuid().equals(uuid))
                .ifPresent(item -> { throw new DocumentoRecepcionException("Ya existe otro producto con el codigo " + codigo); });
        aplicarProducto(entity, request);
        return mapearProducto(productoRepository.save(entity));
    }

    @Transactional
    public ProductoCatalogoResponse actualizarEstadoProducto(UUID uuid, CatalogoActivoRequest request) {
        ProductoCatalogo entity = buscarProducto(uuid);
        entity.setActivo(request.activo());
        return mapearProducto(productoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<FormaPagoCatalogoResponse> listarFormasPago(UUID empresaUuid) {
        return formaPagoRepository.findByEmpresa_UuidOrderByNombreAsc(empresaUuid).stream().map(this::mapearFormaPago).toList();
    }

    @Transactional(readOnly = true)
    public FormaPagoCatalogoResponse obtenerFormaPago(UUID uuid) {
        return mapearFormaPago(buscarFormaPago(uuid));
    }

    @Transactional
    public FormaPagoCatalogoResponse crearFormaPago(FormaPagoCatalogoRequest request) {
        UUID empresaUuid = UUID.fromString(request.empresaId().trim());
        String codigo = request.codigo().trim();
        formaPagoRepository.findByEmpresa_UuidAndCodigo(empresaUuid, codigo)
                .ifPresent(item -> { throw new DocumentoRecepcionException("Ya existe una forma de pago con el codigo " + codigo); });

        FormaPagoCatalogo entity = new FormaPagoCatalogo();
        entity.setUuid(UUID.randomUUID());
        entity.setEmpresa(buscarEmpresa(empresaUuid));
        aplicarFormaPago(entity, request);
        entity.setActivo(true);
        return mapearFormaPago(formaPagoRepository.save(entity));
    }

    @Transactional
    public FormaPagoCatalogoResponse actualizarFormaPago(UUID uuid, FormaPagoCatalogoRequest request) {
        FormaPagoCatalogo entity = buscarFormaPago(uuid);
        String codigo = request.codigo().trim();
        formaPagoRepository.findByEmpresa_UuidAndCodigo(entity.getEmpresa().getUuid(), codigo)
                .filter(item -> !item.getUuid().equals(uuid))
                .ifPresent(item -> { throw new DocumentoRecepcionException("Ya existe otra forma de pago con el codigo " + codigo); });
        aplicarFormaPago(entity, request);
        return mapearFormaPago(formaPagoRepository.save(entity));
    }

    @Transactional
    public FormaPagoCatalogoResponse actualizarEstadoFormaPago(UUID uuid, CatalogoActivoRequest request) {
        FormaPagoCatalogo entity = buscarFormaPago(uuid);
        entity.setActivo(request.activo());
        return mapearFormaPago(formaPagoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<IvaTarifaCatalogoResponse> listarIvaTarifas(UUID empresaUuid) {
        return ivaTarifaRepository.findByEmpresa_UuidOrderByPorcentajeAscNombreAsc(empresaUuid).stream().map(this::mapearIvaTarifa).toList();
    }

    @Transactional(readOnly = true)
    public IvaTarifaCatalogoResponse obtenerIvaTarifa(UUID uuid) {
        return mapearIvaTarifa(buscarIvaTarifa(uuid));
    }

    @Transactional
    public IvaTarifaCatalogoResponse crearIvaTarifa(IvaTarifaCatalogoRequest request) {
        UUID empresaUuid = UUID.fromString(request.empresaId().trim());
        String codigo = request.codigo().trim();
        ivaTarifaRepository.findByEmpresa_UuidAndCodigo(empresaUuid, codigo)
                .ifPresent(item -> { throw new DocumentoRecepcionException("Ya existe una tarifa IVA con el codigo " + codigo); });

        IvaTarifaCatalogo entity = new IvaTarifaCatalogo();
        entity.setUuid(UUID.randomUUID());
        entity.setEmpresa(buscarEmpresa(empresaUuid));
        aplicarIvaTarifa(entity, request);
        entity.setActivo(true);
        return mapearIvaTarifa(ivaTarifaRepository.save(entity));
    }

    @Transactional
    public IvaTarifaCatalogoResponse actualizarIvaTarifa(UUID uuid, IvaTarifaCatalogoRequest request) {
        IvaTarifaCatalogo entity = buscarIvaTarifa(uuid);
        String codigo = request.codigo().trim();
        ivaTarifaRepository.findByEmpresa_UuidAndCodigo(entity.getEmpresa().getUuid(), codigo)
                .filter(item -> !item.getUuid().equals(uuid))
                .ifPresent(item -> { throw new DocumentoRecepcionException("Ya existe otra tarifa IVA con el codigo " + codigo); });
        aplicarIvaTarifa(entity, request);
        return mapearIvaTarifa(ivaTarifaRepository.save(entity));
    }

    @Transactional
    public IvaTarifaCatalogoResponse actualizarEstadoIvaTarifa(UUID uuid, CatalogoActivoRequest request) {
        IvaTarifaCatalogo entity = buscarIvaTarifa(uuid);
        entity.setActivo(request.activo());
        return mapearIvaTarifa(ivaTarifaRepository.save(entity));
    }

    private void aplicarCliente(ClienteCatalogo entity, ClienteCatalogoRequest request) {
        entity.setTipoIdentificacion(request.tipoIdentificacion().trim());
        entity.setIdentificacion(request.identificacion().trim());
        entity.setRazonSocial(request.razonSocial().trim());
        entity.setNombreComercial(normalizar(request.nombreComercial()));
        entity.setEmail(normalizar(request.email()));
        entity.setTelefono(normalizar(request.telefono()));
        entity.setDireccion(normalizar(request.direccion()));
        entity.setObservacion(normalizar(request.observacion()));
    }

    private void aplicarProducto(ProductoCatalogo entity, ProductoCatalogoRequest request) {
        entity.setCodigo(request.codigo().trim());
        entity.setNombre(request.nombre().trim());
        entity.setDescripcion(normalizar(request.descripcion()));
        entity.setUnidadMedida(normalizar(request.unidadMedida()));
        entity.setPrecioBase(orZero(request.precioBase()));
        entity.setPorcentajeIva(orZero(request.porcentajeIva()));
    }

    private void aplicarFormaPago(FormaPagoCatalogo entity, FormaPagoCatalogoRequest request) {
        entity.setCodigo(request.codigo().trim());
        entity.setNombre(request.nombre().trim());
        entity.setDescripcion(normalizar(request.descripcion()));
        entity.setDiasPlazo(request.diasPlazo());
    }

    private void aplicarIvaTarifa(IvaTarifaCatalogo entity, IvaTarifaCatalogoRequest request) {
        entity.setCodigo(request.codigo().trim());
        entity.setNombre(request.nombre().trim());
        entity.setPorcentaje(orZero(request.porcentaje()));
        entity.setCodigoSri(normalizar(request.codigoSri()));
        entity.setDescripcion(normalizar(request.descripcion()));
    }

    private ClienteCatalogo buscarCliente(UUID uuid) {
        return clienteRepository.findByUuid(uuid).orElseThrow(() -> new DocumentoRecepcionException("No existe cliente/beneficiario con uuid " + uuid));
    }

    private ProductoCatalogo buscarProducto(UUID uuid) {
        return productoRepository.findByUuid(uuid).orElseThrow(() -> new DocumentoRecepcionException("No existe producto con uuid " + uuid));
    }

    private FormaPagoCatalogo buscarFormaPago(UUID uuid) {
        return formaPagoRepository.findByUuid(uuid).orElseThrow(() -> new DocumentoRecepcionException("No existe forma de pago con uuid " + uuid));
    }

    private IvaTarifaCatalogo buscarIvaTarifa(UUID uuid) {
        return ivaTarifaRepository.findByUuid(uuid).orElseThrow(() -> new DocumentoRecepcionException("No existe tarifa IVA con uuid " + uuid));
    }

    private Empresa buscarEmpresa(UUID uuid) {
        return empresaRepository.findByUuid(uuid).orElseThrow(() -> new DocumentoRecepcionException("No existe empresa con uuid " + uuid));
    }

    private String normalizar(String value) {
        if (value == null) {
            return null;
        }
        String limpio = value.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private ClienteCatalogoResponse mapearCliente(ClienteCatalogo entity) {
        return new ClienteCatalogoResponse(entity.getUuid().toString(), entity.getEmpresa().getUuid().toString(), entity.getTipoIdentificacion(), entity.getIdentificacion(), entity.getRazonSocial(), entity.getNombreComercial(), entity.getEmail(), entity.getTelefono(), entity.getDireccion(), entity.getObservacion(), entity.isActivo());
    }

    private ProductoCatalogoResponse mapearProducto(ProductoCatalogo entity) {
        return new ProductoCatalogoResponse(entity.getUuid().toString(), entity.getEmpresa().getUuid().toString(), entity.getCodigo(), entity.getNombre(), entity.getDescripcion(), entity.getUnidadMedida(), entity.getPrecioBase(), entity.getPorcentajeIva(), entity.isActivo());
    }

    private FormaPagoCatalogoResponse mapearFormaPago(FormaPagoCatalogo entity) {
        return new FormaPagoCatalogoResponse(entity.getUuid().toString(), entity.getEmpresa().getUuid().toString(), entity.getCodigo(), entity.getNombre(), entity.getDescripcion(), entity.getDiasPlazo(), entity.isActivo());
    }

    private IvaTarifaCatalogoResponse mapearIvaTarifa(IvaTarifaCatalogo entity) {
        return new IvaTarifaCatalogoResponse(entity.getUuid().toString(), entity.getEmpresa().getUuid().toString(), entity.getCodigo(), entity.getNombre(), entity.getPorcentaje(), entity.getCodigoSri(), entity.getDescripcion(), entity.isActivo());
    }
}
