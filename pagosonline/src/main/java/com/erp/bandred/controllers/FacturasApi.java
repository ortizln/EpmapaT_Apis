package com.erp.bandred.controllers;

import com.erp.bandred.DTO.FacturaRequestDTO;
import com.erp.bandred.DTO.ReportdataDTO;
import com.erp.bandred.config.AESUtil;
import com.erp.bandred.interfaces.FacturasCobradas;
import com.erp.bandred.interfaces.LastConection_int;
import com.erp.bandred.models.*;
import com.erp.bandred.repositories.RubroxfacR;
import com.erp.bandred.repositories.TmpinteresxfacR;
import com.erp.bandred.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/pagonline/facturas")
public class FacturasApi {
    private static final ZoneId ECUADOR_ZONE = ZoneId.of("America/Guayaquil");

    @Autowired
    private FacturasService facturasService;
    @Autowired
    private CajasService cajasService;
    @Autowired
    private RecaudacionService recaudacionService;
    @Autowired
    private TmpinteresxfacR tmpinteresxfacR;
    @Autowired
    private ImpuestoService impuestoService;
    @Autowired
    private RubroxfacR rubroxfacR;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> _home() {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang='es'><head><meta charset='utf-8'>");
        html.append("<title>📋 API Cobros Electrónicos</title>");
        html.append("<style>");
        html.append("body{font-family:'Segoe UI',Arial,sans-serif;background:#f5f7fb;margin:0;padding:32px;color:#2c3e50}");
        html.append("h1{margin:0 0 16px} .muted{color:#6b7280;font-size:14px}");
        html.append(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:14px;margin-top:16px}");
        html.append(".card{background:#fff;border-radius:12px;padding:18px;box-shadow:0 6px 18px rgba(0,0,0,.06)}");
        html.append(".method{font-weight:700;padding:2px 8px;border-radius:6px;color:#fff;margin-right:8px}");
        html.append(".GET{background:#27ae60}.POST{background:#2980b9}.PUT{background:#f39c12}");
        html.append("code{background:#eef;border-radius:6px;padding:2px 6px}");
        html.append("pre{background:#0b1020;color:#e5e7eb;padding:12px;border-radius:10px;overflow:auto}");
        html.append("a{color:#0ea5e9;text-decoration:none} a:hover{text-decoration:underline}");
        html.append("table{width:100%;border-collapse:collapse;margin-top:8px} th,td{padding:6px 8px;border-bottom:1px solid #eef}");
        html.append(".tag{display:inline-block;background:#eef;border-radius:999px;padding:2px 8px;margin:0 6px 6px 0;font-size:12px}");
        html.append("</style></head><body>");

        html.append("<h1>📋 Endpoints de <i>pagos eletrónicos</i></h1>");
        html.append("<div class='muted'>Última actualización: ").append(LocalDateTime.now()).append("</div>");
        html.append("<div class='grid'>");

        // ---------- Endpoint: /facturas/sincobrar ----------
        html.append("<section class='card'>");
        html.append("<div><span class='method GET'>GET</span><code>/facturas/sincobrar?user={idUsuario}&cuenta={idCuenta}</code></div>");
        html.append("<p>Obtiene las facturas pendientes de cobro para una <b>cuenta</b>, validando que la caja del <b>usuario</b> esté abierta.</p>");
        html.append("<div><span class='tag'>Query</span> <code>user</code> (Long), <code>cuenta</code> (Long)</div>");
        html.append("<h4>Respuesta</h4>");
        html.append("<table><tr><th>HTTP</th><th>Body</th></tr>");
        html.append("<tr><td>200</td><td><code>{...lista o detalle de facturas...}</code> o <code>{\"mensaje\":\"Caja no iniciada\"}</code></td></tr>");
        html.append("</table>");
        html.append("<h4>Ejemplo real</h4>");
        html.append("<pre>{\n"
                + "  \"cuenta\": 12,\n"
                + "  \"total\": 10.8,\n"
                + "  \"responsablepago\": \"DIAZ  GENNY CECILIA\",\n"
                + "  \"facturas\": [2824638],\n"
                + "  \"cedula\": \"0400801346\",\n"
                + "  \"direccion\": \"CDLA. PADRE CARLOS DE LA 24\"\n"
                + "}</pre>");
        html.append("<h4>Ejemplo de request</h4>");
        html.append("<pre>GET /facturas/sincobrar?user=10041&cuenta=12</pre>");
        html.append("<h4>cURL</h4>");
        html.append("<pre>curl -X GET \"http://localhost:8080/facturas/sincobrar?user=10041&cuenta=12\"</pre>");
        html.append("</section>");

        // ---------- Endpoint: /facturas/cobrar ----------
        html.append("<section class='card'>");
        html.append("<div><span class='method PUT'>PUT</span><code>/facturas/cobrar</code></div>");
        html.append("<p>Cobra una o varias facturas asociadas a una cuenta. Crea la <b>Recaudación</b>, marca facturas como pagadas, registra interés si existe.</p>");
        html.append("<div><span class='tag'>Body (JSON)</span></div>");
        html.append("<pre>{\n  \"autentification\": 10041,\n  \"cuenta\": 1001,\n  \"secuencial\": \"ABC-001-0000123\",\n  \"fechacompensacion\": \"2025-10-28\",\n  \"recaudacion\": { \"totalpagar\": 45.50 }\n}</pre>");
        html.append("<h4>Respuestas</h4>");
        html.append("<table><tr><th>HTTP</th><th>Body</th></tr>");
        html.append("<tr><td>200</td><td><code>{\"status\":\"00\",\"message\":\"TRANSACCIÓN CON ÉXITO\",\"mensaje\":\"Facturas cobradas con éxito\"}</code></td></tr>");
        html.append("<tr><td>200</td><td><code>{\"mensaje\":\"Caja cerrada no se puede cobrar\"}</code></td></tr>");
        html.append("<tr><td>400</td><td><code>{\"error\":\"&lt;detalle&gt;\"}</code></td></tr>");
        html.append("</table>");
        html.append("<h4>cURL</h4>");
        html.append("<pre>curl -X PUT http://localhost:8080/facturas/cobrar \\\n -H \"Content-Type: application/json\" \\\n -d '{\n   \"autentification\":10041,\n   \"cuenta\":1001,\n   \"secuencial\":\"ABC-001-0000123\",\n   \"fechacompensacion\":\"2025-10-28\",\n   \"recaudacion\":{\"totalpagar\":45.50}\n }'</pre>");
        html.append("</section>");

        // ---------- Endpoint: /facturas/reporte ----------
        html.append("<section class='card'>");
        html.append("<div><span class='method POST'>POST</span><code>/facturas/reporte</code></div>");
        html.append("<p>Genera reporte de facturas cobradas por usuario en un rango de fecha y hora.</p>");
        html.append("<div><span class='tag'>Body (JSON)</span></div>");
        html.append("<pre>{\n  \"idusuario\": 10041,\n  \"df\": \"2025-10-01\",\n  \"hf\": \"2025-10-25\",\n  \"dh\": \"08:00:00\",\n  \"hh\": \"17:00:00\"\n}</pre>");
        html.append("<h4>Respuesta</h4>");
        html.append("<p>Lista de objetos <code>FacturasCobradas</code> con campos como <code>nrubros</code>, <code>valortotal</code>, <code>planilla</code>, <code>usuario</code>, <code>nombre</code>, etc.</p>");
        html.append("<h4>cURL</h4>");
        html.append("<pre>curl -X POST http://localhost:165/facturas/reporte \\\n -H \"Content-Type: application/json\" \\\n -d '{\n   \"idusuario\":10041,\n   \"df\":\"2025-10-01\",\n   \"hf\":\"2025-10-25\",\n   \"dh\":\"08:00:00\",\n   \"hh\":\"17:00:00\"\n }'</pre>");
        html.append("</section>");

        html.append("</div>"); // grid
        html.append("<p class='muted' style='margin-top:24px'>Tip: usa la cabecera <code>Content-Type: application/json</code> en peticiones con cuerpo.</p>");
        html.append("</body></html>");
        return ResponseEntity.ok(html.toString());
    }

