package es.ucm.fdi.iw.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import es.ucm.fdi.iw.model.Topic;
import es.ucm.fdi.iw.model.Competicion;
import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.Lorem;
import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.Transferable;
import es.ucm.fdi.iw.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

/**
 * Site administration.
 *
 * Access to this end-point is authenticated - see SecurityConfig
 */
@Controller
@RequestMapping("admin")
public class AdminController {

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private EntityManager entityManager;

  @ModelAttribute
  public void populateModel(HttpSession session, Model model) {
    for (String name : new String[] { "u", "url", "ws", "topics"}) {
      model.addAttribute(name, session.getAttribute(name));
    }
  }

  private static final Logger log = LogManager.getLogger(AdminController.class);

  @GetMapping("/")
  public String index(Model model) {
    log.info("Admin acaba de entrar");
    model.addAttribute("users",
        entityManager.createQuery("select u from User u").getResultList());
    return "admin";
  }

  @PostMapping("/toggle/{id}")
  @Transactional
  @ResponseBody
  public String toggleUser(@PathVariable long id, Model model) {
    log.info("Admin cambia estado de " + id);
    User target = entityManager.find(User.class, id);
    target.setEnabled(!target.isEnabled());
    return "{\"enabled\":" + target.isEnabled() + "}";
  }

  /**
   * Returns JSON with all received messages
   */
  @GetMapping(path = "all-messages", produces = "application/json")
  @Transactional // para no recibir resultados inconsistentes
  @ResponseBody // para indicar que no devuelve vista, sino un objeto (jsonizado)
  public List<Message.Transfer> retrieveMessages(HttpSession session) {
    TypedQuery<Message> query = entityManager.createQuery("select m from Message m", Message.class);
    query.setMaxResults(5);
    query.setFirstResult(0); // para paginar: cambias el 1er resultado
    // devuelve resultado
    return query.getResultList().stream().map(Transferable::toTransfer)
        .collect(Collectors.toList());
  }

  @RequestMapping("/populate")
  @ResponseBody
  @Transactional
  public String populate(Model model) {

    // create some groups
    Topic g1 = new Topic();
    g1.setName("g1");
    g1.setKey(UserController.generateRandomBase64Token(6));
    entityManager.persist(g1);
    Topic g2 = new Topic();
    g2.setName("g2");
    g2.setKey(UserController.generateRandomBase64Token(6));
    entityManager.persist(g2);

    // create some users & assign to groups
    for (int i = 0; i < 15; i++) {
      User u = new User();
      u.setUsername("user" + i);
      u.setPassword(passwordEncoder
          .encode("aa"));
            //UserController.generateRandomBase64Token(9)));
      u.setEnabled(true);
      u.setRoles(User.Role.USER.toString());
      u.setFirstName(Lorem.nombreAlAzar());
      u.setLastName(Lorem.apellidoAlAzar());
      entityManager.persist(u);
      if (i%2 == 0) {
        g1.getMembers().add(u);
        // u.getTopics().add(g1); NO FUNCIONA: propietario es g, no u
      }
      if (i%3 == 0) {
        g2.getMembers().add(u);
      }
    }
    return "{\"admin\": \"populated\"}";
  }

  @PostMapping("/crear-competicion")
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

      if (capacidad % 2 == 1) {
          redirectAttributes.addFlashAttribute("error", "El número de equipos debe ser par.");
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

  @PostMapping("/toggle-user/{id}")
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

  @PostMapping("/eliminar-competicion/{id}")
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

  @PostMapping("/moderar-equipo/{id}")
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

  @PostMapping("/eliminar-equipo/{id}")
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

}
