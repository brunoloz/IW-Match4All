Feature: creacion de un equipo
    Como usuario de la aplicación
    Quiero poder crear un equipo nuevo
    Para convertirme en su capitán y gestionarlo

Background: * configure driver = { type: 'chrome', showDriverLog: true }
            * call read('login.feature@login_b')
            * delay(2000)

Scenario: Un usuario logueado sin equipo crea un equipo exitosamente

    * match html('title') contains 'Perfil de Usuario'

    * click("a.btn.btn-brand-blue")
    * delay(2000)
    * waitForUrl(baseUrl + '/vistacrearequipo')
    * match html('title') contains 'Crear Equipo'

    * input('#nombre', 'Getafe CF')
    * input('#escudo', 'escudogetafe.png')
    * input('#descripcion', 'Equipo luchador y con garra')
    * input('#ubicacion', 'Getafe, Madrid')

    * click("button.btn.btn-brand-blue[type=submit]")
    * delay(2000)
    * waitForUrl(baseUrl + '/vistagestionequipo')
    * match html('title') contains 'Gestión del Equipo'
