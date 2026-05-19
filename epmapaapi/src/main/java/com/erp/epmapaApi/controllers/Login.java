package com.erp.epmapaApi.controllers;

import com.erp.epmapaApi.DTO.LoginRequest;
import com.erp.epmapaApi.DTO.LoginResponse;
import com.erp.epmapaApi.config.AESUtil;
import com.erp.epmapaApi.config.JwtUtil;
import com.erp.epmapaApi.models.Clientes;
import com.erp.epmapaApi.repositories.ClientesR;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class Login {

    private final ClientesR clientesR;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Clientes cliente = clientesR.findByUsernameAndActivoTrue(request.getUsername())
                .orElse(null);

        if (cliente == null) {
            return ResponseEntity.status(401).body(Map.of("mensaje", "Credenciales inválidas"));
        }

        String passRaw = request.getPassword();
        String storedPassword = cliente.getPassword();

        try {
            String encrypted = AESUtil.cifrar(passRaw);
            String myFunEncoded = myFun(passRaw);

            boolean matchAES = storedPassword.equals(encrypted);
            boolean matchMyFun = storedPassword.equals(myFunEncoded);
            boolean matchDirect = storedPassword.equals(passRaw);

            if (matchAES || matchMyFun || matchDirect) {
                if (matchMyFun && !matchAES) {
                    System.out.println("=== LOGIN FALLBACK myFun ===");
                    System.out.println("Username " + request.getUsername() + ": myFun() matched.");
                }
                if (matchDirect) {
                    System.out.println("=== LOGIN FALLBACK plaintext ===");
                    System.out.println("Username " + request.getUsername() + ": plaintext matched.");
                }
            } else {
                System.out.println("=== DEBUG LOGIN ===");
                System.out.println("Username         : " + request.getUsername());
                System.out.println("Raw input        : " + passRaw);
                System.out.println("Stored in DB     : " + storedPassword);
                System.out.println("Stored length    : " + (storedPassword != null ? storedPassword.length() : 0));
                System.out.println("Stored isBase64  : " + (storedPassword != null && storedPassword.matches("^[A-Za-z0-9+/=]+$")));
                System.out.println("Stored isNumeric : " + (storedPassword != null && storedPassword.matches("\\d+")));
                System.out.println("AES.cifrar()     : " + encrypted);
                System.out.println("AES length       : " + encrypted.length());
                System.out.println("myFun(raw)       : " + myFunEncoded);
                System.out.println("myFun length     : " + myFunEncoded.length());
                return ResponseEntity.status(401).body(Map.of("mensaje", "Credenciales inválidas"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401).body(Map.of("mensaje", "Error al procesar la autenticación"));
        }

        String token = jwtUtil.generateToken(
                cliente.getIdcliente(),
                cliente.getUsername(),
                cliente.getRol()
        );

        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .username(cliente.getUsername())
                .rol(cliente.getRol())
                .idcliente(cliente.getIdcliente())
                .nombre(cliente.getNombre())
                .build());
    }

    public static String myFun(String x) {
        StringBuilder y = new StringBuilder();
        for (int i = 0; i < x.length(); i++) {
            y.append((int) x.charAt(i));
        }

        StringBuilder rtn = new StringBuilder();
        for (int i = 0; i < y.length(); i += 2) {
            rtn.append(y.charAt(i));
        }
        rtn.append(x.trim().length());
        for (int i = y.length() - 1; i >= 0; i -= 2) {
            rtn.append(y.charAt(i));
        }
        return rtn.toString();
    }
}
