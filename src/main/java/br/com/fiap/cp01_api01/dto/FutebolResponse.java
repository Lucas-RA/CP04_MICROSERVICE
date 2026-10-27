package br.com.fiap.cp01_api01.dto;

import lombok.Data;

@Data
public class FutebolResponse {
    
    private Long id;

    private int ano;
    
    private String capeao;
 
    private String sede;

    private String vice;

    private String melhorJogador;
}
