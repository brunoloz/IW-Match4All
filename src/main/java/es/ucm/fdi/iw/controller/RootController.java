package es.ucm.fdi.iw.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import es.ucm.fdi.iw.model.Competicion;
import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

/**
 *  Non-authenticated requests only.
 */
@Controller
public class RootController {

    private static final Logger log = LogManager.getLogger(RootController.class);

    @PersistenceContext
    private EntityManager entityManager;

    @ModelAttribute
    public void populateModel(HttpSession session, Model model) {        
        for (String name : new String[] { "u", "url", "ws", "topics"}) {
          model.addAttribute(name, session.getAttribute(name));
        }
    }

	@GetMapping("/login")
    public String login(Model model, HttpServletRequest request) {
        boolean error = request.getQueryString() != null && request.getQueryString().indexOf("error") != -1;
        model.addAttribute("loginError", error);
        return "login";
    }

	@GetMapping("/")
    public String index(Model model) {
        return "fragments/index";
    }

    @GetMapping("/vistaperfil")      //ruta
    public String vistaperfil(Model model) { //nombre da igual
        return "vistaperfil";            //nombre de vista
    }

    @GetMapping("/vistagestionequipo")
    @Transactional
    public String vistagestionequipo(Model model, HttpSession session) {

        User u = (User) session.getAttribute("u");

        if (u != null && u.getEquipo() != null) {

            Equipo equipo = entityManager.find(Equipo.class, u.getEquipo().getId());

            org.hibernate.Hibernate.initialize(equipo.getJugadores());

            model.addAttribute("equipo", equipo);
        } else {
            model.addAttribute("equipo", null);
        }

        return "vistagestionequipo";
    }

    @GetMapping("/vistagestionequipo/{id}")
    @Transactional
    public String vistagestionequipoById(@PathVariable("id") long id, Model model) {
        Equipo equipo = entityManager.find(Equipo.class, id);
        if (equipo != null) {
            org.hibernate.Hibernate.initialize(equipo.getJugadores());
        }
        model.addAttribute("equipo", equipo);
        return "vistagestionequipo";
    }

    @GetMapping("/vistacompeticiones")      //ruta
    public String vistacompeticiones(Model model) { //nombre da igual
        return "vistacompeticiones";            //nombre de vista
    }

    @GetMapping("/vistacompeticiones/{id}")
    public String vistacompeticion(@PathVariable("id") long id, Model model) {
        Competicion competicion = entityManager.find(Competicion.class, id);
        model.addAttribute("competicionSeleccionada", competicion);
        return "vistacompeticiones";
    }

        @GetMapping("/vistalistacompeticiones")      //ruta
    public String vistalistacompeticiones(Model model) { //nombre da igual
        List<Competicion> listaCompeticiones = entityManager.createQuery("SELECT c FROM Competicion c", Competicion.class).getResultList();
        model.addAttribute("competiciones", listaCompeticiones);
        return "vistalistacompeticiones";            //nombre de vista
    }

    @GetMapping("/vistaactapartido")      //ruta
    public String vistaactapartido(Model model) { //nombre da igual
        return "vistaactapartido";            //nombre de vista
    }

    @GetMapping("/vistapaneladmin")      //ruta
    public String vistapaneladmin(Model model) { //nombre da igual
        List<Competicion> competiciones = entityManager
            .createQuery("SELECT DISTINCT c FROM Competicion c LEFT JOIN FETCH c.equipos ORDER BY c.id DESC", Competicion.class)
            .getResultList();

        List<User> jugadores = entityManager
            .createQuery("SELECT u FROM User u ORDER BY u.username ASC", User.class)
            .getResultList();

        List<Equipo> equipos = entityManager
            .createQuery("SELECT DISTINCT e FROM Equipo e LEFT JOIN FETCH e.capitan ORDER BY e.nombre ASC", Equipo.class)
            .getResultList();

        model.addAttribute("competiciones", competiciones);
        model.addAttribute("jugadores", jugadores);
        model.addAttribute("equipos", equipos);
        return "vistapaneladmin";            //nombre de vista
    }

    @PostMapping("/admin/crear-competicion")
    @Transactional
    public String crearCompeticion(
            @RequestParam("nombre") String nombre,
            @RequestParam("tipo") String tipo,
            @RequestParam("capacidad") int capacidad,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("u");
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!currentUser.hasRole(User.Role.ADMIN)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para crear competiciones.");
            return "redirect:/vistapaneladmin";
        }

        if (nombre == null || nombre.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("error", "El nombre de la competición debe tener al menos 3 caracteres.");
            return "redirect:/vistapaneladmin";
        }

        if (capacidad < 2 || capacidad > 128) {
            redirectAttributes.addFlashAttribute("error", "La capacidad debe estar entre 2 y 128 equipos.");
            return "redirect:/vistapaneladmin";
        }

