package es.ucm.fdi.iw.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Equipos")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
    private long id;

    @Column(name = "nom", nullable = false)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "id_capitan")
    private User capitan;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 225)
    private String ubicacion;

    @Column(length = 225)
    private String escudo;

    @OneToMany(mappedBy = "equipo")
    private List<User> jugadores = new ArrayList<>();
}
