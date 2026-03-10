Feature: creacion de un acta

#
#El usuario arbitro se logeara en primer lugar para poder acceder
#a la opcion de creacion de actas.
#

Scenario: creacion de un acta exitoso
    Given call read('login.feature@login_arbitro')
    Given driver baseUrl + '/user/1'
    And input('#username', 'arbitro')
    And input('#password', 'arbitro123')
    And click("button[id=acta-creation]")
    And input('#acta-name', 'Acta de prueba')
    And click("button[id=create-acta]")
    Then match html('#acta-name') contains 'Acta de prueba'

Scenario: creacion de un acta pero ya esta creada
    Given call read('login.feature@login_arbitro')
    Given driver baseUrl + '/user/1'
    And input('#username', 'arbitro')
    And input('#password', 'arbitro123')
    And click("button[id=acta-creation]")
    And input('#acta-name', 'Acta de prueba')
    And click("button[id=create-acta]")
    Then match html('#.error') contains '"Acta de prueba" ya existe'

Scenario: creacion de un acta pero nombre no es valido
    Given call read('login.feature@login_arbitro')
    Given driver baseUrl + '/user/1'
    And input('#username', 'arbitro')
    And input('#password', 'arbitro123')
    And click("button[id=acta-creation]")
    And input('#acta-name', 'Acta de prueba')
    And click("button[id=create-acta]")
    Then match html('#.error') contains '"Acta de prueba" no es válido'