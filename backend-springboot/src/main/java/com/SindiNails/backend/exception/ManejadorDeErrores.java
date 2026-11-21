package com.SindiNails.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

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

    /** Varios campos no válidos: sus mensajes, ordenados por campo para que la respuesta sea siempre la misma. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> camposNoValidos(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
        return conMensaje(HttpStatus.BAD_REQUEST, mensaje);
    }

    private static ResponseEntity<Map<String, String>> conMensaje(HttpStatus estado, RuntimeException e) {
        return conMensaje(estado, e.getMessage());
    }

    private static ResponseEntity<Map<String, String>> conMensaje(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("message", mensaje));
    }
}
