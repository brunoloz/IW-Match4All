# Proyecto IW: Match4All

Match4All es una plataforma diseñada para centralizar y profesionalizar la gestión del fútbol amateur. El proyecto nace para sustituir métodos informales (como grupos de chat u hojas de cálculo) por una herramienta que gestione ligas, amistosos y estadísticas en tiempo real, conectando a organizadores, jugadores y árbitros.

## Propósito del Proyecto
El sistema conecta a organizadores, jugadores y árbitros en un ecosistema unificado con las siguientes capacidades principales:

-   Gestión Integral: Administración de ligas, torneos, campos y horarios.
-   Matchmaking: Un algoritmo dedicado para emparejar equipos según su nivel de juego.
-   Actas Digitales: Los árbitros pueden introducir goles, tarjetas e incidencias tras cada partido.
-   Visualización de Datos: Acceso a tablas de clasificación, estadísticas personales y calendarios de próximos partidos.

## Roles y permisos

-   Administrador: Creación de competiciones y configuración del sistema.
-   Capitán: Inscripción del equipo en torneos y gestión de la plantilla
-   Jugador: Consulta de estadísticas personales y pertenencia a un equipo específico.
-   Árbitro: Perfil independiente encargado de la verificación de actas y gestión de incidencias.
-   Invitado: Acceso de solo lectura a competiciones, perfiles y estadísticas generales.

## Ficheros de interés
En la raíz del proyecto, junto al pom.xml, se encuentran herramientas clave para el despliegue y configuración:

-   deploy.py: Script de automatización en Python para desplegar la aplicación en contenedores Docker de la FDI.
-   credentials.json.template: Plantilla para configurar el acceso al servidor remoto.
-   pom.xml: Archivo de configuración de Maven que define las dependencias del proyecto
    
