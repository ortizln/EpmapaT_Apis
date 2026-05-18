package com.erp.sri_files.services;

import com.erp.sri_files.models.Definir;
import com.erp.sri_files.models.Factura;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
//ESTE SERVICIO GENERA LA CLAVE DE ACCESO PARA LAS FACTURAS
@Service
public class CAG_FacturasService {

    // Tipo de comprobante: Factura = "01"
    private static final String TIPO_COMPROBANTE_FACTURA = "01";
    private static final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("ddMMyyyy");

    /**
     * Genera la clave de acceso (49 dígitos) para Factura (01).
     * Estructura base de 48 dígitos:
     * fechaEmision(8) + tipoComprobante(2) + ruc(13) + ambiente(1) + serie(6) +
     * secuencial(9) + codigoNumerico(8) + tipoEmision(1)  => 48
     * + dígito verificador (módulo 11) => 49
     */
    public String generarClaveAcceso(Factura factura, Definir definir) {
        Objects.requireNonNull(factura, "Factura no puede ser nula");
        Objects.requireNonNull(definir, "Definir no puede ser nulo");

        // 1) Fecha de emisión (DDMMYYYY)
        String fechaEmision = formatToDDMMYYYY(factura.getFechaemision());

        // 2) Tipo de comprobante (2 dígitos)
        String tipoComprobante = TIPO_COMPROBANTE_FACTURA;

        // 3) RUC del emisor (13 dígitos)
        String ruc = leftPadDigits(requireDigits(definir.getRuc(), "RUC"), 13);

        // 4) Ambiente (1 dígito: 1=Pruebas, 2=Producción)
        String ambiente = requireDigits(definir.getTipoambiente() == null ? null : definir.getTipoambiente().toString(),
                "Tipo de ambiente");
        if (!(ambiente.equals("1") || ambiente.equals("2"))) {
            throw new IllegalArgumentException("El ambiente debe ser '1' (pruebas) o '2' (producción)");
        }

        // 5) Serie: 3 dígitos establecimiento + 3 dígitos punto de emisión
        String estab = leftPadDigits(requireDigits(factura.getEstablecimiento(), "Establecimiento"), 3);
        String ptoEmi = leftPadDigits(requireDigits(factura.getPuntoemision(), "Punto de emisión"), 3);
        String serie = estab + ptoEmi;

        // 6) Secuencial (9 dígitos)
        String secuencial = leftPadDigits(requireDigits(factura.getSecuencial(), "Secuencial"), 9);

        // 7) Código numérico (8 dígitos) — aleatorio o el que definas en tu negocio
        String codigoNumerico = generarCodigoNumerico8();

        // 8) Tipo de emisión (1 dígito, normal=1)
        String tipoEmision = "1";

        // Concatenar base de 48 dígitos
        String base48 = fechaEmision + tipoComprobante + ruc + ambiente + serie + secuencial + codigoNumerico + tipoEmision;

        if (base48.length() != 48) {
            throw new IllegalStateException("La base de la clave de acceso debe tener 48 dígitos, actual: " + base48.length());
        }
        if (!base48.chars().allMatch(Character::isDigit)) {
            throw new IllegalStateException("La base de la clave de acceso debe contener solo dígitos");
        }

        // 9) Dígito verificador (módulo 11 con pesos 2..7 de derecha a izquierda)
        char dv = calcularDigitoVerificadorModulo11(base48);

        // 10) Clave completa (49 dígitos)
        System.out.println("CLAVE COMPLETA: "+base48 + dv);
        return base48 + dv;
    }

    // ----------------- Utilidades -----------------

    /** Formatea LocalDateTime a DDMMYYYY (SRI). */
    public static String formatToDDMMYYYY(LocalDateTime dateTime) {
        if (dateTime == null) throw new IllegalArgumentException("La fecha no puede ser nula");
        return dateTime.format(DDMMYYYY);
    }

    /** Genera un código numérico aleatorio de 8 dígitos (00000000–99999999). */
    private static String generarCodigoNumerico8() {
        // Si no deseas ceros a la izquierda, usa rango 10000000..99999999
        int n = ThreadLocalRandom.current().nextInt(0, 100_000_000);
        return String.format("%08d", n);
    }

    /**
     * Cálculo del dígito verificador para clave de acceso SRI (módulo 11):
     * - Ponderación cíclica 2..7 de derecha a izquierda.
     * - dv = 11 - (suma % 11)
     * - Si dv == 11 -> 0 ; si dv == 10 -> 1.
     */
    private static char calcularDigitoVerificadorModulo11(String base48) {
        if (base48 == null || base48.length() != 48 || !base48.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Se espera base de 48 dígitos numéricos");
        }
        final int[] pesos = {2, 3, 4, 5, 6, 7};
        int suma = 0;
        int idx = 0;
        for (int i = base48.length() - 1; i >= 0; i--) {
            int digito = base48.charAt(i) - '0';
            suma += digito * pesos[idx];
            idx = (idx + 1) % pesos.length;
        }
        int mod = suma % 11;
        int dv = 11 - mod;
        if (dv == 11) dv = 0;
        else if (dv == 10) dv = 1;
        return (char) ('0' + dv);
    }

    /** Verifica y normaliza que el texto contenga solo dígitos. */
    private static String requireDigits(String value, String label) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(label + " no puede ser nulo o vacío");
        String digits = value.replaceAll("\\D", "");
        if (digits.isEmpty())
            throw new IllegalArgumentException(label + " debe contener dígitos");
        return digits;
    }

    /** Rellena a la izquierda con ceros hasta la longitud indicada. */
    private static String leftPadDigits(String digits, int length) {
        if (!digits.chars().allMatch(Character::isDigit))
            throw new IllegalArgumentException("Valor no numérico: " + digits);
        if (digits.length() > length)
            throw new IllegalArgumentException("Longitud mayor a " + length + ": " + digits);
        return String.format("%0" + length + "d", Long.parseLong(digits));
    }

    public static void validarClaveAcceso(String clave) {
        if (clave == null || !clave.matches("\\d{49}")) {
            throw new IllegalArgumentException("claveAcceso inválida: debe tener 49 dígitos");
        }
    }
}
