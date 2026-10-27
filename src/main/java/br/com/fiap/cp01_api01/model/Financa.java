package br.com.fiap.cp01_api01.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "financas")
public class Financa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double taxa;
    
    private String emissor;
    @Column(nullable = true)
    private String risco;

    private String vencimento;

    private int quantidade;
}
