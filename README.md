# Proyecto IW: Match4All

Match4All es una plataforma diseñada para centralizar y profesionalizar la gestión del fútbol amateur. El proyecto nace para sustituir métodos informales (como grupos de chat u hojas de cálculo) por una herramienta que gestione ligas, amistosos y estadísticas en tiempo real, conectando a organizadores, jugadores y árbitros.

## Propósito del Proyecto
El sistema conecta a organizadores, jugadores y árbitros en un ecosistema unificado con las siguientes capacidades principales:

- **Gestión Integral:** Administración de ligas, torneos, campos y horarios.
- **Matchmaking:** Un algoritmo dedicado para emparejar equipos según su nivel de juego.
- **Actas Digitales:** Los árbitros pueden introducir goles, tarjetas e incidencias tras cada partido.
- **Visualización de Datos:** Acceso a tablas de clasificación, estadísticas personales y calendarios de próximos partidos.

## Roles y permisos

- **Administrador:** Creación de competiciones y configuración del sistema.
- **Capitán:** Inscripción del equipo en torneos y gestión de la plantilla.
- **Jugador:** Consulta de estadísticas personales y pertenencia a un equipo específico.
- **Árbitro:** Perfil independiente encargado de la verificación de actas y gestión de incidencias.
- **Invitado:** Acceso de solo lectura a competiciones, perfiles y estadísticas generales.

## Ficheros de interés
En la raíz del proyecto, junto al `pom.xml`, se encuentran herramientas clave para el despliegue y configuración:

- **deploy.py:** Script de automatización en Python para desplegar la aplicación en contenedores Docker de la FDI.
- **credentials.json.template:** Plantilla para configurar el acceso al servidor remoto.
- **pom.xml:** Archivo de configuración de Maven que define las dependencias del proyecto.

## Estado actual de la aplicación (Entrega Intermedia 25 de marzo)

### Funcionalidades
Actualmente en nuestra aplicación hemos implementado 4 funcionalidades:
- **Crear un equipo nuevo:** Un usuario (jugador) sin equipo puede crear su propio equipo y convertirse en su capitán.
- **Inscripción de un equipo en una competición:** El capitán del equipo puede solicitar la inscripción de su equipo a cualquier competición disponible. Será el administrador quién acepte la solicitud.
- **Inscripción de un jugador en un equipo:** Un usuario (jugador) sin equipo puede solicitar inscribirse a cualquier equipo ya creado. Será el capitán del equipo quien acepte la solicitud.
- **Crear una competicion nueva:** El administrador puede crear una nueva competición desde el panel de admin.

### Funcionamiento de las vistas

- En **user.html**, si te registras como jugador tendrás acceso a esta vista. En la parte izquierda encontramos los datos del usuario, y en la parte derecha un botón de "Crear tu propio equipo" y otro botón de "Inscribirte a un equipo" (ambos funcionan), éstos dos botones solo serán visibles si el jugador no forma parte de ningún equipo.

<img width="1916" height="867" alt="vistaperfiljugador" src="https://github.com/user-attachments/assets/8a31c298-7421-4bc3-a187-01ae863919ef" />

- Si pulsamos el botón de "Crear tu propio equipo" accedemos a **vistacrearequipo.html**. (Todo funciona correctamente)

<img width="1919" height="1079" alt="vistacrearequipo" src="https://github.com/user-attachments/assets/d859e03f-636e-4368-ae3c-a9c73ef3a402" />

- En cambio, si decidimos pulsar "Inscribirte a un equipo", accedemos a **vistalistaequipos.html**. En esta vista se muestra una lista con los equipos presentes en nuestra base de datos. Los botones de "Ver Detalles" y "Solicitar inscripción" funcionan correctamente. El buscador de competiciones aún no funciona.

<img width="1918" height="1078" alt="vistalistaequipos" src="https://github.com/user-attachments/assets/7ec1190c-0693-4034-b995-900497a13089" />

- Toda la información relativa a un equipo se encuentra en la página **vistagestionequipo.html**. Esta página será visible para todo el mundo, sin embargo, será el capitán del equipo el único con poderes para gestionar su equipo desde aquí. Por ejemplo, la sección de solicitudes de ingreso solo es visible para el capitán. (Todo en esta vista funciona correctamente).

<img width="1915" height="806" alt="vistagestionequipo" src="https://github.com/user-attachments/assets/b782daee-6309-4df6-9e6e-07b7584ccea1" />

<img width="1916" height="811" alt="jugadoraceptado" src="https://github.com/user-attachments/assets/d126cb23-de67-4922-a132-ee947e8353e7" />

- Al panel de administración, presente en la página **vistapaneladmin.html**, solo tiene acceso el administrador. En ella se puede consultar la información de las tablas de Competición, Equipo y Jugador, y realizar acciones sobre ellas. Funciona: La creación de una nueva competición y el buscador. No funciona: Los botones de "Eliminar" y "Deshabilitar".

<img width="1919" height="1079" alt="vistapaneladmin" src="https://github.com/user-attachments/assets/714e06ac-99be-4844-bb64-1fc8bc0e78bd" />

- Si pulsamos el botón de "Crear Competición" aparece una ventana donde rellenar los datos.

<img width="1125" height="636" alt="ventanacrearcompeticion" src="https://github.com/user-attachments/assets/434e01b2-59e0-4db2-bcfe-a5fcdf1c4aaf" />

- En la página **vistalistacompeticiones.html**, podemos consultar la lista de competiciones activas. Si eres capitán de un equipo, podrás ver el botón de "Inscribir Equipo". (Funciona todo menos el buscador).

<img width="1917" height="865" alt="vistalistacompeticiones" src="https://github.com/user-attachments/assets/4a50b7b9-c5bd-49b5-b71e-d320cf61cc16" />

- El admin desde **vistapaneladmin.html** podrá aceptar la solicitud. (Funciona correctamente)

<img width="1114" height="249" alt="solicitudcompeticion" src="https://github.com/user-attachments/assets/0745bd33-3b5b-404a-b27b-541d989de11d" />

<img width="1106" height="305" alt="competicion aceptada" src="https://github.com/user-attachments/assets/7a7cc372-dd9f-4e17-9518-8e8dc327a9bc" />
