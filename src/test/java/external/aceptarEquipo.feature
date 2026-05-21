Feature: admin acepta equipo en competicion
    Como administrador de la aplicación
    Quiero poder aceptar la solicitud de un equipo
    Para que forme parte oficial de la competición

Background: * configure driver = { type: 'chrome', showDriverLog: true }
            * call read('login.feature@login_a')
            * delay(2000)

Scenario: Admin acepta a un equipo pendiente
    * driver baseUrl + '/paneladmin'
    * match html('title') contains 'Panel de Administración'

    * click("form[action='/competicion/aceptar'] button[type='submit']")
    * delay(2000)
    
    * waitForUrl(baseUrl + '/paneladmin')
    * match html('.alert-success') contains 'ha sido aceptado en el equipo'