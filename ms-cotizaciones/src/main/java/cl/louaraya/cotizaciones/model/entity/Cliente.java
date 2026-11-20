package cl.louaraya.cotizaciones.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes", uniqueConstraints = @UniqueConstraint(name = "uk_clientes_correo", columnNames = "correo"))
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 254)
    private String correo;
    @Column(nullable = false, length = 100)
    private String claveHash;

    protected Cliente() {}
    public Cliente(String correo, String claveHash) {
        this.correo = correo;
        this.claveHash = claveHash;
    }
    public Long getId() { return id; }
    public String getCorreo() { return correo; }
    public String getClaveHash() { return claveHash; }
}
