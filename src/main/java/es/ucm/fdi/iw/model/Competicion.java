package es.ucm.fdi.iw.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@ToString(exclude = {"equipos", "equiposSolicitantes", "partidos"})
@EqualsAndHashCode(exclude = {"equipos", "equiposSolicitantes", "partidos"})
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

    @ManyToMany
    @JoinTable(
        name = "competicion_solicitantes", // Nombre de la tabla intermedia que se creará
        joinColumns = @JoinColumn(name = "competicion_id"),
        inverseJoinColumns = @JoinColumn(name = "equipo_id")
    )
    private List<Equipo> equiposSolicitantes = new ArrayList<>();

    // Como en Partido.java ya definimos la relación con @ManyToOne hacia 'idCompeticion',
    // aquí solo usamos mappedBy para decirle a JPA que es una relación bidireccional.
    @OneToMany(mappedBy = "idCompeticion")
    private List<Partido> partidos = new ArrayList<>();
}