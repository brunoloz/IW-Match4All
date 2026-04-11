package es.ucm.fdi.iw.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.ucm.fdi.iw.model.Competicion;
import es.ucm.fdi.iw.model.Equipo;
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
        if (comp.getEquipos().contains(equipo)) {
            redir.addFlashAttribute("error", "Tu equipo ya está inscrito en esta competición.");
            return "redirect:/listacompeticiones";
        }
        // Evita duplicados en solicitudes
        if (comp.getEquiposSolicitantes().contains(equipo)) {
            redir.addFlashAttribute("error", "Tu equipo ya ha solicitado unirse a esta competición.");
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
            comp.getEquiposSolicitantes().remove(eq);
            comp.getEquipos().add(eq);
            entityManager.merge(comp);
            redir.addFlashAttribute("success", eq.getNombre() + " ha sido aceptado en " + comp.getNombre());
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
    
    
    
}
