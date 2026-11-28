package com.SindiNails.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    /** Campos no válidos: sus mensajes, ordenados por campo y luego por mensaje para que la respuesta sea siempre la misma. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> camposNoValidos(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField).thenComparing(FieldError::getDefaultMessage))
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));
        return conMensaje(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** Cuerpo que no se puede leer: JSON mal formado, una fecha mal escrita o un valor que no existe (por ejemplo, un tipo). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return conMensaje(HttpStatus.BAD_REQUEST, "La petición no tiene un formato válido.");
    }

    private static ResponseEntity<Map<String, String>> conMensaje(HttpStatus estado, RuntimeException e) {
        return conMensaje(estado, e.getMessage());
    }

    private static ResponseEntity<Map<String, String>> conMensaje(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("message", mensaje));
    }
}
