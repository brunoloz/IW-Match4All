Feature: creacion de un equipo

#
#El usuario capitan se logeara en primer lugar para poder acceder
#a la opcion de creacion de equipo.
#

Scenario: creacion de un equipo exitoso
    Given call read('login.feature@login_capitan')
    Given driver baseUrl + '/user/1'
    And input('#username', 'capitan')
    And input('#password', 'capitan123')
    And click("button[id=team-creation]")
    And input('#team-name', 'Equipo de prueba')
    And click("button[id=create-team]")
    Then match html('#team-name') contains 'Equipo de prueba'

Scenario: creacion de un equipo pero nombre ya existe
    Given call read('login.feature@login_capitan')
    Given driver baseUrl + '/user/1'
    And input('#username', 'capitan')
    And input('#password', 'capitan123')
    And click("button[id=team-creation]")
    And input('#team-name', 'Equipo de prueba')
    And click("button[id=create-team]")
    Then match html('#.error') contains '"Equipo de prueba" ya existe'

Scenario: creacion de un equipo pero nombre no es valido
    Given call read('login.feature@login_capitan')
    Given driver baseUrl + '/user/1'
    And input('#username', 'capitan')
    And input('#password', 'capitan123')
    And click("button[id=team-creation]")
    And input('#team-name', 'Equipo de prueba')
    And click("button[id=create-team]")
    Then match html('#.error') contains '"Equipo de prueba" no es válido'