package br.com.fiap.cp01_api01.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class FinancaUpdateRequest {
    @PositiveOrZero
    private double taxa;

    @NotBlank
    private String emissor;

    @NotBlank
    private String risco;

    @NotBlank
    private String vencimento;

    @Positive
    private int quantidade;
}
