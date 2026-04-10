package es.ucm.fdi.iw.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
public class EquipoController {

    @PersistenceContext
    private EntityManager entityManager;

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {
        for (String name : new String[] { "u", "url", "ws", "topics" }) {
            model.addAttribute(name, session.getAttribute(name));
        }
    }

    @GetMapping("/gestionequipo")
    @Transactional
    public String gestionequipo(Model model, HttpSession session) {
        User u = (User) session.getAttribute("u");

        if (u != null && u.getEquipo() != null) {
            Equipo equipo = entityManager.find(Equipo.class, u.getEquipo().getId());
            org.hibernate.Hibernate.initialize(equipo.getJugadores());
            org.hibernate.Hibernate.initialize(equipo.getSolicitantes());
            model.addAttribute("equipo", equipo);
        } else {
            model.addAttribute("equipo", null);
        }

        return "gestionequipo";
    }

    @GetMapping("/gestionequipo/{id}")
    @Transactional
    public String gestionequipoById(@PathVariable("id") long id, Model model) {
        Equipo equipo = entityManager.find(Equipo.class, id);
        if (equipo != null) {
            org.hibernate.Hibernate.initialize(equipo.getJugadores());
        }
        model.addAttribute("equipo", equipo);
        return "gestionequipo";
    }

    @GetMapping("/listaequipos")
    public String listaequipos(Model model) {
        List<Equipo> listaEquipos = entityManager.createQuery("SELECT e FROM Equipo e", Equipo.class)
                .getResultList();
        model.addAttribute("equipos", listaEquipos);
        return "listaequipos";
    }

    @GetMapping("/crearequipo")
    public String crearequipo(Model model) {
        return "crearequipo";
    }

    @PostMapping("/crear-equipo")
    @Transactional
    public String crearEquipo(
            @RequestParam("nombre") String nombre,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("ubicacion") String ubicacion,
            @RequestParam(value = "escudo", required = false) MultipartFile escudo,
            HttpSession session,
            Model model) {

        User sessionUser = (User) session.getAttribute("u");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        User currentUser = entityManager.find(User.class, sessionUser.getId());
        if (currentUser.getEquipo() != null) {
            model.addAttribute("error", "Ya perteneces a un equipo. No puedes crear uno nuevo.");
            return "crearequipo";
        }

        if (nombre == null || nombre.trim().length() < 3) {
            model.addAttribute("error", "El nombre del equipo debe tener al menos 3 caracteres.");
            return "crearequipo";
        }

        List<Equipo> equiposExistentes = entityManager
                .createQuery("SELECT e FROM Equipo e WHERE LOWER(e.nombre) = LOWER(:nombre)", Equipo.class)
                .setParameter("nombre", nombre.trim())
                .getResultList();

        if (!equiposExistentes.isEmpty()) {
            model.addAttribute("error", "Ya existe un equipo con ese nombre. Elige otro nombre.");
            return "crearequipo";
        }

        try {
            Equipo nuevoEquipo = new Equipo();
            nuevoEquipo.setNombre(nombre.trim());
            nuevoEquipo.setDescripcion(descripcion != null ? descripcion.trim() : "");
            nuevoEquipo.setUbicacion(ubicacion != null ? ubicacion.trim() : "");
            nuevoEquipo.setCapitan(currentUser);

            if (escudo != null && !escudo.isEmpty()) {
                String fileName = escudo.getOriginalFilename();
                Path uploadPath = Paths.get("src/main/resources/static/img/equipos/");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Files.write(uploadPath.resolve(fileName), escudo.getBytes());
                nuevoEquipo.setEscudo("equipos/" + fileName);
            } else {
                nuevoEquipo.setEscudo("equipos/default.png");
            }

            if (nuevoEquipo.getJugadores() == null) {
                nuevoEquipo.setJugadores(new java.util.ArrayList<>());
            }
            nuevoEquipo.getJugadores().add(currentUser);

            entityManager.persist(nuevoEquipo);
            currentUser.setEquipo(nuevoEquipo);
            User usuarioActualizado = entityManager.merge(currentUser);
            session.setAttribute("u", usuarioActualizado);
            return "redirect:/gestionequipo";
        } catch (IOException e) {
            model.addAttribute("error", "Error al procesar la imagen del escudo. Inténtalo de nuevo.");
            return "crearequipo";
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear el equipo. Inténtalo de nuevo.");
            return "crearequipo";
        }
    }

    @PostMapping("/equipo/solicitar")
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

    @PostMapping("/equipo/aceptar")
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
            return "redirect:/gestionequipo";
        }

        User solicitante = entityManager.find(User.class, idUsuario);
        if (solicitante != null && solicitante.getEquipoSolicitado() != null
                && solicitante.getEquipoSolicitado().getId() == equipo.getId()) {
            solicitante.setEquipo(equipo);
            solicitante.setEquipoSolicitado(null);
            entityManager.merge(solicitante);
            redir.addFlashAttribute("success", solicitante.getUsername() + " ha sido aceptado en el equipo.");
        }
        return "redirect:/gestionequipo";
    }

    @PostMapping("/equipo/rechazar")
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
            return "redirect:/gestionequipo";
        }

        User solicitante = entityManager.find(User.class, idUsuario);
        if (solicitante != null && solicitante.getEquipoSolicitado() != null
                && solicitante.getEquipoSolicitado().getId() == equipo.getId()) {
            solicitante.setEquipoSolicitado(null);
            entityManager.merge(solicitante);
            redir.addFlashAttribute("success", "Has rechazado la solicitud de " + solicitante.getUsername() + ".");
        }
        return "redirect:/gestionequipo";
    }

    @PostMapping("/equipo/expulsar")
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
