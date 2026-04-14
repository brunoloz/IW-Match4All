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

import es.ucm.fdi.iw.model.Clasificacion;
import es.ucm.fdi.iw.model.Competicion;
import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.Partido;
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

    //private static final Logger log = LogManager.getLogger(RootController.class);

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

    @GetMapping("/autores")      //ruta
    public String autores(Model model) { //nombre da igual
        return "autores";            //nombre de 
    }


    @GetMapping("/equipo/{id}")
    @Transactional
    public String equipoById(@PathVariable("id") long id, Model model) {
        Equipo equipo = entityManager.find(Equipo.class, id);
        if (equipo != null) {
            org.hibernate.Hibernate.initialize(equipo.getJugadores());
            List<Competicion> competicionesEquipo = entityManager
                    .createQuery("SELECT c FROM Competicion c JOIN c.equipos e WHERE e.id = :id", Competicion.class)
                    .setParameter("id", equipo.getId())
                    .getResultList();
            model.addAttribute("equipo", equipo);
            model.addAttribute("competicionesEquipo", competicionesEquipo);
        } else {
            model.addAttribute("equipo", null);
            model.addAttribute("competicionesEquipo", java.util.Collections.emptyList());
        }
        return "equipo";
    }

    @GetMapping("/listaequipos")
    public String listaequipos(Model model, HttpSession session) {

        User u = (User) session.getAttribute("u");
        List<Equipo> listaEquipos = entityManager.createQuery("SELECT e FROM Equipo e", Equipo.class)
                .getResultList();
        model.addAttribute("equipos", listaEquipos);
        model.addAttribute("u", u);
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

}
