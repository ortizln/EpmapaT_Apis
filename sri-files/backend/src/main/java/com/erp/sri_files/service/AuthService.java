package com.erp.sri_files.service;

import com.erp.sri_files.domain.auth.UsuarioSistema;
import com.erp.sri_files.dto.request.LoginRequest;
import com.erp.sri_files.dto.response.LoginResponse;
import com.erp.sri_files.dto.response.UsuarioAutenticadoResponse;
import com.erp.sri_files.exceptions.AuthException;
import com.erp.sri_files.repositories.auth.UsuarioSistemaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class AuthService {
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PasswordHashService passwordHashService;
    private final AccessControlService accessControlService;

    @Value("${sri-files.auth.secret:sri-files-dev-secret}")
    private String configuredSecret;

    @Value("${sri-files.auth.expiration-seconds:28800}")
    private long expirationSeconds;

    public AuthService(
            UsuarioSistemaRepository usuarioSistemaRepository,
            PasswordHashService passwordHashService,
            AccessControlService accessControlService
    ) {
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.passwordHashService = passwordHashService;
        this.accessControlService = accessControlService;
    }

    public LoginResponse login(LoginRequest request) {
        UsuarioSistema usuario = buscarUsuarioActivo(request.username());
        if (!passwordHashService.matches(request.password(), usuario.getPasswordHash(), usuario.getPasswordSalt())) {
            throw new AuthException("Credenciales invalidas");
        }

        UsuarioAutenticadoResponse usuarioResponse = construirUsuario(usuario);
        return new LoginResponse(
                emitirToken(usuario.getUsername(), expirationSeconds),
                emitirToken(usuario.getUsername(), expirationSeconds * 2),
                usuarioResponse
        );
    }

    public UsuarioAutenticadoResponse obtenerUsuarioDesdeToken(String token) {
        String username = validarTokenYObtenerUsername(token);
        return construirUsuario(buscarUsuarioActivo(username));
    }

    public void validarToken(String token) {
        validarTokenYObtenerUsername(token);
    }

    private String validarTokenYObtenerUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                throw new AuthException("Token invalido");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            if (!firmar(payload).equals(parts[1])) {
                throw new AuthException("Token invalido");
            }

            String[] tokenParts = payload.split("\\|");
            if (tokenParts.length != 2) {
                throw new AuthException("Token invalido");
            }

            String username = tokenParts[0];
            long expiresAt = Long.parseLong(tokenParts[1]);
            if (Instant.now().getEpochSecond() > expiresAt) {
                throw new AuthException("Token expirado");
            }

            buscarUsuarioActivo(username);
            return username;
        } catch (IllegalArgumentException ex) {
            throw new AuthException("Token invalido");
        }
    }

    private UsuarioSistema buscarUsuarioActivo(String username) {
        UsuarioSistema usuario = usuarioSistemaRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new AuthException("Credenciales invalidas"));

        if (!usuario.isActivo()) {
            throw new AuthException("Usuario inactivo");
        }

        return usuario;
    }

    private UsuarioAutenticadoResponse construirUsuario(UsuarioSistema usuario) {
        List<String> roles = List.of(usuario.getRol());
        return new UsuarioAutenticadoResponse(
                usuario.getUuid().toString(),
                usuario.getNombre(),
                usuario.getCorreo(),
                roles,
                roles.stream()
                        .flatMap(rol -> accessControlService.obtenerPermisosPorRol(rol).stream())
                        .distinct()
                        .toList()
        );
    }

    private String emitirToken(String username, long expiresInSeconds) {
        long expiresAt = Instant.now().plusSeconds(expiresInSeconds).getEpochSecond();
        String payload = username + "|" + expiresAt;
        String encodedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + firmar(payload);
    }

    private String firmar(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(configuredSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception ex) {
            throw new AuthException("No fue posible generar el token");
        }
    }
}
