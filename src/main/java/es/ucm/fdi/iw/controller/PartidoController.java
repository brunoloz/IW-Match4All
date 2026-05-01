package es.ucm.fdi.iw.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import es.ucm.fdi.iw.model.Acta;
import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.Evento;
import es.ucm.fdi.iw.model.Partido;
import es.ucm.fdi.iw.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
@RequestMapping("partido")
public class PartidoController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws", "topics" }) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }
    
    @GetMapping("/{id}")
    @Transactional
    public String partido(Model model, @PathVariable("id") long id, HttpSession session) {

        User u = (User) session.getAttribute("u");

        Partido partido = entityManager.find(Partido.class, id);

        Acta acta = entityManager.createQuery(
        "SELECT a FROM Acta a WHERE a.partido.id = :id", Acta.class)
        .setParameter("id", id)
        .getResultList()
        .stream()
        .findFirst()
        .orElse(null);

        model.addAttribute("u", u);
        model.addAttribute("acta", acta);
        model.addAttribute("partido", partido);

        return "partido";
    }

    @PostMapping("/iniciar/{id}")
    @Transactional
    @ResponseBody
    public Map<String, String> iniciar(Model model, @PathVariable("id") long id, HttpSession session) {

        Partido partido = entityManager.find(Partido.class, id);

        partido.setEstado(Partido.State.EN_CURSO);

        Acta acta = new Acta();
        acta.setPartido(partido);
        acta.setGoles_local(0);
        acta.setGoles_visitante(0);
        List<Evento> eventos = new ArrayList<>();
        acta.setEventos(eventos);

        entityManager.persist(acta);

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode mensaje = mapper.createObjectNode(); 

            mensaje.put("tipo", "INICIO_PARTIDO");
            mensaje.put("partidoId", id);
            
            messagingTemplate.convertAndSend("/topic/partido/" + id, mapper.writeValueAsString(mensaje));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Map.of("status", "ok");
    }

    @PostMapping("/finalizar/{id}")
    @Transactional
    @ResponseBody
    public Map<String,String> finalizar(Model model, @PathVariable("id") long id, HttpSession session) {

        User u = (User) session.getAttribute("u");

        if(u.hasRole(User.Role.ARBITRO)){
            int partidos_dirigidos = u.getPartidosJugados();
            u.setPartidosJugados(partidos_dirigidos + 1);

            entityManager.merge(u);
        }

        Partido partido = entityManager.find(Partido.class, id);

        partido.setEstado(Partido.State.FINALIZADO);

        /*Acta acta = new Acta();
        acta.setPartido(partido);
        acta.setGoles_local(0);
        acta.setGoles_visitante(0);
        List<Evento> eventos = new ArrayList<>();
        acta.setEventos(eventos);

        entityManager.persist(acta);*/

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode mensaje = mapper.createObjectNode(); 

            mensaje.put("tipo", "FIN_PARTIDO");
            mensaje.put("partidoId", id);
            
            messagingTemplate.convertAndSend("/topic/partido/" + id, mapper.writeValueAsString(mensaje));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Map.of("status", "ok");
    }

       @PostMapping("/{id}/evento")
    @Transactional
    @ResponseBody 
    public Map<String, String> registrarEvento(@PathVariable long id, @RequestParam String tipo, @RequestParam long equipoid,
            @RequestParam long jugadorid, @RequestParam int minuto, @RequestParam(required = false) String descripcion, HttpSession session) {

        User u = (User) session.getAttribute("u");
        if (u == null || !u.hasRole(User.Role.ARBITRO)) {
            return Map.of("status", "error", "message", "No autorizado");
        }

        Partido partido = entityManager.find(Partido.class, id);
        if (partido == null || partido.getEstado() != Partido.State.EN_CURSO) {
            return Map.of("status", "error", "message", "Partido no disponible");
        }

        Equipo equipo = entityManager.find(Equipo.class, equipoid);
        User jugador = entityManager.find(User.class, jugadorid);

        if (equipo == null || jugador == null) {
            return Map.of("status", "error", "message", "Datos inválidos");
        }

        Acta acta = entityManager.createQuery("SELECT a FROM Acta a WHERE a.partido.id = :id", Acta.class)
            .setParameter("id", id)
            .getSingleResult();

        Evento evento = new Evento();
        evento.setTipo(Evento.Tipo.valueOf(tipo));
        evento.setEquipo(equipo);
        evento.setUsuario(jugador);
        evento.setMinuto(minuto);
        evento.setDescripcion(descripcion);
        evento.setTimestamp(java.time.LocalDateTime.now());
        evento.setActa(acta);

        acta.getEventos().add(evento);
        entityManager.persist(evento);

        if (evento.getTipo() == Evento.Tipo.GOL) {
            if (equipoid == partido.getLocal().getId()) {
                acta.setGoles_local(acta.getGoles_local() + 1);
            } else if (equipoid == partido.getVisitante().getId()) {
                acta.setGoles_visitante(acta.getGoles_visitante() + 1);
            }

            int goles = jugador.getGoles();
            jugador.setGoles(goles + 1);

        }

        if(evento.getTipo() == Evento.Tipo.TARJETA_AMARILLA) {
            int n_tarjetas = jugador.getTarjetasAmarillas();
            jugador.setTarjetasAmarillas(n_tarjetas + 1);
        }

        if(evento.getTipo() == Evento.Tipo.TARJETA_ROJA) {
            int n_tarjetas = jugador.getTarjetasRojas();
            jugador.setTarjetasAmarillas(n_tarjetas + 1);
        }

        entityManager.merge(jugador);
        entityManager.merge(acta);   

            // Websocket live score update
            try {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode mensaje = mapper.createObjectNode();

                String nombre_jug = jugador.getFirstName() + ' ' + jugador.getLastName();

                mensaje.put("tipo", "NUEVO_EVENTO");
                mensaje.put("partidoId", id);
                mensaje.put("golesLocal", acta.getGoles_local());
                mensaje.put("golesVisitante", acta.getGoles_visitante());
                mensaje.put("minuto", minuto);
                mensaje.put("tipoEvento", tipo);
                mensaje.put("jugador", nombre_jug);
                mensaje.put("equipo", equipo.getNombre());
                mensaje.put("descripcion", descripcion);

                messagingTemplate.convertAndSend("/topic/partido/" + id, mapper.writeValueAsString(mensaje));
            } catch (Exception e) {
                e.printStackTrace();
            }
       // }

        return Map.of("status", "ok");
    }

}
