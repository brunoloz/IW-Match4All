# Proyecto IW: Match4All

Match4All es una plataforma diseñada para centralizar y profesionalizar la gestión del fútbol amateur. El proyecto nace para sustituir métodos informales (como grupos de chat u hojas de cálculo) por una herramienta que gestione ligas, torneos y estadísticas en tiempo real, conectando a organizadores, jugadores y árbitros.

## Propósito del Proyecto
El sistema conecta a organizadores, jugadores y árbitros en un ecosistema unificado con las siguientes capacidades principales:

- **Gestión Integral:** Administración de ligas, torneos, equipos y horarios.
- **Matchmaking:** Un algoritmo dedicado para emparejar equipos en una competición.
- **Actas Digitales:** Los árbitros pueden introducir goles, tarjetas e incidencias durante cada partido.
- **Visualización de Datos:** Acceso a tablas de clasificación, estadísticas personales e informaación de partidos.

## Roles y permisos

- **Administrador:** Creación de competiciones y configuración del sistema.
- **Capitán:** Inscripción del equipo en torneos y gestión de la plantilla.
- **Jugador:** Consulta de estadísticas personales y pertenencia a un equipo específico.
- **Árbitro:** Perfil independiente encargado de la verificación de actas y gestión de incidencias.

## Estructura de la base de datos
<img width="4540" height="2428" alt="bd_match4all" src="https://github.com/user-attachments/assets/ec376b04-6c00-4a18-9b79-46795b605ade" />

## Estado actual de la aplicación (Entrega 12 de mayo)

### Funcionalidades generales
En nuestra aplicación, cualquier usuario que realice el login tendrá acceso a:
- **Consulta de competiciones:** En la sección 'Competiciones, el usuario puede consultar la lista de competiciones disponibles y acceder a toda su información correspondiente (clasificación, brackets, estadísticas, partidos...). Además, existe la posibilidad de hacer una búsqueda a través de una barra de navegación. 
- **Consulta de equipos:** Sección similar a la de 'Competiciones', pero con la lista de los equipos existentes en la aplicación.
- **Consulta de partidos:** Todos los usuarios tienen acceso a la información de un partido. En la página del partido se puede visualizar el marcador, las alineaciones de ambos equipos, y todos los eventos registrados por el árbitro.
- **Consulta de usuarios:** La información de todos los usuarios de la aplicación es visible por todos al visitar la página de su perfil. En su perfil se muestran las estadísticas generales del usuario (goles, amonestaciones, partidos jugados...) y su información personal.

### Funcionalidades designadas al Capitán (Jugador)
- **Crear un equipo nuevo:** Un usuario (jugador) sin equipo puede crear su propio equipo y convertirse en su capitán.
- **Inscripción de un equipo en una competición:** El capitán del equipo puede solicitar la inscripción de su equipo a cualquier competición disponible. Será el administrador quién acepte la solicitud.
- **Aceptar la inscripción de un jugador en un equipo:** Un usuario (jugador) sin equipo puede solicitar inscribirse a cualquier equipo ya creado. Será el capitán del equipo quien acepte la solicitud.
- **Asignar titularidades:** Como capitán, puede decidir que jugadores de la plantilla adquieren el rol de suplente o de titular.

### Funcionalidades designadas al Árbitro
Un usuario registrado con el rol de árbitro puede:
- **Apuntarse a un partido:** Desde el panel de árbitro (solo visible para él), el árbitro tendrá acceso a una lista de todos los partidos pendientes de cada competición. Como árbitro, podrá apuntarse a un partido y arbitrarlo.
- **Registrar Eventos:** Como árbitro tiene el poder de actualizar la información del partido en directo, y registrar todos los eventos que puedan suceder (goles, tarjetas...). La información del partido tiene una implicación directa con la competición a la que pertenece, es decir, cada evento que registre el árbitro modificará automáticamente (websockets) tanto la información del partido como la información de la competición (ej: tabla de clasificación de una LIGA).

### Funcionalidades designadas al Administrador
Un usuario con el rol de admin puede:
- **Crear una competicion nueva:** El administrador puede crear una nueva competición desde el panel de admin.
- **Generar Calendario de Partidos:** Desde el panel del admin, puede generar el calendario de partidos de aquellas competiciones cuya fase de inscripción haya finalizado.
- **Eliminar Equipos y Competiciones.**
- **Deshabilitar usuarios.**

### Pruebas Externas
Actualmente sólo hemos elaborado la prueba externa de Crear un Equipo en el archivo crearEquipo.feauture. El resto de pruebas se implementarán para la próxima entrega.
Para probarla tiene que estar la aplicación lanzada y ejecutar el comando mvn test -Dtest=ExternalRunner en la terminal de Visual Studio Code.

### Despliegue de la aplicación
Aplicación desplegada correctamente en el contenedor proporcionado para la asignatura.
[vm041.containers.fdi.ucm.es](https://vm041.containers.fdi.ucm.es/)

### Lista de Usuarios
Usuarios con el rol de capitán:
- Username: kgarcia  Password: pass
- Username: hmallo  Password: pass
- Username: sramos  Password: pass
- Username: lmessi  Password: pass

Usuarios con el rol de árbitro:
- Username: arbitro  Password: 1234
- Username: arbitro2  Password: arbitro2

Usuarios con el rol de jugador (sin equipo):
- Username: jugador  Password: jugador
- Username: jugador2  Password: jugador2

Administrador:
- Username: admin  Password: 1234  
