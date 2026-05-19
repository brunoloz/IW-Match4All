package external;

import com.intuit.karate.junit5.Karate;

class ExternalRunner {
    
    @Karate.Test
    Karate testLogin() {
        return Karate.run("login").relativeTo(getClass());
    }    

    @Karate.Test
    Karate testWs() {
        return Karate.run("ws").relativeTo(getClass());
    }  

    /*@Karate.Test
    Karate testCrearEquipo() {
        return Karate.run("crearEquipo").relativeTo(getClass());
    }  

    @Karate.Test
    Karate testCrearLiga() {
        return Karate.run("crearLiga").relativeTo(getClass());
    } 

    @Karate.Test
    Karate testInscribirEquipo() {
        return Karate.run("inscribirEquipo").relativeTo(getClass());
    }

    @Karate.Test
    Karate testPartido() {
        return Karate.run("partido").relativeTo(getClass());
    }*/

    @Karate.Test
    Karate test() {
        return Karate.run("crearEquipo", "crearLiga", "inscribirEquipo", "partido").relativeTo(getClass());
    }

}
