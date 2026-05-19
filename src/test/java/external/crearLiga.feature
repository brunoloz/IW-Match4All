Feature: creacion de una competicion en modo LIGA
    Como administrador de la aplicación
    Quiero poder crear una LIGA nueva

Background: * configure driver = { type: 'chrome', showDriverLog: true }
            * call read('login.feature@login_a')
            * delay(2000)

Scenario: El administrador crea una liga

    * match html('title') contains 'Perfil de Usuario'
    
    * click("a.btn.btn-brand-blue")
    * delay(2000)
    * waitForUrl(baseUrl + '/paneladmin')
    * match html('title') contains 'Panel Admin'

    * click("button.btn.btn-brand-blue")
    * delay(2000)


    * input('#competition-name', 'Liga de Verano')
    * select('#competition-type', '{}Liga (Round Robin)')
    * clear('#competition-size')
    * input('#competition-size', '3')

    * click("button.btn.btn-brand-blue[type=submit]")
    * delay(2000)
    * waitForUrl(baseUrl + '/paneladmin')
    * match html('title') contains 'Panel Admin'

