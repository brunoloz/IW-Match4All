package es.ucm.fdi.iw.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Partidos")
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
    private long id;

    @ManyToOne
    @JoinColumn(name = "id_local", nullable = false)
    private Equipo local;

    @ManyToOne
    @JoinColumn(name = "id_visitante", nullable = false)
    private Equipo visitante;

    @Column(name = "ubicacion", nullable = false, length = 225)
    private String ubicacion;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne
    @JoinColumn(name = "id_competicion", nullable = false)
    private Competicion idCompeticion;

    @ManyToOne
    @JoinColumn(name = "id_arbitro", nullable = false)
    private User arbitro;
}
