Feature: generación de calendario
    Como administrador de la aplicación
    Quiero generar el calendario de una competición
    Para que los equipos puedan empezar a jugar

Background: * configure driver = { type: 'chrome', showDriverLog: true }
            * call read('login.feature@login_a')
            * delay(2000)

Scenario: Admin genera calendario de liga en fase de inscripcion
    * driver baseUrl + '/paneladmin'
    * match html('title') contains 'Panel de Administración'

    * click("button.btn.btn-brand-blue[type=submit]")
    * delay(2000)
    * waitForUrl(baseUrl + '/paneladmin')
    * match html('title') contains 'Panel Admin'
    
    * match html('div.alert.alert-danger') contains 'No se han inscrito suficientes equipos para generar el calendario'