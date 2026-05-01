package es.ucm.fdi.iw.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.ucm.fdi.iw.model.Clasificacion;
import es.ucm.fdi.iw.model.Competicion;
import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.Partido;
import es.ucm.fdi.iw.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;


@Controller
@RequestMapping("competicion")
public class CompeticionController {

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
    public String competicion(@PathVariable("id") long id, Model model) {
        Competicion competicion = entityManager.find(Competicion.class, id);
        List<Clasificacion> clasificacion = entityManager.createQuery("SELECT c FROM Clasificacion c WHERE c.competicion.id = :id ORDER BY c.puntos DESC", Clasificacion.class)
        .setParameter("id", id)
        .getResultList()
        ;
        model.addAttribute("clasificacion", clasificacion);
        model.addAttribute("competicionSeleccionada", competicion);

        List<Partido> partidos = entityManager.createQuery("SELECT p FROM Partido p WHERE p.competicion.id = :idCompeticion ORDER BY p.fecha ASC", Partido.class)
        .setParameter("idCompeticion", id)
        .getResultList();
        model.addAttribute("partidos", partidos);

        List<Partido> partidosRoundRobin = new ArrayList<>();
        Map<String, List<Partido>> partidosRoundRobinPorGrupo = new LinkedHashMap<>();
        Map<String, Map<Long, Equipo>> equiposRoundRobinTemp = new LinkedHashMap<>();
        Map<String, List<Partido>> bracketPartidosPorFase = new LinkedHashMap<>();

        if (competicion != null) {
            for (Partido partido : partidos) {
                boolean faseRoundRobin = esFaseRoundRobin(partido.getFase());
                if (competicion.getTipo() == Competicion.Tipo.ROUND_ROBIN_ARBOL && faseRoundRobin) {
                    partidosRoundRobin.add(partido);

                    String grupo = extraerNombreGrupo(partido.getFase());
                    if (grupo != null) {
                        partidosRoundRobinPorGrupo.computeIfAbsent(grupo, k -> new ArrayList<>()).add(partido);

                        equiposRoundRobinTemp.computeIfAbsent(grupo, k -> new LinkedHashMap<>());
                        Map<Long, Equipo> equiposGrupo = equiposRoundRobinTemp.get(grupo);
                        equiposGrupo.put(partido.getLocal().getId(), partido.getLocal());
                        equiposGrupo.put(partido.getVisitante().getId(), partido.getVisitante());
                    }
                } else if (competicion.getTipo() == Competicion.Tipo.ROUND_ROBIN_ARBOL
                        || competicion.getTipo() == Competicion.Tipo.TORNEO) {
                    bracketPartidosPorFase.computeIfAbsent(partido.getFase(), k -> new ArrayList<>()).add(partido);
                }
            }
        }

        Map<String, List<Equipo>> equiposRoundRobinPorGrupo = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Long, Equipo>> entry : equiposRoundRobinTemp.entrySet()) {
            List<Equipo> equiposGrupo = new ArrayList<>(entry.getValue().values());
            equiposGrupo.sort(Comparator.comparingLong(Equipo::getId));
            equiposRoundRobinPorGrupo.put(entry.getKey(), equiposGrupo);
        }

        Map<String, Map<Long, Map<Long, Partido>>> matrizRoundRobinPorGrupo = new LinkedHashMap<>();
        for (Map.Entry<String, List<Equipo>> entry : equiposRoundRobinPorGrupo.entrySet()) {
            String grupo = entry.getKey();
            List<Equipo> equiposGrupo = entry.getValue();

            Map<Long, Map<Long, Partido>> matrizGrupo = new LinkedHashMap<>();
            for (Equipo equipo : equiposGrupo) {
                matrizGrupo.put(equipo.getId(), new LinkedHashMap<>());
            }

            List<Partido> partidosGrupo = partidosRoundRobinPorGrupo.getOrDefault(grupo, new ArrayList<>());
            for (Partido partido : partidosGrupo) {
                long idA = Math.min(partido.getLocal().getId(), partido.getVisitante().getId());
                long idB = Math.max(partido.getLocal().getId(), partido.getVisitante().getId());

                matrizGrupo.computeIfAbsent(idA, k -> new LinkedHashMap<>()).put(idB, partido);
            }

            matrizRoundRobinPorGrupo.put(grupo, matrizGrupo);
        }

