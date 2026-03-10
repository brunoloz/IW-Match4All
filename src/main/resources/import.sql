-- 1. Desactivamos las claves foráneas temporalmente para evitar errores de dependencias al cargar
SET REFERENTIAL_INTEGRITY FALSE;

-- 2. Cargamos los datos usando la función nativa CSVREAD de H2
INSERT INTO competicion (id, nombre, tipo, capacidad) 
SELECT id, nombre, tipo, capacidad FROM CSVREAD('classpath:competicion.csv');

INSERT INTO equipos (id, nom, escudo, id_capitan) 
SELECT id, nom, escudo, id_capitan FROM CSVREAD('classpath:equipos.csv');

INSERT INTO iwuser (id, username, password, first_name, last_name, avatar, goles, asist, t_ama, t_roj, p_jug, porimb, lesion, enabled, roles, id_equipo) 
SELECT id, username, password, first_name, last_name, avatar, goles, asistencias, tarjetas_amarillas, tarjetas_rojas, partidos_jugados, porterias_imbatidas, lesionado, enabled, roles, id_equipo FROM CSVREAD('classpath:users.csv');

INSERT INTO partidos (id, ubicacion, fecha, id_competicion, id_local, id_visitante, id_arbitro) 
SELECT id, ubicacion, fecha, id_competicion, id_local, id_visitante, id_arbitro FROM CSVREAD('classpath:partidos.csv');

SET REFERENTIAL_INTEGRITY TRUE;

-- insert admin (username a, password aa)
INSERT INTO IWUser (id, enabled, roles, username, password, goles, asist, t_ama, t_roj, p_jug, porimb, lesion)
VALUES (1, TRUE, 'ADMIN,USER', 'a', 
    '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 
    0, 0, 0, 0, 0, 0, FALSE);

INSERT INTO IWUser (id, enabled, roles, username, password, goles, asist, t_ama, t_roj, p_jug, porimb, lesion)
VALUES (2, TRUE, 'USER', 'b', 
    '{bcrypt}$2a$10$2BpNTbrsarbHjNsUWgzfNubJqBRf.0Vz9924nRSHBqlbPKerkgX.W', 
    0, 0, 0, 0, 0, 0, FALSE);

-- start id numbering from a value that is larger than any assigned above
ALTER SEQUENCE "PUBLIC"."GEN" RESTART WITH 1024;
