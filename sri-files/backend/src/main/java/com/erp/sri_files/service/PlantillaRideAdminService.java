package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.Empresa;
import com.erp.sri_files.domain.documento.PlantillaRide;
import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.dto.request.PlantillaRideActualizarRequest;
import com.erp.sri_files.dto.request.PlantillaRideCrearRequest;
import com.erp.sri_files.dto.request.PlantillaRideEstadoRequest;
import com.erp.sri_files.dto.response.PlantillaRideResponse;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.dto.response.VerificacionPlantillaRideResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import com.erp.sri_files.repositories.documento.EmpresaRepository;
import com.erp.sri_files.repositories.documento.PlantillaRideRepository;
import com.erp.sri_files.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class PlantillaRideAdminService {

    private final PlantillaRideRepository plantillaRideRepository;
    private final EmpresaRepository empresaRepository;
    private final EmpresaService empresaService;
    private final StorageService storageService;
    private final JasperRideTemplateRenderer jasperRideTemplateRenderer;

    public PlantillaRideAdminService(
            PlantillaRideRepository plantillaRideRepository,
            EmpresaRepository empresaRepository,
            EmpresaService empresaService,
            StorageService storageService,
            JasperRideTemplateRenderer jasperRideTemplateRenderer
    ) {
        this.plantillaRideRepository = plantillaRideRepository;
        this.empresaRepository = empresaRepository;
        this.empresaService = empresaService;
        this.storageService = storageService;
        this.jasperRideTemplateRenderer = jasperRideTemplateRenderer;
    }

    @Transactional(readOnly = true)
    public List<PlantillaRideResponse> listar(UUID empresaId) {
        Empresa empresa = buscarEmpresa(empresaId);
        return plantillaRideRepository.findByEmpresaOrderByTipoDocumentoAscVersionDesc(empresa).stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional
    public PlantillaRideResponse crear(UUID empresaId, PlantillaRideCrearRequest request, MultipartFile file, UsuarioAutenticadoResponse actor) {
        Empresa empresa = buscarEmpresa(empresaId);
        validarDuplicado(empresa, request.tipoDocumento(), request.version(), null);

        PlantillaRide plantilla = new PlantillaRide();
        plantilla.setUuid(UUID.randomUUID());
        plantilla.setEmpresa(empresa);
        aplicarCambios(plantilla, request.tipoDocumento(), request.nombre(), request.version(), request.predeterminada(), request.activa());
        guardarArchivoSiExiste(empresa, plantilla, file);
        normalizarPredeterminadas(empresa, plantilla);
        plantillaRideRepository.save(plantilla);

        empresaService.registrarAuditoriaConfiguracion(empresa, actor, "PLANTILLA_RIDE_CREADA",
                "Plantilla RIDE " + plantilla.getNombre() + " creada para " + plantilla.getTipoDocumento().name());
        return mapear(plantilla);
    }

    @Transactional
    public PlantillaRideResponse actualizar(UUID uuid, PlantillaRideActualizarRequest request, MultipartFile file, UsuarioAutenticadoResponse actor) {
        PlantillaRide plantilla = buscar(uuid);
        validarDuplicado(plantilla.getEmpresa(), request.tipoDocumento(), request.version(), plantilla.getUuid());
        aplicarCambios(plantilla, request.tipoDocumento(), request.nombre(), request.version(), request.predeterminada(), request.activa());
        guardarArchivoSiExiste(plantilla.getEmpresa(), plantilla, file);
        normalizarPredeterminadas(plantilla.getEmpresa(), plantilla);
        plantillaRideRepository.save(plantilla);

        empresaService.registrarAuditoriaConfiguracion(plantilla.getEmpresa(), actor, "PLANTILLA_RIDE_ACTUALIZADA",
                "Plantilla RIDE " + plantilla.getNombre() + " actualizada");
        return mapear(plantilla);
    }

    @Transactional
    public PlantillaRideResponse actualizarEstado(UUID uuid, PlantillaRideEstadoRequest request, UsuarioAutenticadoResponse actor) {
        PlantillaRide plantilla = buscar(uuid);
        plantilla.setActiva(Boolean.TRUE.equals(request.activa()));
        plantillaRideRepository.save(plantilla);
        empresaService.registrarAuditoriaConfiguracion(plantilla.getEmpresa(), actor, "PLANTILLA_RIDE_ESTADO_ACTUALIZADO",
                "Plantilla RIDE " + plantilla.getNombre() + " " + (plantilla.isActiva() ? "activada" : "desactivada"));
        return mapear(plantilla);
    }

    @Transactional(readOnly = true)
    public VerificacionPlantillaRideResponse verificar(UUID uuid) {
        PlantillaRide plantilla = buscar(uuid);
        if (plantilla.getRutaArchivo() == null || plantilla.getRutaArchivo().isBlank()) {
            throw new DocumentoRecepcionException("La plantilla no tiene un archivo JRXML asociado");
        }
        try {
            jasperRideTemplateRenderer.verificar(Files.readAllBytes(Path.of(plantilla.getRutaArchivo())));
            return new VerificacionPlantillaRideResponse(
                    plantilla.getUuid().toString(),
                    true,
                    "Plantilla JRXML valida y compilable",
                    plantilla.getTipoDocumento().name(),
                    plantilla.getNombreArchivo()
            );
        } catch (DocumentoRecepcionException ex) {
            return new VerificacionPlantillaRideResponse(
                    plantilla.getUuid().toString(),
                    false,
                    ex.getMessage(),
                    plantilla.getTipoDocumento().name(),
                    plantilla.getNombreArchivo()
            );
        } catch (Exception ex) {
            return new VerificacionPlantillaRideResponse(
                    plantilla.getUuid().toString(),
                    false,
                    "No fue posible verificar la plantilla: " + ex.getMessage(),
                    plantilla.getTipoDocumento().name(),
                    plantilla.getNombreArchivo()
            );
        }
    }

    private void aplicarCambios(PlantillaRide plantilla, TipoDocumento tipoDocumento, String nombre, String version, boolean predeterminada, boolean activa) {
        plantilla.setTipoDocumento(tipoDocumento);
        plantilla.setNombre(normalizar(nombre));
        plantilla.setVersion(normalizar(version));
        plantilla.setPredeterminada(predeterminada);
        plantilla.setActiva(activa);
    }

    private void guardarArchivoSiExiste(Empresa empresa, PlantillaRide plantilla, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String originalName = file.getOriginalFilename() == null ? "plantilla.jrxml" : file.getOriginalFilename();
        if (!originalName.toLowerCase().endsWith(".jrxml")) {
            throw new DocumentoRecepcionException("La plantilla del RIDE debe estar en formato .jrxml");
        }
        String fileName = file.getOriginalFilename() == null ? "plantilla.jrxml" : file.getOriginalFilename();
        String path = storageService.buildArchivoPath(
                "plantillas-ride",
                empresa.getRuc(),
                empresa.getUuid().toString() + "/" + plantilla.getTipoDocumento().name().toLowerCase(),
                UUID.randomUUID() + "-" + fileName
        );
        try {
            byte[] bytes = file.getBytes();
            jasperRideTemplateRenderer.verificar(bytes);
            storageService.saveBytes(path, bytes);
        } catch (IOException ex) {
            throw new DocumentoRecepcionException("No fue posible guardar el archivo de la plantilla RIDE");
        }
        plantilla.setRutaArchivo(path);
        plantilla.setNombreArchivo(fileName);
    }

    private void normalizarPredeterminadas(Empresa empresa, PlantillaRide actual) {
        if (!actual.isPredeterminada()) {
            return;
        }
        List<PlantillaRide> plantillas = plantillaRideRepository.findByEmpresaAndTipoDocumento(empresa, actual.getTipoDocumento());
        for (PlantillaRide item : plantillas) {
            if (!item.getUuid().equals(actual.getUuid()) && item.isPredeterminada()) {
                item.setPredeterminada(false);
                plantillaRideRepository.save(item);
            }
        }
    }

    private void validarDuplicado(Empresa empresa, TipoDocumento tipoDocumento, String version, UUID actualUuid) {
        List<PlantillaRide> plantillas = plantillaRideRepository.findByEmpresaAndTipoDocumento(empresa, tipoDocumento);
        boolean duplicado = plantillas.stream()
                .anyMatch(item -> item.getVersion().equalsIgnoreCase(version.trim())
                        && (actualUuid == null || !item.getUuid().equals(actualUuid)));
        if (duplicado) {
            throw new DocumentoRecepcionException("Ya existe una plantilla RIDE para el tipo " + tipoDocumento.name() + " con version " + version.trim());
        }
    }

    private Empresa buscarEmpresa(UUID empresaId) {
        return empresaRepository.findByUuid(empresaId)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe empresa con uuid " + empresaId));
    }

    private PlantillaRide buscar(UUID uuid) {
        return plantillaRideRepository.findByUuid(uuid)
                .orElseThrow(() -> new DocumentoRecepcionException("No existe plantilla RIDE con uuid " + uuid));
    }

    private PlantillaRideResponse mapear(PlantillaRide plantilla) {
        return new PlantillaRideResponse(
                plantilla.getUuid().toString(),
                plantilla.getEmpresa().getUuid().toString(),
                plantilla.getTipoDocumento().name(),
                plantilla.getNombre(),
                plantilla.getVersion(),
                plantilla.isPredeterminada(),
                plantilla.isActiva(),
                plantilla.getNombreArchivo(),
                plantilla.getCreatedAt().toString()
        );
    }

    private String normalizar(String value) {
        if (value == null) {
            return null;
        }
        String limpio = value.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
