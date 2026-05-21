Feature: finalizar partido
    Como árbitro del partido
    Quiero poder finalizar un encuentro en curso
    Para que las estadísticas se cierren y se actualicen los rankings

Background: * configure driver = { type: 'chrome', showDriverLog: true }
            * call read('login.feature@login_c')
            * delay(2000)

Scenario: Arbitro pita el final del partido
    * driver baseUrl + '/partido/1'
    * match html('title') contains 'Partido'

    * click("#form-finalizar button[type='submit']")
    * delay(2000)
    
    * match html('.badge.bg-danger') contains 'FINALIZADO'