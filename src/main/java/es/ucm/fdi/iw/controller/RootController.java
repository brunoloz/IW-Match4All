package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import es.ucm.fdi.iw.model.Competicion;
import es.ucm.fdi.iw.model.Equipo;
import es.ucm.fdi.iw.model.User;

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

    @GetMapping("/vistacompeticiones")      //ruta
    public String vistacompeticiones(Model model) { //nombre da igual
        return "vistacompeticiones";            //nombre de vista
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
        return "vistapaneladmin";            //nombre de vista
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
