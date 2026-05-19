Feature: Inscripcion de un arbitro a un partido
    Como arbitro puedo arbitrar los partidos en los que esté apuntado
    Quiero poder iniciar el partido
    Registrar los eventos que sucedan y
    Finalizar el partido

Background: * configure driver = { type: 'chrome', showDriverLog: true }
            * call read('login.feature@login_a')
            * delay(2000)

Scenario: El admin genera los partidos de una competicion

    * match html('title') contains 'Perfil de Usuario'
    
    * click("a.btn.btn-brand-blue")
    * delay(2000)
    * waitForUrl(baseUrl + '/paneladmin')
    * match html('title') contains 'Panel Admin'

    * click("//table//tbody//button[contains(., 'Generar Calendario')]")
    * delay(2000)

Scenario: Un arbitro se apunta a un partido, lo inicia, registra eventos y lo finaliza.

    * call read('login.feature@login_arbitro')
    * delay(2000)

    * click("a.btn.btn-brand-blue")
    * delay(2000)
    * waitForUrl(baseUrl + '/panelarbitro')
    * match html('title') contains 'Panel de Árbitro'   

    * click("//table//tbody//button[contains(., 'Apuntarse')]")
    * delay(2000) 
    * click("//table//tbody//a[contains(., 'Arbitrar')]")
    * delay(2000)

    * click("i.bi.bi-play-circle")
    * delay(2000)

    * click("i.bi.bi-plus-circle")
    * delay(2000)
    * select('#event-type', '{}Gol')
    * delay(1000)
    * select('#event-team', 1)
    * delay(1000)
    * select('#event-player', 4)
    * delay(1000)
    * select('#event-assister', 5)
    * input('#event-minute', '21')
    * input('#event-description', '¡Gooool del equipo local!' )
    * delay(1000)
    * click("button.btn.btn-brand-blue")
    * delay(2000)

    * click("i.bi.bi-plus-circle")
    * delay(2000)
    * select('#event-type', '{}Tarjeta Amarilla')
    * delay(1000)
    * select('#event-team', 2)
    * delay(1000)
    * select('#event-player', 6)
    * delay(1000)
    * input('#event-minute', '44')
    * input('#event-description', 'Amonestación por protestar.' )
    * delay(1000)
    * click("button.btn.btn-brand-blue")
    * delay(2000)

    * click("i.bi.bi-plus-circle")
    * delay(2000)
    * select('#event-type', '{}Tarjeta Roja')
    * delay(1000)
    * select('#event-team', 1)
    * delay(1000)
    * select('#event-player', 5)
    * delay(1000)
    * input('#event-minute', '80')
    * input('#event-description', '¡Expulsión! Entrada durísima al adversario.' )
    * delay(1000)
    * click("button.btn.btn-brand-blue")
    * delay(2000)

    * click("i.bi.bi-stop-circle")
    * delay(2000)
    * match html('title') contains 'Partido'