        model.addAttribute("partidosRoundRobin", partidosRoundRobin);
        model.addAttribute("partidosRoundRobinPorGrupo", partidosRoundRobinPorGrupo);
        model.addAttribute("equiposRoundRobinPorGrupo", equiposRoundRobinPorGrupo);
        model.addAttribute("matrizRoundRobinPorGrupo", matrizRoundRobinPorGrupo);
        model.addAttribute("bracketPartidosPorFase", bracketPartidosPorFase);


        return "competiciones";
    }

    private boolean esFaseRoundRobin(String fase) {
        return fase != null && fase.toUpperCase(Locale.ROOT).startsWith("ROUND ROBIN");
    }

    @PostMapping("/solicitar")
    @Transactional
    public String solicitarCompeticion(@RequestParam("idCompeticion") long idCompeticion, HttpSession session, RedirectAttributes redir) {
        User sessionUser = (User) session.getAttribute("u");
        if (sessionUser == null) return "redirect:/login";

        User capitan = entityManager.find(User.class, sessionUser.getId());
        Equipo equipo = capitan.getEquipo();

        if (equipo == null || equipo.getCapitan().getId() != capitan.getId()) {
            redir.addFlashAttribute("error", "Solo los capitanes pueden inscribir al equipo en una competición.");
            return "redirect:/listacompeticiones";
        }

        Competicion comp = entityManager.find(Competicion.class, idCompeticion);
        // Solo permite inscripciones si el estado es INSCRIPCION
        if (comp.getEstado() != Competicion.Estado.INSCRIPCION) {
            redir.addFlashAttribute("error", "La competición no está abierta a inscripciones.");
            return "redirect:/listacompeticiones";
        }
        if (comp.getEquipos().contains(equipo)) {
            redir.addFlashAttribute("error", "Tu equipo ya está inscrito en esta competición.");
            return "redirect:/listacompeticiones";
        }
        // Evita duplicados en solicitudes
        if (comp.getEquiposSolicitantes().contains(equipo)) {
            redir.addFlashAttribute("error", "Tu equipo ya ha solicitado unirse a esta competición.");
            return "redirect:/listacompeticiones";
        }

        if (comp.getEquiposSolicitantes().size() == (comp.getCapacidad() - comp.getEquipos().size())) {
            redir.addFlashAttribute("error", "No se ha podido hacer la solicitud. El buzón de solicitudes está lleno");
            return "redirect:/listacompeticiones";
        }

        // Evita que un equipo solicite varias a la vez
        for (Competicion c : entityManager.createQuery("SELECT c FROM Competicion c", Competicion.class).getResultList()) {
            if (c.getEquiposSolicitantes().contains(equipo)) {
                redir.addFlashAttribute("error", "Tu equipo ya tiene una solicitud pendiente para otra competición.");
                return "redirect:/listacompeticiones";
            }
        }
        comp.getEquiposSolicitantes().add(equipo);
        entityManager.merge(comp);
        redir.addFlashAttribute("success", "Solicitud de inscripción enviada a " + comp.getNombre());
        return "redirect:/listacompeticiones";
    }

    @PostMapping("/aceptar")
    @Transactional
    public String aceptarEquipoCompeticion(@RequestParam("idCompeticion") long idCompeticion, @RequestParam("idEquipo") long idEquipo, HttpSession session, RedirectAttributes redir) {
        User admin = (User) session.getAttribute("u");
        if (admin == null || !admin.hasRole(User.Role.ADMIN)) return "redirect:/paneladmin";

        Competicion comp = entityManager.find(Competicion.class, idCompeticion);
        Equipo eq = entityManager.find(Equipo.class, idEquipo);

        if (comp != null && eq != null && comp.getEquiposSolicitantes().contains(eq)) {
            // Solo permite aceptar si el estado es INSCRIPCION
            if (comp.getEstado() != Competicion.Estado.INSCRIPCION) {
                redir.addFlashAttribute("error", "No se pueden aceptar equipos, la competición no está en fase de inscripción.");
                return "redirect:/paneladmin";
            }
            comp.getEquiposSolicitantes().remove(eq);
            comp.getEquipos().add(eq);
            entityManager.merge(comp);
            redir.addFlashAttribute("success", eq.getNombre() + " ha sido aceptado en " + comp.getNombre());

            if (comp.getTipo().name().equals("LIGA") || comp.getTipo().name().equals("ROUND_ROBIN_ARBOL")) {
                Clasificacion nuevaClasificacion = new Clasificacion();
                nuevaClasificacion.setCompeticion(comp);
                nuevaClasificacion.setEquipo(eq);
                nuevaClasificacion.setPuntos(0);
                nuevaClasificacion.setPartidos_jugados(0);
                nuevaClasificacion.setVictorias(0);
                nuevaClasificacion.setEmpates(0);
                nuevaClasificacion.setDerrotas(0);
                nuevaClasificacion.setGoles_a_favor(0);
                nuevaClasificacion.setGoles_en_contra(0);
                entityManager.persist(nuevaClasificacion);
            }
        }

        return "redirect:/paneladmin";
    }

    @PostMapping("/rechazar")
    @Transactional
    public String rechazarEquipoCompeticion(@RequestParam("idCompeticion") long idCompeticion, @RequestParam("idEquipo") long idEquipo, HttpSession session, RedirectAttributes redir) {
        User admin = (User) session.getAttribute("u");
        if (admin == null || !admin.hasRole(User.Role.ADMIN)) return "redirect:/paneladmin";

        Competicion comp = entityManager.find(Competicion.class, idCompeticion);
        Equipo eq = entityManager.find(Equipo.class, idEquipo);

        if (comp != null && eq != null && comp.getEquiposSolicitantes().contains(eq)) {
            comp.getEquiposSolicitantes().remove(eq);
            entityManager.merge(comp);
            redir.addFlashAttribute("success", "Solicitud de " + eq.getNombre() + " rechazada.");
        }
        return "redirect:/paneladmin";
    }
        
    @PostMapping("/generar-calendario")
    @Transactional
    public String generarCalendario(@RequestParam("idCompeticion") long idCompeticion, HttpSession session, RedirectAttributes redir) {
        User admin = (User) session.getAttribute("u");
        if (admin == null || !admin.hasRole(User.Role.ADMIN)) return "redirect:/paneladmin";

        Competicion comp = entityManager.find(Competicion.class, idCompeticion);
        if (comp == null) {
            redir.addFlashAttribute("error", "No se ha encontrado la competición.");
            return "redirect:/paneladmin";
        }

        // Solo permite generar calendario si el estado es INSCRIPCION
        if (comp.getEstado() != Competicion.Estado.INSCRIPCION) {
            redir.addFlashAttribute("error", "Solo se puede generar el calendario cuando la competición está en fase de inscripción.");
            return "redirect:/paneladmin";
        }
        Long partidosExistentes = entityManager
                .createQuery("SELECT COUNT(p) FROM Partido p WHERE p.competicion.id = :idCompeticion", Long.class)
                .setParameter("idCompeticion", idCompeticion)
                .getSingleResult();
        if (partidosExistentes != null && partidosExistentes > 0) {
            redir.addFlashAttribute("error", "Esta competición ya tiene partidos generados.");
            return "redirect:/paneladmin";
        }

        List<Equipo> equipos = new ArrayList<>(comp.getEquipos());

        equipos.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        LocalDate fechaInicio = LocalDate.now();
        String successMessage;

        switch (comp.getTipo()) {
            case LIGA -> {
                generarRoundRobin(comp, equipos, fechaInicio, "", true);
                successMessage = "Calendario generado con éxito. La competición ha comenzado.";
            }
            case ROUND_ROBIN_ARBOL -> {
                Integer equiposPorGrupo = comp.getEquiposPorGrupo();
                Integer equiposClasificanArbol = comp.getEquiposClasificanArbol();

                List<List<Equipo>> grupos = repartirEquiposEnGrupos(equipos, equiposPorGrupo);
                int numeroGrupos = grupos.size();

                for (int i = 0; i < numeroGrupos; i++) {
                    List<Equipo> grupo = grupos.get(i);
                    String fasePrefix = "ROUND ROBIN - GRUPO " + getNombreGrupo(i) + " -";
                    generarRoundRobin(comp, grupo, fechaInicio, fasePrefix, false);
                }
                successMessage = "Fase Round Robin generada con éxito. La fase de árbol podrá generarse después.";
            }
            case TORNEO -> {
                generarPrimeraRondaBracket(comp, equipos, fechaInicio, "BRACKET - RONDA 1");
                successMessage = "Bracket de torneo generado con éxito (Ronda 1).";
            }
            default -> throw new IllegalStateException("Tipo de competición no soportado: " + comp.getTipo());
        }

        comp.setEstado(Competicion.Estado.EN_CURSO);
        entityManager.merge(comp);
        redir.addFlashAttribute("success", successMessage);
        return "redirect:/paneladmin";
    }

    @PostMapping("/debug-finalizar-round-robin")
    @Transactional
    public String debugFinalizarRoundRobin(@RequestParam("idCompeticion") long idCompeticion, HttpSession session,
            RedirectAttributes redir) {
        User admin = (User) session.getAttribute("u");
        if (admin == null || !admin.hasRole(User.Role.ADMIN)) return "redirect:/paneladmin";

        Competicion comp = entityManager.find(Competicion.class, idCompeticion);
        if (comp == null) {
            redir.addFlashAttribute("error", "No se ha encontrado la competición.");
            return "redirect:/competiciones/" + idCompeticion;
        }
        if (comp.getTipo() != Competicion.Tipo.ROUND_ROBIN_ARBOL) {
            redir.addFlashAttribute("error", "Este botón debug solo aplica a Round Robin + Árbol.");
            return "redirect:/competiciones/" + idCompeticion;
        }

        List<Partido> partidosRoundRobin = entityManager
                .createQuery("SELECT p FROM Partido p WHERE p.competicion.id = :idCompeticion AND p.fase LIKE :fase ORDER BY p.fecha ASC", Partido.class)
                .setParameter("idCompeticion", idCompeticion)
                .setParameter("fase", "ROUND ROBIN -%")
                .getResultList();

        if (partidosRoundRobin.isEmpty()) {
            redir.addFlashAttribute("error", "No hay partidos de Round Robin para finalizar.");
            return "redirect:/competiciones/" + idCompeticion;
        }

        for (Partido partido : partidosRoundRobin) {
            partido.setEstado(Partido.State.FINALIZADO);
        }

        Long bracketExistente = entityManager
                .createQuery("SELECT COUNT(p) FROM Partido p WHERE p.competicion.id = :idCompeticion AND p.fase LIKE :fase", Long.class)
                .setParameter("idCompeticion", idCompeticion)
                .setParameter("fase", "BRACKET -%")
                .getSingleResult();

        if (bracketExistente == null || bracketExistente == 0) {
            Integer equiposClasificanArbol = comp.getEquiposClasificanArbol();
            if (equiposClasificanArbol == null || equiposClasificanArbol < 1) {
                redir.addFlashAttribute("error", "La competición no tiene configurados equiposClasificanArbol válidos.");
                return "redirect:/competiciones/" + idCompeticion;
            }

            List<Equipo> clasificados = obtenerClasificadosDebug(partidosRoundRobin, equiposClasificanArbol);
            if (clasificados.size() < 2 || !esPotenciaDeDos(clasificados.size())) {
                redir.addFlashAttribute("error", "No se puede generar el árbol: número de clasificados inválido.");
                return "redirect:/competiciones/" + idCompeticion;
            }

            LocalDate fechaArbol = partidosRoundRobin.stream()
                    .map(Partido::getFecha)
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now())
                    .plusWeeks(1);

            generarPrimeraRondaBracket(comp, clasificados, fechaArbol, "BRACKET - RONDA 1");
        }

        redir.addFlashAttribute("success", "Debug aplicado: Round Robin finalizado y fase de árbol disponible.");
        return "redirect:/competiciones/" + idCompeticion;
    }

    private void generarRoundRobin(Competicion comp, List<Equipo> equipos, LocalDate fechaInicio, String fasePrefix, boolean idaYVuelta) {
        List<Equipo> rotacion = new ArrayList<>(equipos);
        if (rotacion.size() % 2 != 0) {
            rotacion.add(null);
        }

        int numEquipos = rotacion.size();
        int numJornadasIda = numEquipos - 1;
        int numPartidosPorJornada = numEquipos / 2;

        for (int i = 0; i < numJornadasIda; i++) {
            for (int j = 0; j < numPartidosPorJornada; j++) {
                Equipo equipoA = rotacion.get(j);
                Equipo equipoB = rotacion.get(numEquipos - 1 - j);

                if (equipoA == null || equipoB == null) {
                    continue;
                }

                Equipo local = equipoA;
                Equipo visitante = equipoB;
                if (j == 0 && i % 2 == 1) {
                    local = equipoB;
                    visitante = equipoA;
                }

                String faseIda = (fasePrefix == null || fasePrefix.isBlank())
                        ? "JORNADA " + (i + 1)
                        : fasePrefix + " JORNADA " + (i + 1);
                persistirPartido(comp, local, visitante, faseIda, fechaInicio.plusWeeks(i));

                if (idaYVuelta) {
                    int jornadaVuelta = i + 1 + numJornadasIda;
                    String faseVuelta = (fasePrefix == null || fasePrefix.isBlank())
                            ? "JORNADA " + jornadaVuelta
                            : fasePrefix + " JORNADA " + jornadaVuelta;
                    persistirPartido(comp, visitante, local, faseVuelta, fechaInicio.plusWeeks(jornadaVuelta - 1));
                }
            }

            Equipo ultimo = rotacion.remove(rotacion.size() - 1);
            rotacion.add(1, ultimo);
        }
    }

    private void persistirPartido(Competicion comp, Equipo local, Equipo visitante, String fase, LocalDate fecha) {
        Partido partido = new Partido();
        partido.setCompeticion(comp);
        partido.setFase(fase);
        partido.setFecha(fecha);
        partido.setEstado(Partido.State.PENDIENTE);
        partido.setLocal(local);
        partido.setVisitante(visitante);

        String ubicacion = local.getUbicacion();
        if (ubicacion == null || ubicacion.isBlank()) {
            ubicacion = "Por determinar";
        }
        partido.setUbicacion(ubicacion);

        entityManager.persist(partido);
    }

    private List<List<Equipo>> repartirEquiposEnGrupos(List<Equipo> equipos, int equiposPorGrupoObjetivo) {
        List<List<Equipo>> grupos = new ArrayList<>();

        int totalEquipos = equipos.size();
        int numeroGrupos = Math.max(1, totalEquipos / equiposPorGrupoObjetivo);
        int tamBase = totalEquipos / numeroGrupos;
        int gruposConUnoExtra = totalEquipos % numeroGrupos;

        int indiceInicio = 0;
        for (int i = 0; i < numeroGrupos; i++) {
            int tamGrupo = tamBase + (i >= numeroGrupos - gruposConUnoExtra ? 1 : 0);
            int indiceFin = indiceInicio + tamGrupo;
            grupos.add(new ArrayList<>(equipos.subList(indiceInicio, indiceFin)));
            indiceInicio = indiceFin;
        }

        return grupos;
    }

    private void generarPrimeraRondaBracket(Competicion comp, List<Equipo> equipos, LocalDate fechaInicio, String fase) {
        int totalEquipos = equipos.size();
        int totalPartidos = totalEquipos / 2;

        for (int i = 0; i < totalPartidos; i++) {
            Equipo local = equipos.get(i);
            Equipo visitante = equipos.get(totalEquipos - 1 - i);
            persistirPartido(comp, local, visitante, fase, fechaInicio.plusDays(i));
        }
    }

    private List<Equipo> obtenerClasificadosDebug(List<Partido> partidosRoundRobin, int equiposClasificanArbol) {
        Map<String, Map<Long, Equipo>> equiposPorGrupo = new LinkedHashMap<>();

        for (Partido partido : partidosRoundRobin) {
            String grupo = extraerNombreGrupo(partido.getFase());
            if (grupo == null) {
                continue;
            }

            equiposPorGrupo.computeIfAbsent(grupo, g -> new LinkedHashMap<>());
            Map<Long, Equipo> equiposGrupo = equiposPorGrupo.get(grupo);
            equiposGrupo.put(partido.getLocal().getId(), partido.getLocal());
            equiposGrupo.put(partido.getVisitante().getId(), partido.getVisitante());
        }

        List<Equipo> clasificados = new ArrayList<>();
        for (Map.Entry<String, Map<Long, Equipo>> entry : equiposPorGrupo.entrySet()) {
            List<Equipo> equiposGrupo = new ArrayList<>(entry.getValue().values());
            equiposGrupo.sort(Comparator.comparingLong(Equipo::getId));

            if (equiposGrupo.size() < equiposClasificanArbol) {
                return new ArrayList<>();
            }

            for (int i = 0; i < equiposClasificanArbol; i++) {
                clasificados.add(equiposGrupo.get(i));
            }
        }

        return clasificados;
    }

    private String extraerNombreGrupo(String fase) {
        if (fase == null || !fase.startsWith("ROUND ROBIN - GRUPO ")) {
            return null;
        }

        int inicio = "ROUND ROBIN - GRUPO ".length();
        int fin = fase.indexOf(" - JORNADA", inicio);
        if (fin <= inicio) {
            return null;
        }
        return fase.substring(inicio, fin).trim();
    }

    private boolean esPotenciaDeDos(int valor) {
        return valor > 0 && (valor & (valor - 1)) == 0;
    }

    private String getNombreGrupo(int indice) {
        StringBuilder nombre = new StringBuilder();
        int numero = indice;
        do {
            int resto = numero % 26;
            nombre.insert(0, (char) ('A' + resto));
            numero = (numero / 26) - 1;
        } while (numero >= 0);
        return nombre.toString();
    }
}
