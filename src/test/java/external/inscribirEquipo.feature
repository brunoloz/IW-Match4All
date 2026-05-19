Feature: inscripcion de un equipo a una competicion
    Como capitán de un equipo
    Quiero poder inscribir a mi equipo en alguna 
    competición disponible

Background: * configure driver = { type: 'chrome', showDriverLog: true }
            * call read('login.feature@login_capitan')
            * delay(2000)

Scenario: Un capitan inscribe a su equipo exitosamente

    * match html('title') contains 'Perfil de Usuario'

    * click("a.nav-link.comp")
    * delay(2000)
    * waitForUrl(baseUrl + '/listacompeticiones')

    * click("//div[contains(@class, 'competicion-card')][.//h2[@class='competicion-title' and text()='Liga de Verano']]//button[contains(., 'Inscribir Equipo')]")
    * delay(2000)
    * waitForUrl(baseUrl + '/listacompeticiones')
    * match html('title') contains 'Listado de Competiciones'