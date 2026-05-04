package es.ucm.fdi.iw.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Estadisticas_Jugador", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"competicion_id", "jugador_id"})
})
public class EstadisticasJugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "competicion_id", nullable = false)
    private Competicion competicion;

    @ManyToOne
    @JoinColumn(name = "jugador_id", nullable = false)
    private User jugador;

    private int goles = 0;
    private int asistencias = 0;
    private int tarjetasAmarillas = 0;
    private int tarjetasRojas = 0;
    private int partidosJugados = 0;
}