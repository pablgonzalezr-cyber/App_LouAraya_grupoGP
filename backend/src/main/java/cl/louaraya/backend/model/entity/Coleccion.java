package cl.louaraya.backend.model.entity;

import cl.louaraya.backend.dto.ColeccionRequest;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "colecciones")
public class Coleccion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false, length = 2000)
    private String descripcion;
    @Column(nullable = false)
    private boolean activa;
    @Column(nullable = false)
    private boolean publicada;

    @ManyToMany
    @JoinTable(name = "coleccion_productos",
        joinColumns = @JoinColumn(name = "coleccion_id"),
        inverseJoinColumns = @JoinColumn(name = "producto_id"),
        indexes = @Index(columnList = "producto_id"))
    private Set<Producto> productos = new HashSet<>();

    protected Coleccion() {}

    public Coleccion(ColeccionRequest datos, Set<Producto> miembros) {
        actualizar(datos, miembros);
    }

    public void actualizar(ColeccionRequest datos, Set<Producto> miembros) {
        nombre = datos.nombre();
        descripcion = datos.descripcion();
        activa = datos.activa();
        publicada = datos.publicada();
        productos.clear();
        productos.addAll(miembros);
    }

    public void publicar(boolean valor) { publicada = valor; }
    public void desactivar() { activa = false; }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isActiva() { return activa; }
    public boolean isPublicada() { return publicada; }
    public Set<Producto> getProductos() { return productos; }
}
