@ignore
Feature: login en servidor

#
#  Este test funciona, pero no es de buena educación martillear una API externa
#
Scenario: login malo en github
    Given driver 'https://github.com/login'
    And input('#login_field', 'dummy')
    And input('#password', 'world')
    When submit().click("input[name=commit]")
    Then match html('.flash-error') contains 'Incorrect username or password.'
#

  Scenario: login malo en plantilla
    Given driver baseUrl + '/user/2'
    And input('#username', 'dummy')
    And input('#password', 'world')
    When submit().click(".form-signin button")
    Then karate.stop(9000)
    Then match html('.error') contains 'Error en nombre de usuario o contraseña'

  @login_capitan
    Scenario: login correcto como jugador
    Given driver baseUrl + '/login'
    And input('#username', 'hmallo')
    And input('#password', 'pass')
    When submit().click(".form-signin button")
    Then waitForUrl(baseUrl + '/user/24')

  @login_arbitro
    Scenario: login correcto como jugador
    Given driver baseUrl + '/login'
    And input('#username', 'arbitro')
    And input('#password', '1234')
    When submit().click(".form-signin button")
    Then waitForUrl(baseUrl + '/user/2')

  @login_b
  Scenario: login correcto como jugador
    Given driver baseUrl + '/login'
    And input('#username', 'jugador')
    And input('#password', 'jugador')
    When submit().click(".form-signin button")
    Then waitForUrl(baseUrl + '/user/403')

  @login_a
  Scenario: login correcto como a
    Given driver baseUrl + '/login'
    And input('#username', 'admin')
    And input('#password', '1234')
    When submit().click(".form-signin button")
    Then waitForUrl(baseUrl + '/user/1')

  @logout
  Scenario: logout after login
    Given driver baseUrl + '/login'
    And input('#username', 'admin')
    And input('#password', '1234')
    When submit().click(".form-signin button")
    Then waitForUrl(baseUrl + '/admin')
    When submit().click("{button}logout")
    Then waitForUrl(baseUrl + '/login')
