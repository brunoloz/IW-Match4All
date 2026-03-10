package es.ucm.fdi.iw.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import es.ucm.fdi.iw.model.Acta;
import es.ucm.fdi.iw.model.Partido;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.Data;

@RestController
@RequestMapping("api/actas")
public class ActaController {

    @Autowired
    private EntityManager entityManager;

    @GetMapping
    public List<ActaDto> listActas() {
        return entityManager.createQuery("SELECT a FROM Acta a", Acta.class)
            .getResultList()
            .stream()
            .map(ActaDto::from)
            .toList();
    }

    @GetMapping("/{id}")
    public ActaDto getActa(@PathVariable long id) {
        Acta acta = entityManager.find(Acta.class, id);
        if (acta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Acta no encontrada");
        }
        return ActaDto.from(acta);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ActaDto> createActa(@RequestBody ActaCreateRequest request) {
        if (request.getPartidoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID del partido es obligatorio");
        }

        Partido partido = entityManager.find(Partido.class, request.getPartidoId());
        if (partido == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El partido especificado no existe");
        }

        Acta acta = new Acta();
        acta.setPartido(partido);
        acta.setGolesLocal(request.getGolesLocal());
        acta.setGolesVisitante(request.getGolesVisitante());

        entityManager.persist(acta);
        entityManager.flush();
        return ResponseEntity.status(HttpStatus.CREATED).body(ActaDto.from(acta));
    }

    @Data
    public static class ActaCreateRequest {
        private long partidoId;
        private long golesLocal;
        private long golesVisitante;
    }

    @Data
    public static class ActaDto {
        private long id;
        private long partidoId;
        private long golesLocal;
        private long golesVisitante;

        public static ActaDto from(Acta acta) {
            ActaDto dto = new ActaDto();
            dto.setId(acta.getId());
            dto.setPartidoId(acta.getPartido().getId()); 
            dto.setGolesLocal(acta.getGolesLocal());
            dto.setGolesVisitante(acta.getGolesVisitante());
            return dto;
        }
    }
}