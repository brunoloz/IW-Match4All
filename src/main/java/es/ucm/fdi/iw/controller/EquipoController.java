package es.ucm.fdi.iw.controller;

import java.util.ArrayList;
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

import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.User;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.Data;

@RestController
@RequestMapping("api/equipos")
public class EquipoController {

    @Autowired
    private EntityManager entityManager;

    @GetMapping
    public List<EquipoDto> listEquipos() {
        return entityManager.createQuery("SELECT e FROM Equipo e", Equipo.class)
            .getResultList()
            .stream()
            .map(EquipoDto::from)
            .toList();
    }

    @GetMapping("/{id}")
    public EquipoDto getEquipo(@PathVariable long id) {
        Equipo equipo = entityManager.find(Equipo.class, id);
        if (equipo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo no encontrado");
        }
        return EquipoDto.from(equipo);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<EquipoDto> createEquipo(@RequestBody EquipoCreateRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nombre es obligatorio");
        }

        Equipo equipo = new Equipo();
        equipo.setNombre(request.getNombre());
        equipo.setEscudo(request.getEscudo());

        if (request.getCapitanId() != null) {
            User capitan = entityManager.find(User.class, request.getCapitanId());
            if (capitan == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Capitan no encontrado");
            }
            equipo.setCapitan(capitan);
        }

        entityManager.persist(equipo);
        entityManager.flush();

        if (request.getJugadorIds() != null) {
            for (Long userId : request.getJugadorIds()) {
                if (userId == null) {
                    continue;
                }
                User jugador = entityManager.find(User.class, userId);
                if (jugador == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Jugador no encontrado: " + userId);
                }
                jugador.setEquipo(equipo);
            }
        }

        if (equipo.getCapitan() != null) {
            equipo.getCapitan().setEquipo(equipo);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(EquipoDto.from(equipo));
    }

    @Data
    public static class EquipoCreateRequest {
        private String nombre;
        private String escudo;
        private Long capitanId;
        private List<Long> jugadorIds;
    }

    @Data
    public static class EquipoDto {
        private long id;
        private String nombre;
        private String escudo;
        private Long capitanId;
        private List<Long> jugadorIds = new ArrayList<>();

        public static EquipoDto from(Equipo equipo) {
            EquipoDto dto = new EquipoDto();
            dto.setId(equipo.getId());
            dto.setNombre(equipo.getNombre());
            dto.setEscudo(equipo.getEscudo());
            dto.setCapitanId(equipo.getCapitan() == null ? null : equipo.getCapitan().getId());
            if (equipo.getJugadores() != null) {
                dto.setJugadorIds(equipo.getJugadores().stream().map(User::getId).toList());
            }
            return dto;
        }
    }
}
