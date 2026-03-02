package es.ucm.fdi.iw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.ucm.fdi.iw.model.User;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Eventos")
public class Evento {
    
    public enum Tipo {
        GOL, 
        TARJETA_AMARILLA,
        TARJETA_ROJA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "gen", sequenceName = "gen")
    private long id;

    @Column(name = "tipo", nullable = false)
    private Tipo tipo;

    @ManyToOne 
    @JoinColumn(name = "id_usuario")
    private User usuario;

    @ManyToOne
    @JoinColumn(name = "id_equipo")
    private Equipo equipo;

    private java.time.LocalDateTime fecha;
}
