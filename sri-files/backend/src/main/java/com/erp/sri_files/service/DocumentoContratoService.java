package com.erp.sri_files.service;

import com.erp.sri_files.domain.documento.TipoDocumento;
import com.erp.sri_files.dto.response.DocumentoCampoContratoResponse;
import com.erp.sri_files.dto.response.DocumentoContratoResponse;
import com.erp.sri_files.dto.response.DocumentoSeccionContratoResponse;
import com.erp.sri_files.exceptions.DocumentoRecepcionException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentoContratoService {

    public DocumentoContratoResponse obtener(String tipoDocumento) {
        TipoDocumento tipo = parseTipoDocumento(tipoDocumento);
        return new DocumentoContratoResponse(
                tipo.name(),
                "/api/v1/documentos",
                "POST",
                switch (tipo) {
                    case FACTURA -> contratoFactura();
                    case LIQUIDACION_COMPRA -> contratoLiquidacionCompra();
                    case NOTA_CREDITO -> contratoNotaCredito();
                    case NOTA_DEBITO -> contratoNotaDebito();
                    case RETENCION -> contratoRetencion();
                    case GUIA_REMISION -> contratoGuiaRemision();
                }
        );
    }

    private TipoDocumento parseTipoDocumento(String tipoDocumento) {
        try {
            return TipoDocumento.valueOf(tipoDocumento.trim().toUpperCase());
        } catch (Exception ex) {
            throw new DocumentoRecepcionException("Tipo de documento no soportado para contrato: " + tipoDocumento);
        }
    }

    private List<DocumentoSeccionContratoResponse> contratoFactura() {
        return List.of(
                seccionEmisor(),
                seccionReceptor(),
                seccionDocumentoBase(List.of(
                        campo("fechaEmision", "date", true, "Fecha ISO del comprobante", "2026-08-14"),
                        campo("secuencial", "string", true, "Secuencial de 9 digitos", "000000123"),
                        campo("moneda", "string", false, "Moneda del documento", "USD"),
                        campo("subtotal", "number", true, "Subtotal sin impuestos", "10.00"),
                        campo("impuestos", "number", true, "Total impuestos", "1.20"),
                        campo("total", "number", true, "Total del comprobante", "11.20")
                )),
                seccionDetallesFactura()
        );
    }

    private List<DocumentoSeccionContratoResponse> contratoLiquidacionCompra() {
        return List.of(
                seccionEmisor(),
                seccionReceptor(),
                seccionDocumentoBase(List.of(
                        campo("fechaEmision", "date", true, "Fecha ISO del comprobante", "2026-08-14"),
                        campo("secuencial", "string", true, "Secuencial de 9 digitos", "000000128"),
                        campo("moneda", "string", false, "Moneda del documento", "USD"),
                        campo("subtotal", "number", true, "Subtotal sin impuestos", "100.00"),
                        campo("impuestos", "number", true, "Total impuestos", "12.00"),
                        campo("total", "number", true, "Total del comprobante", "112.00")
                )),
                seccionDetallesFactura()
        );
    }

    private List<DocumentoSeccionContratoResponse> contratoNotaCredito() {
        return List.of(
                seccionEmisor(),
                seccionReceptor(),
                seccionDocumentoBase(List.of(
                        campo("fechaEmision", "date", true, "Fecha ISO del comprobante", "2026-08-14"),
                        campo("secuencial", "string", true, "Secuencial de 9 digitos", "000000124"),
                        campo("moneda", "string", false, "Moneda del documento", "USD"),
                        campo("subtotal", "number", true, "Base total sin impuestos", "25.00"),
                        campo("impuestos", "number", true, "Impuestos totales", "0.00"),
                        campo("total", "number", true, "Valor de modificacion", "25.00"),
                        campo("motivo", "string", true, "Motivo global de la nota de credito", "Devolucion parcial"),
                        campo("numeroDocumentoModificado", "string", true, "Documento afectado", "001-001-000000123"),
                        campo("fechaEmisionDocumentoModificado", "string", true, "Fecha dd/MM/yyyy del documento afectado", "14/08/2026")
                )),
                new DocumentoSeccionContratoResponse(
                        "detalles",
                        true,
                        List.of(
                                campo("codigo", "string", false, "Codigo principal del item", "MAT-001"),
                                campo("codigoAdicional", "string", false, "Codigo auxiliar opcional", "EXT-001"),
                                campo("descripcion", "string", true, "Descripcion del item", "Tuberia PVC"),
                                campo("cantidad", "number", true, "Cantidad mayor a cero", "2"),
                                campo("precioUnitario", "number", true, "Precio unitario", "10.00"),
                                campo("descuento", "number", false, "Descuento aplicado", "0.00"),
                                campo("precioTotalSinImpuesto", "number", false, "Subtotal del detalle", "20.00"),
                                campo("baseImponible", "number", false, "Base imponible del impuesto", "20.00"),
                                campo("valorImpuesto", "number", false, "Valor del impuesto del detalle", "0.00")
                        )
                )
        );
    }

    private List<DocumentoSeccionContratoResponse> contratoNotaDebito() {
        return List.of(
                seccionEmisor(),
                seccionReceptor(),
                seccionDocumentoBase(List.of(
                        campo("fechaEmision", "date", true, "Fecha ISO del comprobante", "2026-08-14"),
                        campo("secuencial", "string", true, "Secuencial de 9 digitos", "000000125"),
                        campo("moneda", "string", false, "Moneda del documento", "USD"),
                        campo("subtotal", "number", true, "Base total sin impuestos", "25.00"),
                        campo("impuestos", "number", true, "Impuestos totales", "0.00"),
                        campo("total", "number", true, "Valor total del debito", "25.00"),
                        campo("motivo", "string", true, "Motivo principal de respaldo", "Recargo operativo"),
                        campo("numeroDocumentoModificado", "string", true, "Documento afectado", "001-001-000000123"),
                        campo("fechaEmisionDocumentoModificado", "string", true, "Fecha dd/MM/yyyy del documento afectado", "14/08/2026")
                )),
                new DocumentoSeccionContratoResponse(
                        "motivos",
                        true,
                        List.of(
                                campo("razon", "string", true, "Descripcion del cargo o motivo", "Recargo por diferencia"),
                                campo("valor", "number", true, "Valor mayor a cero", "5.00")
                        )
                )
        );
    }

    private List<DocumentoSeccionContratoResponse> contratoRetencion() {
        return List.of(
                seccionEmisor(),
                seccionReceptor(),
                new DocumentoSeccionContratoResponse(
                        "documento",
                        false,
                        List.of(
                                campo("fechaEmision", "date", true, "Fecha ISO del comprobante", "2026-08-14"),
                                campo("secuencial", "string", true, "Secuencial de 9 digitos", "000000126"),
                                campo("xml", "string", true, "XML de retencion listo para validar y procesar", "<comprobanteRetencion .../>")
                        )
                )
        );
    }

    private List<DocumentoSeccionContratoResponse> contratoGuiaRemision() {
        return List.of(
                seccionEmisor(),
                seccionReceptor(),
                seccionDocumentoBase(List.of(
                        campo("fechaEmision", "date", true, "Fecha ISO del comprobante", "2026-08-14"),
                        campo("secuencial", "string", true, "Secuencial de 9 digitos", "000000127"),
                        campo("direccionPartida", "string", true, "Punto de partida", "Bodega central"),
                        campo("fechaInicioTransporte", "string", true, "Fecha dd/MM/yyyy de inicio", "14/08/2026"),
                        campo("fechaFinTransporte", "string", true, "Fecha dd/MM/yyyy de fin", "14/08/2026"),
                        campo("placa", "string", true, "Placa del vehiculo", "ABC1234"),
                        campo("motivoTraslado", "string", false, "Motivo general si no se usa destinatarios[]", "Entrega programada"),
                        campo("numDocSustento", "string", false, "Documento sustento general", "001-001-000000123")
                )),
                new DocumentoSeccionContratoResponse(
                        "destinatarios",
                        true,
                        List.of(
                                campo("identificacion", "string", true, "Identificacion del destinatario", "0102030405"),
                                campo("razonSocial", "string", true, "Nombre del destinatario", "Cliente Demo"),
                                campo("direccion", "string", true, "Direccion del destinatario", "Av. Principal"),
                                campo("motivoTraslado", "string", true, "Motivo del traslado", "Entrega sector norte"),
                                campo("codDocSustento", "string", true, "Codigo del documento sustento", "01"),
                                campo("numDocSustento", "string", true, "Numero del documento sustento", "001-001-000000123")
                        )
                ),
                new DocumentoSeccionContratoResponse(
                        "detalles",
                        true,
                        List.of(
                                campo("codigo", "string", false, "Codigo principal del item", "MAT-001"),
                                campo("descripcion", "string", true, "Descripcion del producto", "Tuberia PVC"),
                                campo("cantidad", "number", true, "Cantidad mayor a cero", "3")
                        )
                )
        );
    }

    private DocumentoSeccionContratoResponse seccionEmisor() {
        return new DocumentoSeccionContratoResponse(
                "emisor",
                false,
                List.of(
                        campo("ruc", "string", true, "RUC de empresa registrada en backend", "1790012345001"),
                        campo("establecimiento", "string", false, "Codigo de establecimiento", "001"),
                        campo("puntoEmision", "string", false, "Codigo de punto de emision", "002"),
                        campo("ambiente", "string", false, "1 pruebas, 2 produccion", "1")
                )
        );
    }

    private DocumentoSeccionContratoResponse seccionReceptor() {
        return new DocumentoSeccionContratoResponse(
                "receptor",
                false,
                List.of(
                        campo("identificacion", "string", true, "Identificacion del cliente o sujeto", "0102030405"),
                        campo("razonSocial", "string", true, "Nombre o razon social", "Cliente Demo"),
                        campo("email", "string", false, "Correo principal del receptor", "cliente@correo.com")
                )
        );
    }

    private DocumentoSeccionContratoResponse seccionDocumentoBase(List<DocumentoCampoContratoResponse> campos) {
        return new DocumentoSeccionContratoResponse("documento", false, campos);
    }

    private DocumentoSeccionContratoResponse seccionDetallesFactura() {
        return new DocumentoSeccionContratoResponse(
                "detalles",
                true,
                List.of(
                        campo("codigo", "string", false, "Codigo principal", "ITEM-1"),
                        campo("descripcion", "string", true, "Descripcion del item", "Servicio de agua"),
                        campo("cantidad", "number", true, "Cantidad", "1"),
                        campo("precioUnitario", "number", true, "Precio unitario", "10.00")
                )
        );
    }

    private DocumentoCampoContratoResponse campo(String nombre, String tipo, boolean requerido, String descripcion, String ejemplo) {
        return new DocumentoCampoContratoResponse(nombre, tipo, requerido, descripcion, ejemplo);
    }
}
