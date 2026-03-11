Feature: Inscripción de un Equipo en una Competición

  Background:
    * url baseUrl
    * def loginResponse = call read('classpath:external/login.feature@login_a')
    * def authToken = loginResponse.responseHeaders['Set-Cookie'][0].split(';')[0]

  Scenario: Inscripción exitosa por el capitán del equipo
    Given path '/api/competiciones/1/inscribir-equipo'
    And header Cookie = authToken
    And request { equipoId: 1 }
    When method POST
    Then status 200
    And match response contains { mensaje: 'Equipo inscrito exitosamente' }

  Scenario: Intento de inscripción por un usuario que no es capitán
    Given path '/api/competiciones/1/inscribir-equipo'
    And header Cookie = call read('classpath:external/login.feature@login_b').responseHeaders['Set-Cookie'][0].split(';')[0]
    And request { equipoId: 1 }
    When method POST
    Then status 403
    And match response contains { error: 'Solo el capitán puede inscribir al equipo' }

  Scenario: Intento de inscripción de un equipo ya inscrito en la competición
    Given path '/api/competiciones/1/inscribir-equipo'
    And header Cookie = authToken
    And request { equipoId: 1 }
    When method POST
    Then status 400
    And match response contains { error: 'El equipo ya está inscrito en esta competición' }

