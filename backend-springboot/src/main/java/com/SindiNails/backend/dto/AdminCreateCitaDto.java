package com.SindiNails.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminCreateCitaDto extends CreateCitaDto {

    @NotNull(message = "El ID del cliente no puede ser nulo.")
    private Long idCliente;
}
