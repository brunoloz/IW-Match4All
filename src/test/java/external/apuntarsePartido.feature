Feature: arbitro se apunta a un partido
    Como árbitro del sistema
    Quiero poder apuntarme a un partido pendiente
    Para ser el encargado de rellenar el acta

Background: * configure driver = { type: 'chrome', showDriverLog: true }
            * call read('login.feature@login_c')
            * delay(2000)

Scenario: Arbitro se apunta a un partido pendiente
    * driver baseUrl + '/panelarbitro'
    * match html('title') contains 'Panel de Árbitro'

    * click("form[action='/arbitro/apuntarse'] button.btn-success")
    * delay(2000)
    
    * waitForUrl(baseUrl + '/panelarbitro')
    * match html('title') contains 'Panel de Árbitro'

    * match html('div.alert.alert-success') contains 'Te has apuntado al partido con éxito'