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

    private string nombre;
    private Tipo tipo;
    private int capacidad;

    @ManyToMany
    @JoinColumn(name = "id_equipo")
    private int equipos;

    @OneToMany
    @JoinColumn(name = "id_partido")
    private int partidos;
}