package es.ucm.fdi.iw.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
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

            if (comp.getTipo().name().equals("LIGA")) {
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
    // Solo permite generar calendario si el estado es INSCRIPCION
    if (comp.getEstado() != Competicion.Estado.INSCRIPCION) {
        redir.addFlashAttribute("error", "Solo se puede generar el calendario cuando la competición está en fase de inscripción.");
        return "redirect:/paneladmin";
    }
    List<Equipo> equipos = new ArrayList<>(comp.getEquipos());

    int numEquipos = equipos.size();
    int numJornadasIda = numEquipos - 1;
    int numPartidosPorJornada = numEquipos / 2;

    for (int i = 0; i < numJornadasIda; i++) {
        for (int j = 0; j < numPartidosPorJornada; j++) {
            Equipo local = equipos.get(j);
            Equipo visitante = equipos.get(numEquipos - 1 - j);

            Partido partidoIda = new Partido();
            partidoIda.setCompeticion(comp);
            partidoIda.setFase("JORNADA " + (i + 1));
            partidoIda.setFecha(LocalDate.now().plusWeeks(i));
            partidoIda.setEstado("PENDIENTE");

            if (j == 0 && i % 2 == 1) {
                partidoIda.setLocal(visitante);
                partidoIda.setVisitante(local);
            } else {
                partidoIda.setLocal(local);
                partidoIda.setVisitante(visitante);
            }

            partidoIda.setUbicacion(partidoIda.getLocal().getUbicacion());
            entityManager.persist(partidoIda);

            Partido partidoVuelta = new Partido();
            partidoVuelta.setCompeticion(comp);
            int jornadaVuelta = i + 1 + numJornadasIda;
            partidoVuelta.setFase("JORNADA " + jornadaVuelta);
            partidoVuelta.setFecha(LocalDate.now().plusWeeks(jornadaVuelta - 1));
            partidoVuelta.setEstado("PENDIENTE");

            partidoVuelta.setLocal(partidoIda.getVisitante());
            partidoVuelta.setVisitante(partidoIda.getLocal());
            partidoVuelta.setUbicacion(partidoVuelta.getLocal().getUbicacion());
            entityManager.persist(partidoVuelta);
        }

        Equipo ultimo = equipos.remove(equipos.size() - 1);
        equipos.add(1, ultimo);
    }


    comp.setEstado(Competicion.Estado.EN_CURSO);
    entityManager.merge(comp);
    redir.addFlashAttribute("success", "Calendario generado con éxito. La competición ha comenzado.");
    return "redirect:/paneladmin";
}
    
}
