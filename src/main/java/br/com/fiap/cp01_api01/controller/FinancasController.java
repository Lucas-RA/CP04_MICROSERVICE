package br.com.fiap.cp01_api01.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.cp01_api01.service.FinancaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

import br.com.fiap.cp01_api01.dto.FinancaCreateRequest;
import br.com.fiap.cp01_api01.dto.FinancaMapper;
import br.com.fiap.cp01_api01.dto.FinancaResponse;
import br.com.fiap.cp01_api01.dto.FinancaUpdateRequest;
import br.com.fiap.cp01_api01.model.Financa;
import java.util.List;


@RestController
@RequestMapping("/api/v1/financas")
public class FinancasController {

    @Autowired
    private FinancaService service;

    @Autowired
    private FinancaMapper financaMapper;

    @PostMapping
    public ResponseEntity<FinancaResponse> create(@Valid @RequestBody FinancaCreateRequest dtoRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financaMapper.toDTO(service.createOrUpdate(financaMapper.toModel(dtoRequest))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinancaResponse> findById(@PathVariable Long id) {
        return service
                .findById(id)
                .map(financa -> financaMapper.toDTO(financa))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FinancaResponse>> findAll() {
        return ResponseEntity.ok(service.findAll()
        .stream()
        .map(financa -> financaMapper.toDTO(financa))
        .toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinancaResponse> update(@PathVariable Long id,
            @Valid @RequestBody FinancaUpdateRequest dtoRequest) {

        if (service.findById(id).isPresent()) {
            Financa financa = financaMapper.toModel(id, dtoRequest);
            financa.setId(id);
            return ResponseEntity.ok(financaMapper.toDTO(service.createOrUpdate(financa)));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
         if (service.findById(id).isPresent()) {
            service.deleteById(id);
            return ResponseEntity.noContent().build();
    } else{
        return ResponseEntity.notFound().build();
    }
}
}
