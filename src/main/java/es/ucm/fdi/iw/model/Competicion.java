package es.ucm.fdi.iw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Data
public class Competicion {

    public enum Tipo{
        LIGA,
        TORNEO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String nombre;

    @Enumerated(EnumType.STRING)
    private Tipo tipo;
    
    private int capacidad;

// Para ManyToMany he usado @JoinTable para definir la tabla intermedia
    @ManyToMany
    @JoinTable(
        name = "competicion_equipos", // Nombre de la tabla intermedia que se creará
        joinColumns = @JoinColumn(name = "competicion_id"),
        inverseJoinColumns = @JoinColumn(name = "equipo_id")
    )
    private List<Equipo> equipos = new ArrayList<>();

    // Como en Partido.java ya definimos la relación con @ManyToOne hacia 'idCompeticion',
    // aquí solo usamos mappedBy para decirle a JPA que es una relación bidireccional.
    @OneToMany(mappedBy = "idCompeticion")
    private List<Partido> partidos = new ArrayList<>();
}