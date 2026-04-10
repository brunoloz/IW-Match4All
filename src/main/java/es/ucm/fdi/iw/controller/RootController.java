package es.ucm.fdi.iw.controller;

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

    @GetMapping("/competiciones")      //ruta
    public String competiciones(Model model) { //nombre da igual
        return "competiciones";            //nombre de 
    }

    @GetMapping("/competiciones/{id}")
    public String competicion(@PathVariable("id") long id, Model model) {
        Competicion competicion = entityManager.find(Competicion.class, id);
        //List<Equipo> clasificacion = entityManager.createQuery()
        model.addAttribute("competicionSeleccionada", competicion);
        return "competiciones";
    }

    @GetMapping("/listacompeticiones")      //ruta
    public String listacompeticiones(Model model) { //nombre da igual
        List<Competicion> listaCompeticiones = entityManager.createQuery("SELECT c FROM Competicion c", Competicion.class).getResultList();
        model.addAttribute("competiciones", listaCompeticiones);
        return "listacompeticiones";            //nombre de 
    }

    @GetMapping("/actapartido")      //ruta
    public String actapartido(Model model) { //nombre da igual
        return "actapartido";            //nombre de 
    }

    @GetMapping("/paneladmin")      //ruta
    @Transactional
    public String paneladmin(Model model) { //nombre da igual
        List<Competicion> competiciones = entityManager
            .createQuery("SELECT DISTINCT c FROM Competicion c LEFT JOIN FETCH c.equipos ORDER BY c.id DESC", Competicion.class)
            .getResultList();

        for(Competicion c : competiciones) { //carga solicitudes de inscripcion
            org.hibernate.Hibernate.initialize(c.getEquiposSolicitantes());
            if (c.getEquiposSolicitantes() == null) {
                c.setEquiposSolicitantes(new java.util.ArrayList<>());
            }
        }

        List<User> jugadores = entityManager
            .createQuery("SELECT u FROM User u ORDER BY u.username ASC", User.class)
            .getResultList();

        List<Equipo> equipos = entityManager
            .createQuery("SELECT DISTINCT e FROM Equipo e LEFT JOIN FETCH e.capitan ORDER BY e.nombre ASC", Equipo.class)
            .getResultList();

        model.addAttribute("competiciones", competiciones);
        model.addAttribute("jugadores", jugadores);
        model.addAttribute("equipos", equipos);
        return "paneladmin";            //nombre de 
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
            return "redirect:/paneladmin";
        }

        if (nombre == null || nombre.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("error", "El nombre de la competición debe tener al menos 3 caracteres.");
            return "redirect:/paneladmin";
        }

        if (capacidad < 2 || capacidad > 128) {
            redirectAttributes.addFlashAttribute("error", "La capacidad debe estar entre 2 y 128 equipos.");
            return "redirect:/paneladmin";
        }

        List<Competicion> existentes = entityManager
            .createQuery("SELECT c FROM Competicion c WHERE LOWER(c.nombre) = LOWER(:nombre)", Competicion.class)
            .setParameter("nombre", nombre.trim())
            .getResultList();

        if (!existentes.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ya existe una competición con ese nombre.");
            return "redirect:/paneladmin";
        }

        Competicion.Tipo tipoCompeticion;
        try {
            tipoCompeticion = Competicion.Tipo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", "Tipo de competición no válido.");
            return "redirect:/paneladmin";
        }

        Competicion competicion = new Competicion();
        competicion.setNombre(nombre.trim());
        competicion.setTipo(tipoCompeticion);
        competicion.setCapacidad(capacidad);
        entityManager.persist(competicion);

        redirectAttributes.addFlashAttribute("success", "Competición creada correctamente.");
        return "redirect:/paneladmin";
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
            return "redirect:/paneladmin";
        }

        User target = entityManager.find(User.class, id);
        if (target == null) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/paneladmin";
        }

        if (target.getId() == currentUser.getId()) {
            redirectAttributes.addFlashAttribute("error", "No puedes deshabilitar tu propio usuario.");
            return "redirect:/paneladmin";
        }

        if (reason == null || reason.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("error", "Debes indicar un motivo de al menos 3 caracteres.");
            return "redirect:/paneladmin";
        }

        target.setEnabled(!target.isEnabled());
        log.info("Moderación de usuario {} por admin {}. Motivo: {}", target.getUsername(), currentUser.getUsername(), reason.trim());
        redirectAttributes.addFlashAttribute("success", "Estado de usuario actualizado.");
        return "redirect:/paneladmin";
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
            return "redirect:/paneladmin";
        }
        if (reason == null || reason.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("error", "Debes indicar un motivo de al menos 3 caracteres.");
            return "redirect:/paneladmin";
        }

        Competicion competicion = entityManager.find(Competicion.class, id);
        if (competicion == null) {
            redirectAttributes.addFlashAttribute("error", "Competición no encontrada.");
            return "redirect:/paneladmin";
        }

        long partidosCount = entityManager
            .createQuery("SELECT COUNT(p) FROM Partido p WHERE p.idCompeticion.id = :id", Long.class)
            .setParameter("id", id)
            .getSingleResult();

        if (partidosCount > 0) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar la competición porque tiene partidos asociados.");
            return "redirect:/paneladmin";
        }

        competicion.getEquipos().clear();
        entityManager.merge(competicion);
        entityManager.remove(competicion);

        log.info("Competición {} eliminada por admin {}. Motivo: {}", competicion.getNombre(), currentUser.getUsername(), reason.trim());
        redirectAttributes.addFlashAttribute("success", "Competición eliminada correctamente.");
        return "redirect:/paneladmin";
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
            return "redirect:/paneladmin";
        }
        if (reason == null || reason.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("error", "Debes indicar un motivo de al menos 3 caracteres.");
            return "redirect:/paneladmin";
        }

        Equipo equipo = entityManager.find(Equipo.class, id);
        if (equipo == null) {
            redirectAttributes.addFlashAttribute("error", "Equipo no encontrado.");
            return "redirect:/paneladmin";
        }

        log.info("Equipo {} moderado por admin {}. Motivo: {}", equipo.getNombre(), currentUser.getUsername(), reason.trim());
        redirectAttributes.addFlashAttribute("success", "Equipo moderado correctamente.");
        return "redirect:/paneladmin";
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
            return "redirect:/paneladmin";
        }
        if (reason == null || reason.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("error", "Debes indicar un motivo de al menos 3 caracteres.");
            return "redirect:/paneladmin";
        }

        Equipo equipo = entityManager.find(Equipo.class, id);
        if (equipo == null) {
            redirectAttributes.addFlashAttribute("error", "Equipo no encontrado.");
            return "redirect:/paneladmin";
        }

        long partidosCount = entityManager
            .createQuery("SELECT COUNT(p) FROM Partido p WHERE p.local.id = :id OR p.visitante.id = :id", Long.class)
            .setParameter("id", id)
            .getSingleResult();

        if (partidosCount > 0) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar el equipo porque tiene partidos asociados.");
            return "redirect:/paneladmin";
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
        return "redirect:/paneladmin";
    }

    @GetMapping("/autores")      //ruta
    public String autores(Model model) { //nombre da igual
        return "autores";            //nombre de 
    }

    //----Solicitar, Aceptar, Rechazar, expulsar equipos----

    @PostMapping("/competicion/solicitar")
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

    @PostMapping("/competicion/aceptar")
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

    @PostMapping("/competicion/rechazar")
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
