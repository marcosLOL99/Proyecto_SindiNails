package com.SindiNails.backend.exception;

/**
 * La petición es correcta pero incumple una regla del salón
 * (por ejemplo, cancelar una cita ya cancelada o reservar un hueco ocupado).
 * Su mensaje está pensado para mostrarse tal cual a la clienta o a la administración.
 */
public class ReglaDeNegocioException extends RuntimeException {
    public ReglaDeNegocioException(String message) {
        super(message);
    }
}
