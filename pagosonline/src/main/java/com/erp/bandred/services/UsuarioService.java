package com.erp.bandred.services;

import com.erp.bandred.interfaces.UsuarioLogin_int;
import com.erp.bandred.repositories.UsuariosR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    @Autowired
    private UsuariosR u_dao;

    public Long userLogin(String username, String password){
        String pass = convertPassword(password);
        UsuarioLogin_int user = u_dao.getUsuario(username, pass);
        return  user.getIdusuario();
    }

    public String convertPassword(String x) {
        StringBuilder y = new StringBuilder();
        for (int i = 0; i < x.length(); i++) {
            y.append((int) x.charAt(i));  // Convertimos el carácter a su valor Unicode y lo agregamos a 'y'
        }
        StringBuilder rtn = new StringBuilder();
        for (int i = 0; i < y.length(); i += 2) {
            rtn.append(y.charAt(i));  // Agregamos cada segundo carácter
        }
        rtn.append(x.trim().length());  // Agregamos la longitud de la cadena original, sin espacios al principio o al final
        for (int i = y.length() - 1; i >= 0; i -= 2) {
            rtn.append(y.charAt(i));  // Agregamos cada segundo carácter, pero desde el final hacia el inicio
        }
        return rtn.toString();  // Convertimos StringBuilder a String
    }
    public String decodePassword(Long e) {
        String encoded = e.toString();
        // El último carácter representa la longitud original de la cadena
        int originalLength = Character.getNumericValue(encoded.charAt(encoded.length() - 1));

        // Eliminar el último carácter de la longitud de la cadena codificada
        encoded = encoded.substring(0, encoded.length() - 1);

        // Dividimos la cadena en dos partes: mitad inicial y mitad final
        int halfLength = encoded.length() / 2;
        String firstHalf = encoded.substring(0, halfLength);
        String secondHalf = encoded.substring(halfLength);

        StringBuilder y = new StringBuilder();

        // Tomar los caracteres alternados: primero de la primera mitad, luego de la segunda mitad
        for (int i = 0; i < firstHalf.length(); i++) {
            y.append(firstHalf.charAt(i));   // Caracteres alternados del inicio
            if (i < secondHalf.length()) {
                y.append(secondHalf.charAt(secondHalf.length() - 1 - i));  // Alternados desde el final
            }
        }

        StringBuilder decoded = new StringBuilder();

        // Aquí manejamos correctamente los valores Unicode, considerando su longitud variable
        for (int i = 0; i < y.length(); i++) {
            StringBuilder unicodeValue = new StringBuilder();

            // Asegurarnos de tomar todos los dígitos de cada número Unicode
            while (i < y.length() && Character.isDigit(y.charAt(i))) {
                unicodeValue.append(y.charAt(i));
                i++;
            }

            // Convertimos el valor Unicode de nuevo a carácter
            decoded.append((char) Integer.parseInt(unicodeValue.toString()));
        }

        return decoded.toString();
    }
    public static String codificar(String input) {
        StringBuilder hexString = new StringBuilder();

        // Convertimos cada carácter a su valor hexadecimal
        for (char c : input.toCharArray()) {
            hexString.append(String.format("%02x", (int) c));
        }
        return hexString.toString();
    }
    public static String decodificar(String hex) {
        System.out.println(hex);
        StringBuilder decodedString = new StringBuilder();

        // Recorremos cada par de dígitos hexadecimales y los convertimos a un carácter
        for (int i = 0; i < hex.length(); i += 2) {
            String hexChar = hex.substring(i, i + 2);
            decodedString.append((char) Integer.parseInt(hexChar, 16));
        }

        return decodedString.toString();
    }
}