        List<Competicion> existentes = entityManager
            .createQuery("SELECT c FROM Competicion c WHERE LOWER(c.nombre) = LOWER(:nombre)", Competicion.class)
            .setParameter("nombre", nombre.trim())
            .getResultList();

        if (!existentes.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ya existe una competición con ese nombre.");
            return "redirect:/vistapaneladmin";
        }

        Competicion.Tipo tipoCompeticion;
        try {
            tipoCompeticion = Competicion.Tipo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", "Tipo de competición no válido.");
            return "redirect:/vistapaneladmin";
        }

        Competicion competicion = new Competicion();
        competicion.setNombre(nombre.trim());
        competicion.setTipo(tipoCompeticion);
        competicion.setCapacidad(capacidad);
        entityManager.persist(competicion);

        redirectAttributes.addFlashAttribute("success", "Competición creada correctamente.");
        return "redirect:/vistapaneladmin";
    }

    @PostMapping("/admin/toggle-user/{id}")
    @Transactional
    public String toggleUserPanel(
            @PathVariable("id") long id,
            @RequestParam("reason") String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("u");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!currentUser.hasRole(User.Role.ADMIN)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para moderar usuarios.");
            return "redirect:/vistapaneladmin";
        }

        User target = entityManager.find(User.class, id);
        if (target == null) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/vistapaneladmin";
        }

        if (target.getId() == currentUser.getId()) {
            redirectAttributes.addFlashAttribute("error", "No puedes deshabilitar tu propio usuario.");
            return "redirect:/vistapaneladmin";
        }

        if (reason == null || reason.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("error", "Debes indicar un motivo de al menos 3 caracteres.");
            return "redirect:/vistapaneladmin";
        }

        target.setEnabled(!target.isEnabled());
        log.info("Moderación de usuario {} por admin {}. Motivo: {}", target.getUsername(), currentUser.getUsername(), reason.trim());
        redirectAttributes.addFlashAttribute("success", "Estado de usuario actualizado.");
        return "redirect:/vistapaneladmin";
    }

    @PostMapping("/admin/eliminar-competicion/{id}")
    @Transactional
    public String eliminarCompeticion(
            @PathVariable("id") long id,
            @RequestParam("reason") String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("u");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!currentUser.hasRole(User.Role.ADMIN)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para eliminar competiciones.");
            return "redirect:/vistapaneladmin";
        }
        if (reason == null || reason.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("error", "Debes indicar un motivo de al menos 3 caracteres.");
            return "redirect:/vistapaneladmin";
        }

        Competicion competicion = entityManager.find(Competicion.class, id);
        if (competicion == null) {
            redirectAttributes.addFlashAttribute("error", "Competición no encontrada.");
            return "redirect:/vistapaneladmin";
        }

        long partidosCount = entityManager
            .createQuery("SELECT COUNT(p) FROM Partido p WHERE p.idCompeticion.id = :id", Long.class)
            .setParameter("id", id)
            .getSingleResult();

        if (partidosCount > 0) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar la competición porque tiene partidos asociados.");
            return "redirect:/vistapaneladmin";
        }

        competicion.getEquipos().clear();
        entityManager.merge(competicion);
        entityManager.remove(competicion);

        log.info("Competición {} eliminada por admin {}. Motivo: {}", competicion.getNombre(), currentUser.getUsername(), reason.trim());
        redirectAttributes.addFlashAttribute("success", "Competición eliminada correctamente.");
        return "redirect:/vistapaneladmin";
    }

    @PostMapping("/admin/moderar-equipo/{id}")
    @Transactional
    public String moderarEquipo(
            @PathVariable("id") long id,
            @RequestParam("reason") String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("u");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!currentUser.hasRole(User.Role.ADMIN)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para moderar equipos.");
            return "redirect:/vistapaneladmin";
        }
        if (reason == null || reason.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("error", "Debes indicar un motivo de al menos 3 caracteres.");
            return "redirect:/vistapaneladmin";
        }

        Equipo equipo = entityManager.find(Equipo.class, id);
        if (equipo == null) {
            redirectAttributes.addFlashAttribute("error", "Equipo no encontrado.");
            return "redirect:/vistapaneladmin";
        }

        log.info("Equipo {} moderado por admin {}. Motivo: {}", equipo.getNombre(), currentUser.getUsername(), reason.trim());
        redirectAttributes.addFlashAttribute("success", "Equipo moderado correctamente.");
        return "redirect:/vistapaneladmin";
    }

    @PostMapping("/admin/eliminar-equipo/{id}")
    @Transactional
    public String eliminarEquipo(
            @PathVariable("id") long id,
            @RequestParam("reason") String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("u");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!currentUser.hasRole(User.Role.ADMIN)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permisos para eliminar equipos.");
            return "redirect:/vistapaneladmin";
        }
        if (reason == null || reason.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("error", "Debes indicar un motivo de al menos 3 caracteres.");
            return "redirect:/vistapaneladmin";
        }

        Equipo equipo = entityManager.find(Equipo.class, id);
        if (equipo == null) {
            redirectAttributes.addFlashAttribute("error", "Equipo no encontrado.");
            return "redirect:/vistapaneladmin";
        }

        long partidosCount = entityManager
            .createQuery("SELECT COUNT(p) FROM Partido p WHERE p.local.id = :id OR p.visitante.id = :id", Long.class)
            .setParameter("id", id)
            .getSingleResult();

        if (partidosCount > 0) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el equipo porque tiene partidos asociados.");
            return "redirect:/vistapaneladmin";
        }

        List<User> usersInTeam = entityManager
            .createQuery("SELECT u FROM User u WHERE u.equipo.id = :id", User.class)
            .setParameter("id", id)
            .getResultList();
        for (User user : usersInTeam) {
            user.setEquipo(null);
        }

        List<Competicion> competicionesConEquipo = entityManager
            .createQuery("SELECT DISTINCT c FROM Competicion c JOIN c.equipos e WHERE e.id = :id", Competicion.class)
            .setParameter("id", id)
            .getResultList();
        for (Competicion competicion : competicionesConEquipo) {
            competicion.getEquipos().removeIf(e -> e.getId() == id);
            entityManager.merge(competicion);
        }

        entityManager.remove(equipo);

        log.info("Equipo {} eliminado por admin {}. Motivo: {}", equipo.getNombre(), currentUser.getUsername(), reason.trim());
        redirectAttributes.addFlashAttribute("success", "Equipo eliminado correctamente.");
        return "redirect:/vistapaneladmin";
    }

    @GetMapping("/vistacrearequipo")      //ruta
    public String vistacrearequipo(Model model) { //nombre da igual
        return "vistacrearequipo";            //nombre de vista
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

        User currentUser = (User) session.getAttribute("u");

        // Validaciones
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (currentUser.getEquipo() != null) {
            model.addAttribute("error", "Ya perteneces a un equipo. No puedes crear uno nuevo.");
            return "vistacrearequipo";
        }

        if (nombre == null || nombre.trim().length() < 3) {
            model.addAttribute("error", "El nombre del equipo debe tener al menos 3 caracteres.");
            return "vistacrearequipo";
        }

        // Verificar si ya existe un equipo con ese nombre
        List<Equipo> equiposExistentes = entityManager
            .createQuery("SELECT e FROM Equipo e WHERE LOWER(e.nombre) = LOWER(:nombre)", Equipo.class)
            .setParameter("nombre", nombre.trim())
            .getResultList();

        if (!equiposExistentes.isEmpty()) {
            model.addAttribute("error", "Ya existe un equipo con ese nombre. Elige otro nombre.");
            return "vistacrearequipo";
        }

        try {
            // Crear el nuevo equipo
            Equipo nuevoEquipo = new Equipo();
            nuevoEquipo.setNombre(nombre.trim());
            nuevoEquipo.setDescripcion(descripcion != null ? descripcion.trim() : "");
            nuevoEquipo.setUbicacion(ubicacion != null ? ubicacion.trim() : "");
            nuevoEquipo.setCapitan(currentUser);

            // Manejar la subida del escudo
            if (escudo != null && !escudo.isEmpty()) {
                String fileName = escudo.getOriginalFilename();
                Path uploadPath = Paths.get("src/main/resources/static/img/equipos/");

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Files.write(uploadPath.resolve(fileName), escudo.getBytes());
                nuevoEquipo.setEscudo("equipos/" + fileName);
            }

            // Añadir el usuario a la lista de jugadores 
            if (nuevoEquipo.getJugadores() == null) {
                nuevoEquipo.setJugadores(new java.util.ArrayList<>());
            }
            nuevoEquipo.getJugadores().add(currentUser);

            entityManager.persist(nuevoEquipo); //con esto guardo el equipo en la bd

            currentUser.setEquipo(nuevoEquipo);
            
            //actualizo la información del usuario que tiene la sesion iniciada
            User usuarioActualizado = entityManager.merge(currentUser);
            session.setAttribute("u", usuarioActualizado); 

            return "redirect:/user/" + usuarioActualizado.getId();

        } catch (IOException e) {
            log.error("Error al subir el escudo del equipo", e);
            model.addAttribute("error", "Error al procesar la imagen del escudo. Inténtalo de nuevo.");
            return "vistacrearequipo";
        } catch (Exception e) {
            log.error("Error al crear el equipo", e);
            model.addAttribute("error", "Error al crear el equipo. Inténtalo de nuevo.");
            return "vistacrearequipo";
        }
    }

    @GetMapping("/autores")      //ruta
    public String autores(Model model) { //nombre da igual
        return "autores";            //nombre de vista
    }

}
