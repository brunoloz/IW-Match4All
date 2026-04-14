package es.ucm.fdi.iw.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
@RequestMapping("equipo")
public class EquipoController {

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
    public String solicitarUnirse(@RequestParam("idEquipo") long idEquipo, HttpSession session,
            RedirectAttributes redir) {
        User sessionUser = (User) session.getAttribute("u");
        if (sessionUser == null)
            return "redirect:/login";

        User currentUser = entityManager.find(User.class, sessionUser.getId());
        if (currentUser.getEquipo() != null) {
            redir.addFlashAttribute("error", "Ya perteneces a un equipo.");
            return "redirect:/listaequipos";
        }

        if (currentUser.getEquipoSolicitado() != null) {
            redir.addFlashAttribute("error", "Ya tienes una solicitud pendiente para otro equipo.");
            return "redirect:/listaequipos";
        }

        Equipo eq = entityManager.find(Equipo.class, idEquipo);
        currentUser.setEquipoSolicitado(eq);
        entityManager.merge(currentUser);
        session.setAttribute("u", currentUser);
        redir.addFlashAttribute("success", "Solicitud enviada a " + eq.getNombre());
        return "redirect:/listaequipos";
    }

    @PostMapping("/aceptar")
    @Transactional
    public String aceptarJugador(@RequestParam("idUsuario") long idUsuario, HttpSession session,
            RedirectAttributes redir) {
        User capitan = (User) session.getAttribute("u");
        if (capitan == null)
            return "redirect:/login";

        User dbCapitan = entityManager.find(User.class, capitan.getId());
        Equipo equipo = dbCapitan.getEquipo();
        if (equipo == null || equipo.getCapitan().getId() != capitan.getId()) {
            redir.addFlashAttribute("error", "No tienes permisos.");
            return "redirect:/equipo/" + (equipo != null ? equipo.getId() : "");
        }

        User solicitante = entityManager.find(User.class, idUsuario);
        if (solicitante != null && solicitante.getEquipoSolicitado() != null
                && solicitante.getEquipoSolicitado().getId() == equipo.getId()) {
            solicitante.setEquipo(equipo);
            solicitante.setEquipoSolicitado(null);
            entityManager.merge(solicitante);
            redir.addFlashAttribute("success", solicitante.getUsername() + " ha sido aceptado en el equipo.");
        }

        
        return "redirect:/equipo/" + equipo.getId();
    }

    @PostMapping("/rechazar")
    @Transactional
    public String rechazarJugador(@RequestParam("idUsuario") long idUsuario, HttpSession session,
            RedirectAttributes redir) {
        User capitan = (User) session.getAttribute("u");
        if (capitan == null)
            return "redirect:/login";

        User dbCapitan = entityManager.find(User.class, capitan.getId());
        Equipo equipo = dbCapitan.getEquipo();
        if (equipo == null || equipo.getCapitan().getId() != capitan.getId()) {
            redir.addFlashAttribute("error", "No tienes permisos.");
            return "redirect:/equipo/" + (equipo != null ? equipo.getId() : "");
        }

        User solicitante = entityManager.find(User.class, idUsuario);
        if (solicitante != null && solicitante.getEquipoSolicitado() != null
                && solicitante.getEquipoSolicitado().getId() == equipo.getId()) {
            solicitante.setEquipoSolicitado(null);
            entityManager.merge(solicitante);
            redir.addFlashAttribute("success", "Has rechazado la solicitud de " + solicitante.getUsername() + ".");
        }
        return "redirect:/equipo/" + equipo.getId();
    }

    @PostMapping("/expulsar")
    @Transactional
    public String expulsarJugador(@RequestParam("idUsuario") long idUsuario, HttpSession session,
            RedirectAttributes redir) {
        User sessionUser = (User) session.getAttribute("u");
        if (sessionUser == null)
            return "redirect:/login";

        User capitan = entityManager.find(User.class, sessionUser.getId());
        Equipo equipo = capitan.getEquipo();
        if (equipo == null || equipo.getCapitan().getId() != capitan.getId()) {
            redir.addFlashAttribute("error", "No tienes permisos para expulsar jugadores.");
            return "redirect:/gestionequipo";
        }

        User jugadorAExpulsar = entityManager.find(User.class, idUsuario);
        if (jugadorAExpulsar == null) {
            redir.addFlashAttribute("error", "El jugador no existe.");
            return "redirect:/gestionequipo";
        }

        if (jugadorAExpulsar.getEquipo() == null || jugadorAExpulsar.getEquipo().getId() != equipo.getId()) {
            redir.addFlashAttribute("error", "Ese jugador no pertenece a tu equipo.");
            return "redirect:/gestionequipo";
        }

        if (jugadorAExpulsar.getId() == capitan.getId()) {
            redir.addFlashAttribute("error",
                    "No puedes expulsarte a ti mismo. Para salir, debes disolver el equipo o ceder la capitanía.");
            return "redirect:/gestionequipo";
        }

        jugadorAExpulsar.setEquipo(null);
        entityManager.merge(jugadorAExpulsar);
        redir.addFlashAttribute("success", "Has expulsado a " + jugadorAExpulsar.getUsername() + " del equipo.");
        return "redirect:/gestionequipo";
    }
}
