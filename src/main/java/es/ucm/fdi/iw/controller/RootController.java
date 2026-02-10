package es.ucm.fdi.iw.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 *  Non-authenticated requests only.
 */
@Controller
public class RootController {

    private static final Logger log = LogManager.getLogger(RootController.class);

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

    @GetMapping("/vistaprincipal")      //ruta
    public String match4all(Model model) { //nombre da igual
        return "vistaprincipal";            //nombre de vista
    }

    @GetMapping("/vistaperfil")      //ruta
    public String vistaperfil(Model model) { //nombre da igual
        return "vistaperfil";            //nombre de vista
    }

    @GetMapping("/vistagestionequipo")      //ruta
    public String vistagestionequipo(Model model) { //nombre da igual
        return "vistagestionequipo";            //nombre de vista
    }

    @GetMapping("/vistacompeticiones")      //ruta
    public String vistacompeticiones(Model model) { //nombre da igual
        return "vistacompeticiones";            //nombre de vista
    }

    @GetMapping("/vistaactapartido")      //ruta
    public String vistaactapartido(Model model) { //nombre da igual
        return "vistaactapartido";            //nombre de vista
    }

    @GetMapping("/vistapaneladmin")      //ruta
    public String vistapaneladmin(Model model) { //nombre da igual
        return "vistapaneladmin";            //nombre de vista
    }

    @GetMapping("/autores")      //ruta
    public String autores(Model model) { //nombre da igual
        return "autores";            //nombre de vista
    }

}
