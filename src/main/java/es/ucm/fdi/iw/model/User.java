package es.ucm.fdi.iw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An authorized user of the system.
 */
@Entity
@Data
@NoArgsConstructor
@NamedQueries({
    @NamedQuery(name = "User.byUsername", query = "SELECT u FROM User u "
        + "WHERE u.username = :username AND u.enabled = TRUE"),
    @NamedQuery(name = "User.hasUsername", query = "SELECT COUNT(u) "
        + "FROM User u "
        + "WHERE u.username = :username"),
    @NamedQuery(name = "User.topics", query = "SELECT t.key "
        + "FROM Topic t JOIN t.members u "
        + "WHERE u.id = :id")
})
@Table(name = "IWUser")
public class User implements Transferable<User.Transfer> {

  public enum Role {
    USER, // jugador
    ADMIN, // admin users
    ARBITRO
  }

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
  @SequenceGenerator(name = "gen", sequenceName = "gen")
  private long id;

  @ManyToOne
  @JoinColumn(name="id_equipo")
  private Equipo equipo;

  @ManyToOne
  @JoinColumn(name="id_equipo_solicitado")
  private Equipo equipoSolicitado;

  @Column(nullable = false, unique = true)
  private String username;
  @Column(nullable = false)
  private String password;

  private String firstName;
  private String lastName;
  private int age;

  @Column(length = 512)
  private String avatar;

  @Column(length = 512)
  private String descripcion;

  private String posicion;

  private int goles;

  @Column(name = "asist")
  private int asistencias;
    
  @Column(name = "t_ama")
  private int tarjetasAmarillas;

  @Column(name = "t_roj")
  private int tarjetasRojas;

  @Column(name = "p_jug")
  private int partidosJugados;

  @Column(name = "porimb")
  private int porteriasImbatidas;

  @Column(name = "lesion")
  private boolean lesionado;

  private boolean enabled;
  private String roles; // split by ',' to separate roles

  @OneToMany
  @JoinColumn(name = "sender_id")
  private List<Message> sent = new ArrayList<>();
  @OneToMany
  @JoinColumn(name = "recipient_id")
  private List<Message> received = new ArrayList<>();
  @ManyToMany(mappedBy = "members")
  private List<Topic> groups = new ArrayList<>();

  /**
   * Checks whether this user has a given role.
   * 
   * @param role to check
   * @return true iff this user has that role.
   */
  public boolean hasRole(Role role) {
    String roleName = role.name();
    return Arrays.asList(roles.split(",")).contains(roleName);
  }

  @Getter
  @AllArgsConstructor
  public static class Transfer {
    private long id;
    private String username;
    private int totalReceived;
    private int totalSent;
    private String groups;
  }

  @Override
  public Transfer toTransfer() {
    StringBuilder gs = new StringBuilder();
    for (Topic g : groups) {
      gs.append(g.getName()).append(", ");
    } 
    return new Transfer(id, username, received.size(), sent.size(), gs.toString());
  }

  @Override
  public String toString() {
    return toTransfer().toString();
  }
}
