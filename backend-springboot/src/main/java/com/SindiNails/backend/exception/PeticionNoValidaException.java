package com.SindiNails.backend.exception;

/**
 * Los datos de la petición no son válidos y no se puede comprobar con jakarta.validation
 * (por ejemplo, un campo obligatorio solo para cierto tipo de bloqueo).
 * Su mensaje está pensado para mostrarse tal cual.
 */
public class PeticionNoValidaException extends RuntimeException {
    public PeticionNoValidaException(String message) {
        super(message);
    }
}
