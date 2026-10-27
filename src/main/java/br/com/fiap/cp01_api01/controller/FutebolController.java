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
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.cp01_api01.service.FutebolService;
import jakarta.validation.Valid;
import br.com.fiap.cp01_api01.dto.FutebolCreateRequest;
import br.com.fiap.cp01_api01.dto.FutebolMapper;
import br.com.fiap.cp01_api01.dto.FutebolResponse;
import br.com.fiap.cp01_api01.dto.FutebolUpdateRequest;
import br.com.fiap.cp01_api01.model.Futebol;
import java.util.List;


@RestController
@RequestMapping("/api/v1/copa")
public class FutebolController {

    @Autowired
    private FutebolService service;

    @Autowired
    private FutebolMapper futebolMapper;

    @PostMapping
    public ResponseEntity<FutebolResponse> create(@Valid @RequestBody FutebolCreateRequest dtoRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(futebolMapper.toDTO(service.createOrUpdate(futebolMapper.toModel(dtoRequest))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FutebolResponse> findById(@PathVariable Long id) {
        return service
                .findById(id)
                .map(futebol -> futebolMapper.toDTO(futebol))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FutebolResponse>> findAll(){
        return ResponseEntity.ok(service.findAll()
        .stream()
        .map(futebol -> futebolMapper.toDTO(futebol))
        .toList()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<FutebolResponse> update(@PathVariable Long id,
            @Valid @RequestBody FutebolUpdateRequest dtoRequest) {
        if (service.findById(id).isPresent()) {
            Futebol futebol = futebolMapper.toModel(id, dtoRequest);
            return ResponseEntity.ok(futebolMapper.toDTO(service.createOrUpdate(futebol)));
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
