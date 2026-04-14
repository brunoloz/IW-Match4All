Feature: Crear una competicion


#
#  Comprobar que se crea una competicion
#
Scenario: nombre ya existente
    Given call read('login.feature@login_admin')
    Given driver baseUrl + '/vistapaneladmin'
    And input('#name', 'comp existente')
    And input('#type', 'liga')
    And input('#size', '16')
    When submit().click(".form-submit button")
    Then match html('.error') contains 'La competicion ya existe'


#
#  Comprobar que se crea una competicion
#
Scenario: crear competicion correcto
    Given call read('login.feature@login_admin')
    Given driver baseUrl + '/vistapaneladmin'
    And input('#name', 'dummy comp')
    And input('#type', 'liga')
    And input('#size', '16')
    When submit().click(".form-submit button")
    Then match html('#competitions-body') contains 'dummy comp'
