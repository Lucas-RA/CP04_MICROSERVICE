package br.com.fiap.cp01_api01.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FutebolCreateRequest {
    @Min(1930)
    @Max(2100)
    private int ano;

    @NotBlank
    private String capeao;

    @NotBlank
    private String sede;

    @NotBlank
    private String vice;

    @NotBlank
    private String melhorJogador;
}
