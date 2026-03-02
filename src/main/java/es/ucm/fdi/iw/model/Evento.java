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
public class Evento {
    
    public enum Tipo {
        GOL, 
        TARJETA_AMARILLA,
        TARJETA_ROJA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Tipo tipo;

    @ManyToOne 
    @JoinColumn(name = "id_usuario")
    private User usuario;

    @ManyToOne
    @JoinColumn(name = "id_equipo")
    private Equipo equipo;

    private java.time.LocalDateTime fecha;
}