    @GetMapping("/sincobrar")
    public ResponseEntity<Object> getFacturasSinCobro(@RequestParam String user,@RequestParam Long cuenta) throws Exception {
        Object datos = facturasService.findFacturasSinCobro(1L, cuenta);
        if(datos == null){
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje","Caja no iniciada");
            return ResponseEntity.ok(respuesta);
        }else{
            return ResponseEntity.ok(datos);

        }
    }

    @PutMapping("/cobrar")
    public ResponseEntity<Map<String, Object>> cobrar_Factura(@RequestBody FacturaRequestDTO facturaRequest) throws Exception {
        ZonedDateTime nowInEcuador = ZonedDateTime.now(ECUADOR_ZONE).withNano(0);
        LocalDate fechaCobro = nowInEcuador.toLocalDate();
        LocalDateTime fechaHoraCobro = nowInEcuador.toLocalDateTime();
        LocalTime horaCobro = nowInEcuador.toLocalTime();
        Map<String, Object> respuesta = new HashMap<>();
        Long _user = Long.valueOf(AESUtil.descifrar(facturaRequest.getAutentification()));

        LastConection_int lastConection = cajasService.getLastConectionByUduario(_user);
        //DECLARAR NUEVA RECAUDACION
        Recaudacion recaudacion = new Recaudacion();
        if(lastConection == null){
            respuesta.put("mensaje", "Caja cerrada no se puede cobrar");
            return ResponseEntity.ok(respuesta);
        }
        if(lastConection.getEstado() == 1) {
            try {
                // Imprimir las claves "facturas"
                //List<Long> facturas = facturaRequest.getFacturas();
                List<Long> facturas = facturasService.getListaPlanillas(facturaRequest.getCuenta());
                //Aqui voy a crear la lógica para crear una nueva recaudacion
                Usuarios recuadador = new Usuarios();
                recuadador.setIdusuario(_user);
                recaudacion.setEstado(1L);
                recaudacion.setFechacobro(fechaHoraCobro);
                recaudacion.setRecaudador(_user);
                recaudacion.setTotalpagar(facturaRequest.getRecaudacion().getTotalpagar());
                recaudacion.setValor(facturaRequest.getRecaudacion().getTotalpagar());
                recaudacion.setFormapago(6L);
                recaudacion.setRecibo(BigDecimal.valueOf(0));
                recaudacion.setCambio(BigDecimal.valueOf(0));
                recaudacion.setFeccrea(fechaHoraCobro);
                recaudacion.setUsucrea(_user);
                Recaudacion recaudacion_saved = recaudacionService.save(recaudacion);
                if (recaudacion_saved != null) {
                    // Aquí podrías agregar la lógica para cobrar cada factura
                    for (Long facturaId : facturas) {
                        Facturas _factura = facturasService.findFacturaById(facturaId).orElseThrow();
                        if (_factura.getPagado() == 0 || (_factura.getEstado() == 3 && _factura.getPagado() == 1)) {
                            //DECLARAR NUEVA OBJETO FACXRECAUDA
                            Facxrecauda facxrecauda = new Facxrecauda();
                            if (_factura.getEstado() == 3) {
                                _factura.setEstado(1L);
                            }
                            BigDecimal interesapagar = (tmpinteresxfacR.interesapagar(facturaId));
                            _factura.setPagado(1);
                            _factura.setFechacobro(fechaCobro);
                            _factura.setHoracobro(horaCobro);
                            _factura.setUsuariocobro(_user);
                            _factura.setInterescobrado(interesapagar);
                            _factura.setFormapago(6L);
                            _factura.setSecuencialfacilito(facturaRequest.getSecuencial());
                            _factura.setFechacompensacion(facturaRequest.getFechacompensacion());
                            _factura.setSwiva(impuestoService.calcularIva(facturaId));
                            if (interesapagar.compareTo(BigDecimal.ZERO) > 0) {
                                Rubros rubro = new Rubros();
                                Facturas factura = new Facturas();
                                factura.setIdfactura(facturaId);
                                Rubroxfac rxf = new Rubroxfac();
                                rubro.setIdrubro(5L);
                                rxf.setIdfactura_facturas(factura);
                                rxf.setCantidad(BigDecimal.ONE);
                                rxf.setValorunitario(interesapagar);
                                rxf.setIdrubro_rubros(rubro);
                                rubroxfacR.save(rxf);
                            }
                            Facturas facCobrada = facturasService.cobrarFactura(_factura);
                            facxrecauda.setEstado(1L);
                            facxrecauda.setIdfactura(facCobrada);
                            facxrecauda.setIdrecaudacion(recaudacion_saved);
                            respuesta.put("status", "00");
                            respuesta.put("message", "TRANSACCIÓN CON ÉXITO");
                        } else {
                            respuesta.put("Facturas no cobradas: ", _factura.getIdfactura());
                        }
                    }
                    respuesta.put("mensaje", "Facturas cobradas con éxito");
                } else {
                    respuesta.put("mensaje", "No se puede cobrar porque la forma de cobro no existe");
                }
                return ResponseEntity.ok(respuesta);
            } catch (Exception e) {
                respuesta.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
            }
        }
        else{
            respuesta.put("mensaje", "Caja cerrada no se puede cobrar");
            return ResponseEntity.ok(respuesta);
        }
    }

    @PostMapping("/reporte")
    public ResponseEntity<List<FacturasCobradas>> getReporteFacturasCobradas(@RequestBody ReportdataDTO datos){
        Long idusuario = datos.getIdusuario();
        LocalDate df = datos.getDf();
        LocalTime dh = datos.getDh();
        LocalDate hf = datos.getHf();
        LocalTime hh = datos.getHh();
        return ResponseEntity.ok(facturasService.getReporteFacturasCobradas(idusuario,df,hf,dh, hh));
    }
}
