package com.SindiNails.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Traduce los errores que el servicio explica con un mensaje a su estado HTTP,
 * con el cuerpo {"message": "..."} que muestra el frontend.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Map<String, String>> reglaDeNegocio(ReglaDeNegocioException e) {
        return conMensaje(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(PeticionNoValidaException.class)
    public ResponseEntity<Map<String, String>> peticionNoValida(PeticionNoValidaException e) {
        return conMensaje(HttpStatus.BAD_REQUEST, e);
    }

    private static ResponseEntity<Map<String, String>> conMensaje(HttpStatus estado, RuntimeException e) {
        return ResponseEntity.status(estado).body(Map.of("message", e.getMessage()));
    }
}
