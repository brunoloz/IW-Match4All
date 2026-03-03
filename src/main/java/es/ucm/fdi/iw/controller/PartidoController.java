package es.ucm.fdi.iw.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import es.ucm.fdi.iw.model.Competicion;
import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.Partido;
import es.ucm.fdi.iw.model.User;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.Data;

@RestController
@RequestMapping("api/partidos")
public class PartidoController {

    @Autowired
    private EntityManager entityManager;

    @GetMapping
    public List<PartidoDto> listPartidos() {
        return entityManager.createQuery("SELECT p FROM Partido p", Partido.class)
            .getResultList()
            .stream()
            .map(PartidoDto::from)
            .toList();
    }

    @GetMapping("/{id}")
    public PartidoDto getPartido(@PathVariable long id) {
        Partido partido = entityManager.find(Partido.class, id);
        if (partido == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partido no encontrado");
        }
        return PartidoDto.from(partido);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<PartidoDto> createPartido(@RequestBody PartidoCreateRequest request) {
        if (request.getLocalId() == null || request.getVisitanteId() == null || request.getArbitroId() == null
            || request.getCompeticionId() == null || request.getUbicacion() == null || request.getUbicacion().isBlank()
            || request.getFecha() == null || request.getFecha().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan campos obligatorios");
        }

        Equipo local = entityManager.find(Equipo.class, request.getLocalId());
        Equipo visitante = entityManager.find(Equipo.class, request.getVisitanteId());
        User arbitro = entityManager.find(User.class, request.getArbitroId());
        Competicion competicion = entityManager.find(Competicion.class, request.getCompeticionId());

        if (local == null || visitante == null || arbitro == null || competicion == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron entidades relacionadas");
        }

        Partido partido = new Partido();
        partido.setLocal(local);
        partido.setVisitante(visitante);
        partido.setArbitro(arbitro);
        partido.setIdCompeticion(competicion);
        partido.setUbicacion(request.getUbicacion());

        try {
            partido.setFecha(LocalDate.parse(request.getFecha()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fecha debe tener formato yyyy-MM-dd");
        }

        entityManager.persist(partido);
        entityManager.flush();
        return ResponseEntity.status(HttpStatus.CREATED).body(PartidoDto.from(partido));
    }

    @Data
    public static class PartidoCreateRequest {
        private Long localId;
        private Long visitanteId;
        private String ubicacion;
        private String fecha;
        private Long competicionId;
        private Long arbitroId;
    }

    @Data
    public static class PartidoDto {
        private long id;
        private long localId;
        private long visitanteId;
        private String ubicacion;
        private String fecha;
        private long competicionId;
        private long arbitroId;

        public static PartidoDto from(Partido partido) {
            PartidoDto dto = new PartidoDto();
            dto.setId(partido.getId());
            dto.setLocalId(partido.getLocal().getId());
            dto.setVisitanteId(partido.getVisitante().getId());
            dto.setUbicacion(partido.getUbicacion());
            dto.setFecha(partido.getFecha().toString());
            dto.setCompeticionId(partido.getIdCompeticion().getId());
            dto.setArbitroId(partido.getArbitro().getId());
            return dto;
        }
    }
}
