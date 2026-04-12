package es.ucm.fdi.iw.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Clasificacion")
public class Clasificacion {
    
 @Id
 @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
 @SequenceGenerator(name = "gen", sequenceName = "gen")
 private long id;

 @ManyToOne
 private Competicion competicion;
 
 @ManyToOne
 private Equipo equipo;

 private int puntos;
 private int partidos_jugados;
 private int victorias;
 private int empates;
 private int derrotas;
 private int goles_a_favor;
 private int goles_en_contra;

}
