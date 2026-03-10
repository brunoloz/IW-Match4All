package es.ucm.fdi.iw.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import es.ucm.fdi.iw.model.Competicion;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.Data;

@RestController
@RequestMapping("api/competiciones")
public class CompeticionController {

    @Autowired
    private EntityManager entityManager;

    @GetMapping
    public List<CompeticionDto> listCompeticiones() {
        return entityManager.createQuery("SELECT c FROM Competicion c", Competicion.class)
            .getResultList()
            .stream()
            .map(CompeticionDto::from)
            .toList();
    }

    @GetMapping("/{id}")
    public CompeticionDto getCompeticion(@PathVariable long id) {
        Competicion competicion = entityManager.find(Competicion.class, id);
        if (competicion == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competición no encontrada");
        }
        return CompeticionDto.from(competicion);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CompeticionDto> createCompeticion(@RequestBody CompeticionCreateRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank() || request.getTipo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan campos obligatorios");
        }

        Competicion competicion = new Competicion();
        competicion.setNombre(request.getNombre());
        competicion.setCapacidad(request.getCapacidad());
        
        try {
            competicion.setTipo(Competicion.Tipo.valueOf(request.getTipo().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de competición inválido. Use LIGA o TORNEO");
        }

        entityManager.persist(competicion);
        entityManager.flush();
        return ResponseEntity.status(HttpStatus.CREATED).body(CompeticionDto.from(competicion));
    }

    @Data
    public static class CompeticionCreateRequest {
        private String nombre;
        private String tipo;
        private int capacidad;
    }

    @Data
    public static class CompeticionDto {
        private long id;
        private String nombre;
        private String tipo;
        private int capacidad;

        public static CompeticionDto from(Competicion competicion) {
            CompeticionDto dto = new CompeticionDto();
            dto.setId(competicion.getId());
            dto.setNombre(competicion.getNombre());
            dto.setTipo(competicion.getTipo().name());
            dto.setCapacidad(competicion.getCapacidad());
            return dto;
        }
    }
}