package com.SindiNails.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CitasPorDiaDto {
    private String name;
    @JsonProperty("Citas")
    private long citas;
}

