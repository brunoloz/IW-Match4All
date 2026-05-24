package es.ucm.fdi.iw.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.ucm.fdi.iw.model.Competicion;
import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.Partido;
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

    @GetMapping("/{id}")
    @Transactional
    public String equipoById(@PathVariable("id") long id, Model model) {
        Equipo equipo = entityManager.find(Equipo.class, id);
        if (equipo != null) {
            org.hibernate.Hibernate.initialize(equipo.getJugadores());
            List<Competicion> competicionesEquipo = entityManager
                    .createQuery("SELECT c FROM Competicion c JOIN c.equipos e WHERE e.id = :id", Competicion.class)
                    .setParameter("id", equipo.getId())
                    .getResultList();
            
            List<Partido> partidosJugados = entityManager
                    .createQuery("SELECT p FROM Partido p WHERE (p.local.id = :id OR p.visitante.id = :id) AND p.estado != :estadoPendiente ORDER BY p.fecha DESC", Partido.class)
                    .setParameter("id", equipo.getId())
                    .setParameter("estadoPendiente", Partido.State.PENDIENTE)
                    .getResultList();

            List<Partido> proximosPartidos = entityManager
                    .createQuery("SELECT p FROM Partido p WHERE (p.local.id = :id OR p.visitante.id = :id) AND p.estado = :estadoPendiente ORDER BY p.fecha ASC", Partido.class)
                    .setParameter("id", equipo.getId())
                    .setParameter("estadoPendiente", Partido.State.PENDIENTE)
                    .getResultList();

            // NUEVO: Comprobar si el equipo tiene algún partido en curso
            boolean partidoEnCurso = !entityManager
                    .createQuery("SELECT p FROM Partido p WHERE (p.local.id = :id OR p.visitante.id = :id) AND p.estado = :estadoEnCurso", Partido.class)
                    .setParameter("id", equipo.getId())
                    .setParameter("estadoEnCurso", Partido.State.EN_CURSO) // Asegúrate de que EN_CURSO existe en tu enum
                    .getResultList().isEmpty();

            // Contar titulares en el equipo
            Long numTitulares = entityManager.createQuery("SELECT COUNT(u) FROM User u WHERE u.titular = true AND u.equipo.id = :equipoId", Long.class)
                    .setParameter("equipoId", equipo.getId())
                    .getSingleResult();

            // Si hay menos de 11 titulares, mostrar mensaje de aviso
            if (numTitulares < 11) {
                model.addAttribute("warning", "¡La plantilla no tiene suficientes titulares! Añade jugadores titulares. Titulares: " + numTitulares + "/11");
            }

            model.addAttribute("equipo", equipo);
            model.addAttribute("competicionesEquipo", competicionesEquipo);
            model.addAttribute("partidosJugados", partidosJugados);
            model.addAttribute("proximosPartidos", proximosPartidos);
            model.addAttribute("partidoEnCurso", partidoEnCurso); // Lo pasamos a la vista
        } else {
            model.addAttribute("equipo", null);
            model.addAttribute("competicionesEquipo", java.util.Collections.emptyList());
            model.addAttribute("partidosJugados", java.util.Collections.emptyList());
            model.addAttribute("proximosPartidos", java.util.Collections.emptyList());
            model.addAttribute("partidoEnCurso", false);
        }
        return "equipo";
    }

    @PostMapping("/titular-user/{id}")
    @Transactional
    public String titularUser(
          @PathVariable("id") long id,
          HttpSession session,
          RedirectAttributes redirectAttributes) {
    
        User currentUser = (User) session.getAttribute("u");
        if (currentUser == null) {
            return "redirect:/login";
        }

        currentUser = entityManager.find(User.class, currentUser.getId());
        Equipo equipo = currentUser.getEquipo();

        if (equipo == null || equipo.getCapitan() == null || currentUser.getId() != equipo.getCapitan().getId()) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para modificar jugadores.");
            return "redirect:/equipo/" + (equipo != null ? equipo.getId() : "");
        }

        Long partidosEnCurso = entityManager.createQuery(
                "SELECT COUNT(p) FROM Partido p WHERE (p.local.id = :equipoId OR p.visitante.id = :equipoId) AND p.estado = :estadoEnCurso", Long.class)
                .setParameter("equipoId", equipo.getId())
                .setParameter("estadoEnCurso", Partido.State.EN_CURSO)
                .getSingleResult();

        if (partidosEnCurso > 0) {
            redirectAttributes.addFlashAttribute("error", "No puedes modificar la alineación mientras hay un partido en curso.");
            return "redirect:/equipo/" + equipo.getId();
        }

        User target = entityManager.find(User.class, id);
        
        Long titulares = entityManager.createQuery("SELECT COUNT(u) FROM User u WHERE u.titular = true AND u.equipo.id = :equipoId", Long.class)
                .setParameter("equipoId", equipo.getId())
                .getSingleResult();
        
        if (!target.isTitular()) {

            if (titulares >= 11) {
                redirectAttributes.addFlashAttribute("error", "No puede haber más de 11 jugadores titulares. Haz suplente a otro jugador primero.");
                return "redirect:/equipo/" + equipo.getId(); 
            }
        }

        target.setTitular(!target.isTitular());
        return "redirect:/equipo/" + equipo.getId();
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
            return "redirect:/equipo/" + equipo.getId();
        }

        User jugadorAExpulsar = entityManager.find(User.class, idUsuario);
        if (jugadorAExpulsar == null) {
            redir.addFlashAttribute("error", "El jugador no existe.");
            return "redirect:/equipo/" + equipo.getId();
        }

        if (jugadorAExpulsar.getEquipo() == null || jugadorAExpulsar.getEquipo().getId() != equipo.getId()) {
            redir.addFlashAttribute("error", "Ese jugador no pertenece a tu equipo.");
            return "redirect:/equipo/" + equipo.getId();
        }

        if (jugadorAExpulsar.getId() == capitan.getId()) {
            redir.addFlashAttribute("error",
                    "No puedes expulsarte a ti mismo.");
            return "redirect:/equipo/" + equipo.getId();
        }

        if(jugadorAExpulsar.isTitular()){

            redir.addFlashAttribute("error",
                    "No puedes expulsar a un jugador titular. Debes hacerle suplente si deseas expulsarle");
            return "redirect:/equipo/" + equipo.getId();
        }



        jugadorAExpulsar.setEquipo(null);
        entityManager.merge(jugadorAExpulsar);
        redir.addFlashAttribute("success", "Has expulsado a " + jugadorAExpulsar.getUsername() + " del equipo.");
        return "redirect:/equipo/" + equipo.getId();
    }
}
