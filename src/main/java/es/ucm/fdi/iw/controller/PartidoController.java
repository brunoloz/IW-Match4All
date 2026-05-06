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
import es.ucm.fdi.iw.model.Clasificacion;
import es.ucm.fdi.iw.model.Competicion;
import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.EstadisticasJugador;
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
    
    //para websocket
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

        if(partido.getCompeticion().getTipo() == Competicion.Tipo.LIGA)
            updateClasificacion(partido, null, 0, 0);

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
            messagingTemplate.convertAndSend("/topic/competicion/" + partido.getCompeticion().getId(), mapper.writeValueAsString(mensaje));

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

        // Si es competición tipo TORNEO o ROUND_ROBIN_ARBOL y pertenece a un bracket,
        // enlazamos el ganador con el siguiente partido en la siguiente ronda.
        try {
            Acta acta = entityManager.createQuery("SELECT a FROM Acta a WHERE a.partido.id = :id", Acta.class)
                    .setParameter("id", id)
                    .getResultList()
                    .stream()
                    .findFirst()
                    .orElse(null);

            Competicion comp = partido.getCompeticion();
            if (acta != null && (comp.getTipo() == Competicion.Tipo.TORNEO || comp.getTipo() == Competicion.Tipo.ROUND_ROBIN_ARBOL)) {
                String fase = partido.getFase();
                if (fase != null && fase.toUpperCase().startsWith("BRACKET - RONDA ")) {
                    // extraer número de ronda
                    int ronda = 1;
                    try {
                        String suf = fase.substring("BRACKET - RONDA ".length()).trim();
                        String[] parts = suf.split("\\s+", 2);
                        ronda = Integer.parseInt(parts[0]);
                    } catch (Exception e) {
                        // si no se puede parsear, no hacemos nada
                        ronda = -1;
                    }

                    if (ronda > 0) {
                        String nextFase = "BRACKET - RONDA " + (ronda + 1);

                        List<Partido> matchesThisRound = entityManager.createQuery(
                                "SELECT p FROM Partido p WHERE p.competicion.id = :compId AND p.fase = :fase ORDER BY p.id ASC", Partido.class)
                                .setParameter("compId", comp.getId())
                                .setParameter("fase", fase)
                                .getResultList();

                        int idx = -1;
                        for (int i = 0; i < matchesThisRound.size(); i++) {
                            if (matchesThisRound.get(i).getId() == partido.getId()) {
                                idx = i;
                                break;
                            }
                        }

                        if (idx >= 0) {
                            int targetIndex = idx / 2;

                                // determinar ganador (si hay empate, no enlazamos automáticamente)
                                Equipo ganador = null;
                                if (acta.getGoles_local() > acta.getGoles_visitante()) {
                                    ganador = partido.getLocal();
                                } else if (acta.getGoles_visitante() > acta.getGoles_local()) {
                                    ganador = partido.getVisitante();
                                }

                                // Si esta ronda tiene un solo partido, es la final: no crear partido siguiente.
                                if (matchesThisRound.size() == 1) {
                                    if (ganador != null) {
                                        comp.setEstado(Competicion.Estado.FINALIZADA);
                                        entityManager.merge(comp);
                                    }
                                } else {
                                    List<Partido> nextMatches = entityManager.createQuery(
                                            "SELECT p FROM Partido p WHERE p.competicion.id = :compId AND p.fase = :fase ORDER BY p.id ASC", Partido.class)
                                            .setParameter("compId", comp.getId())
                                            .setParameter("fase", nextFase)
                                            .getResultList();

                                    Partido target;
                                    if (nextMatches.size() > targetIndex) {
                                        target = nextMatches.get(targetIndex);
                                    } else {
                                        // crear partido vacío en la siguiente ronda con equipos "Pendiente"
                                        Equipo placeholder = getOrCreatePlaceholderEquipo(comp);
                                        target = new Partido();
                                        target.setCompeticion(comp);
                                        target.setFase(nextFase);
                                        target.setFecha(partido.getFecha().plusDays(1 + targetIndex));
                                        target.setEstado(Partido.State.PENDIENTE);
                                        target.setUbicacion("Por determinar");
                                        // Asignamos placeholder en ambos lados; luego sobrescribiremos el correspondiente
                                        target.setLocal(placeholder);
                                        target.setVisitante(placeholder);
                                        entityManager.persist(target);
                                    }

                                    if (ganador != null) {
                                        if (idx % 2 == 0) {
                                            target.setLocal(ganador);
                                        } else {
                                            target.setVisitante(ganador);
                                        }
                                        entityManager.merge(target);

                                        // notificar actualización de bracket
                                        try {
                                            ObjectMapper mapper = new ObjectMapper();
                                            ObjectNode msg = mapper.createObjectNode();
                                            msg.put("tipo", "UPDATE_BRACKET");
                                            msg.put("partidoId", target.getId());
                                            msg.put("equipoId", ganador.getId());
                                            msg.put("lado", (idx % 2 == 0) ? "local" : "visitante");
                                            messagingTemplate.convertAndSend("/topic/competicion/" + comp.getId(), mapper.writeValueAsString(msg));
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Competicion comp = partido.getCompeticion();
        Long partidosPendientes = entityManager.createQuery(
            "SELECT COUNT(p) FROM Partido p " +
            "WHERE p.competicion.id = :compId " +
            "AND p.estado != :estadoFinalizado", Long.class)
            .setParameter("compId", comp.getId())
            .setParameter("estadoFinalizado", Partido.State.FINALIZADO)
            .getSingleResult();

            if (partidosPendientes == 0) {
                comp.setEstado(Competicion.Estado.FINALIZADA);
            }

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode mensaje = mapper.createObjectNode(); 

            mensaje.put("tipo", "FIN_PARTIDO");
            mensaje.put("partidoId", id);
            
            messagingTemplate.convertAndSend("/topic/partido/" + id, mapper.writeValueAsString(mensaje));
            messagingTemplate.convertAndSend("/topic/competicion/" + partido.getCompeticion().getId(), mapper.writeValueAsString(mensaje));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Map.of("status", "ok");
    }

    @PostMapping("/{id}/evento")
    @Transactional
    @ResponseBody 
    public Map<String, String> registrarEvento(@PathVariable long id, @RequestParam String tipo, @RequestParam long equipoid,
            @RequestParam long jugadorid, @RequestParam int minuto, @RequestParam(required = false) String descripcion, 
            @RequestParam(required = false) Long asistenteid,
            HttpSession session) {

        User u = (User) session.getAttribute("u");
        if (u == null || !u.hasRole(User.Role.ARBITRO)) {
            return Map.of("status", "error", "message", "No autorizado");
        }

        if(minuto < 1 || minuto > 120){
            return Map.of("status", "error", "message", "El minuto debe ser un valor válido (entre 1 y 120)");
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

        User asistente = null;
        if (asistenteid != null) {
            asistente = entityManager.find(User.class, asistenteid);
        }

        Acta acta = entityManager.createQuery("SELECT a FROM Acta a WHERE a.partido.id = :id", Acta.class)
            .setParameter("id", id)
            .getSingleResult();

        Evento evento = new Evento();
        evento.setTipo(Evento.Tipo.valueOf(tipo));
        evento.setEquipo(equipo);
        evento.setUsuario(jugador);
        evento.setAsistente(asistente);
        evento.setMinuto(minuto);
        evento.setDescripcion(descripcion);
        evento.setTimestamp(java.time.LocalDateTime.now());
        evento.setActa(acta);

        acta.getEventos().add(evento);
        entityManager.persist(evento);

        Competicion comp = partido.getCompeticion();
        EstadisticasJugador statsJugador = getOrCreateEstadisticas(comp, jugador);

        if (evento.getTipo() == Evento.Tipo.GOL) {
            long golesLocalAntes = acta.getGoles_local();
            long golesVisitAntes = acta.getGoles_visitante();

            if (equipoid == partido.getLocal().getId()) {
                acta.setGoles_local(acta.getGoles_local() + 1);
            } else if (equipoid == partido.getVisitante().getId()) {
                acta.setGoles_visitante(acta.getGoles_visitante() + 1);
            }

            // Goles Globales
            jugador.setGoles(jugador.getGoles() + 1);
            // Goles en la Competición
            statsJugador.setGoles(statsJugador.getGoles() + 1);

            // Asistencias
            if (asistente != null) {
                asistente.setAsistencias(asistente.getAsistencias() + 1); // Global
                EstadisticasJugador statsAsistente = getOrCreateEstadisticas(comp, asistente);
                statsAsistente.setAsistencias(statsAsistente.getAsistencias() + 1); // En competición
                entityManager.merge(statsAsistente);
                entityManager.merge(asistente);
            }

            if(comp.getTipo() == Competicion.Tipo.LIGA) {
                updateClasificacion(partido, acta, golesLocalAntes, golesVisitAntes);
            }
        }
        else if(evento.getTipo() == Evento.Tipo.TARJETA_AMARILLA) {
            jugador.setTarjetasAmarillas(jugador.getTarjetasAmarillas() + 1); // Global
            statsJugador.setTarjetasAmarillas(statsJugador.getTarjetasAmarillas() + 1);
        }
        else if(evento.getTipo() == Evento.Tipo.TARJETA_ROJA) {
            jugador.setTarjetasRojas(jugador.getTarjetasRojas() + 1); // Global
            statsJugador.setTarjetasRojas(statsJugador.getTarjetasRojas() + 1);
        }

        entityManager.merge(jugador);
        entityManager.merge(statsJugador);
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

            ObjectNode mensajeStats = mapper.createObjectNode();
            mensajeStats.put("tipo", "UPDATE_ESTADISTICAS");
            messagingTemplate.convertAndSend("/topic/competicion/" + comp.getId(), mapper.writeValueAsString(mensajeStats));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Map.of("status", "ok");
    }

    private void updateClasificacion(Partido partido, Acta acta, long golesLocalAntes, long golesVisitAntes){


        long id_comp = partido.getCompeticion().getId();

        Clasificacion fila_local = entityManager.createQuery(
            "SELECT c FROM Clasificacion c WHERE c.competicion.id = :cId AND c.equipo.id = :eId", Clasificacion.class)
            .setParameter("cId", id_comp)
            .setParameter("eId", partido.getLocal().getId())
            .getSingleResult();

        Clasificacion fila_visitante = entityManager.createQuery(
            "SELECT c FROM Clasificacion c WHERE c.competicion.id = :cId AND c.equipo.id = :eId", Clasificacion.class)
            .setParameter("cId", id_comp)
            .setParameter("eId", partido.getVisitante().getId())
            .getSingleResult();

        if(acta == null) {

            int pj_local = fila_local.getPartidos_jugados();
            fila_local.setPartidos_jugados(pj_local + 1);
            fila_local.setEmpates(fila_local.getEmpates() + 1);
            fila_local.setPuntos(fila_local.getPuntos() + 1);

            int pj_visitante = fila_visitante.getPartidos_jugados();
            fila_visitante.setPartidos_jugados(pj_visitante + 1);
            fila_visitante.setEmpates(fila_visitante.getEmpates() + 1);
            fila_visitante.setPuntos(fila_visitante.getPuntos() + 1);

        }
        else {

            int pgL_ant = (golesLocalAntes > golesVisitAntes) ? 1 : 0;
            int peL_ant = (golesLocalAntes == golesVisitAntes) ? 1 : 0;
            int ppL_ant = (golesLocalAntes < golesVisitAntes) ? 1 : 0;

            int pgV_ant = (golesVisitAntes > golesLocalAntes) ? 1 : 0;
            int peV_ant = (golesVisitAntes == golesLocalAntes) ? 1 : 0;
            int ppV_ant = (golesVisitAntes < golesLocalAntes) ? 1 : 0;

            int pgL_ahora = (acta.getGoles_local() > acta.getGoles_visitante()) ? 1 : 0;
            int peL_ahora = (acta.getGoles_local() == acta.getGoles_visitante()) ? 1 : 0;
            int ppL_ahora = (acta.getGoles_local() < acta.getGoles_visitante()) ? 1 : 0;

            int pgV_ahora = (acta.getGoles_visitante() > acta.getGoles_local()) ? 1 : 0;
            int peV_ahora = (acta.getGoles_visitante() == acta.getGoles_local()) ? 1 : 0;
            int ppV_ahora = (acta.getGoles_visitante() < acta.getGoles_local()) ? 1 : 0;

            fila_local.setVictorias(fila_local.getVictorias() - pgL_ant + pgL_ahora);
            fila_local.setEmpates(fila_local.getEmpates() - peL_ant + peL_ahora);
            fila_local.setDerrotas(fila_local.getDerrotas() - ppL_ant + ppL_ahora);

            fila_visitante.setVictorias(fila_visitante.getVictorias() - pgV_ant + pgV_ahora);
            fila_visitante.setEmpates(fila_visitante.getEmpates() - peV_ant + peV_ahora);
            fila_visitante.setDerrotas(fila_visitante.getDerrotas() - ppV_ant + ppV_ahora);

            if(golesLocalAntes < acta.getGoles_local()){
                
                fila_local.setGoles_a_favor(fila_local.getGoles_a_favor() + 1);
                fila_visitante.setGoles_en_contra(fila_visitante.getGoles_en_contra() + 1);

            }

            if(golesVisitAntes < acta.getGoles_visitante()){

                fila_visitante.setGoles_a_favor(fila_visitante.getGoles_a_favor() + 1);
                fila_local.setGoles_en_contra(fila_local.getGoles_en_contra() + 1);

            }

            int pts_local_antes;
            if(golesLocalAntes > golesVisitAntes) pts_local_antes = 3;
            else if(golesLocalAntes == golesVisitAntes) pts_local_antes = 1;
            else pts_local_antes = 0;

            int pts_visitante_antes;
            if(golesLocalAntes < golesVisitAntes) pts_visitante_antes = 3;
            else if(golesLocalAntes == golesVisitAntes) pts_visitante_antes = 1;
            else pts_visitante_antes = 0;

            int pts_local;
            if(acta.getGoles_local() > acta.getGoles_visitante()) pts_local = 3;
            else if(acta.getGoles_local() == acta.getGoles_visitante()) pts_local = 1;
            else pts_local = 0;

            int pts_visitante;
            if(acta.getGoles_local() < acta.getGoles_visitante()) pts_visitante = 3;
            else if(acta.getGoles_local() == acta.getGoles_visitante()) pts_visitante = 1;
            else pts_visitante = 0;

            fila_local.setPuntos(fila_local.getPuntos() - pts_local_antes + pts_local);
            fila_visitante.setPuntos(fila_visitante.getPuntos() - pts_visitante_antes + pts_visitante);

        } 

        try {

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode mensaje = mapper.createObjectNode(); 

            mensaje.put("tipo", "UPDATE_CLASIFICACION");
            mensaje.put("id_local", fila_local.getEquipo().getId());
            mensaje.put("pj_local", fila_local.getPartidos_jugados());
            mensaje.put("pts_local", fila_local.getPuntos());
            mensaje.put("gf_local", fila_local.getGoles_a_favor());
            mensaje.put("gc_local", fila_local.getGoles_en_contra());
            mensaje.put("id_visitante", fila_visitante.getEquipo().getId());
            mensaje.put("pj_visitante", fila_visitante.getPartidos_jugados());
            mensaje.put("pts_visitante", fila_visitante.getPuntos());
            mensaje.put("gf_visitante", fila_visitante.getGoles_a_favor());
            mensaje.put("gc_visitante", fila_visitante.getGoles_en_contra());
            mensaje.put("pg_local", fila_local.getVictorias());
            mensaje.put("pg_visitante", fila_visitante.getVictorias());
            mensaje.put("pe_local", fila_local.getEmpates());
            mensaje.put("pe_visitante", fila_visitante.getEmpates());
            mensaje.put("pp_local", fila_local.getDerrotas());
            mensaje.put("pp_visitante", fila_visitante.getDerrotas());
            
            messagingTemplate.convertAndSend("/topic/competicion/" + id_comp, mapper.writeValueAsString(mensaje));


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private EstadisticasJugador getOrCreateEstadisticas(Competicion comp, User jugador) {
        List<EstadisticasJugador> stats = entityManager.createQuery(
            "SELECT e FROM EstadisticasJugador e WHERE e.competicion.id = :cId AND e.jugador.id = :jId", EstadisticasJugador.class)
            .setParameter("cId", comp.getId())
            .setParameter("jId", jugador.getId())
            .getResultList();

        if (stats.isEmpty()) {
            EstadisticasJugador stat = new EstadisticasJugador();
            stat.setCompeticion(comp);
            stat.setJugador(jugador);
            entityManager.persist(stat);
            return stat;
        }
        return stats.get(0);
    }

    private Equipo getOrCreatePlaceholderEquipo(Competicion comp) {
        String nombre = "PENDIENTE";
        List<Equipo> existentes = entityManager.createQuery("SELECT e FROM Equipo e WHERE e.nombre = :nombre", Equipo.class)
                .setParameter("nombre", nombre)
                .getResultList();
        if (!existentes.isEmpty()) return existentes.get(0);

        Equipo e = new Equipo();
        e.setNombre(nombre);
        e.setDescripcion("Equipo marcador de posición");
        e.setUbicacion("Por determinar");
        entityManager.persist(e);
        return e;
    }

}
