Feature: registro de eventos en acta
    Como árbitro asignado a un partido
    Quiero registrar un gol con su asistente
    Para que se actualice el marcador y las estadísticas

Background: * configure driver = { type: 'chrome', showDriverLog: true }
            * call read('login.feature@login_c')
            * delay(2000)

Scenario: Arbitro registra un gol con asistente en un partido pendiente
    * driver baseUrl + '/partido/1'
    * match html('title') contains 'Partido'

    * click("#btn-create-comp")
    * delay(1000)

    * select('#event-type', '{}Gol')
    * select('#event-team', '{}Equipo local')
    * delay(1000)
    * select('#event-player', '{}Jugador 1 del Equipo A')
    * select('#event-assister', '{}Jugador 2 del Equipo A')
    * input('#event-minute', '15')

    * click("#form-evento button[type='submit']")
    * delay(2000)
    
    * match html('#timeline-eventos') contains 'Jugador 1 del Equipo A'