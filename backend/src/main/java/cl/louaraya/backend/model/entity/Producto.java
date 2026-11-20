package cl.louaraya.backend.model.entity;

import cl.louaraya.backend.dto.ProductoRequest;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import java.math.BigDecimal;

@Entity
@Table(name = "productos")
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_ids")
    @SequenceGenerator(name = "producto_ids", sequenceName = "producto_ids", initialValue = 9, allocationSize = 1)
    private Long id;
    @Column(nullable = false, length = 100) private String nombre;
    @Column(nullable = false, length = 2000) private String descripcion;
    @Column(nullable = false, length = 100) private String categoria;
    @Column(nullable = false, length = 100) private String material;
    @Column(nullable = false, precision = 7, scale = 2) private BigDecimal anchoCm;
    @Column(nullable = false, precision = 7, scale = 2) private BigDecimal altoCm;
    @Column(nullable = false, precision = 11, scale = 2) private BigDecimal precio;
    @Column(nullable = false) private boolean disponible;
    @Column(nullable = false) private boolean publicado;
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean archivado;

    protected Producto() {}
    public Producto(ProductoRequest datos) { actualizar(datos); }
    public void actualizar(ProductoRequest d) {
        nombre = d.nombre().strip(); descripcion = d.descripcion().strip(); categoria = d.categoria().strip(); material = d.material().strip();
        anchoCm = d.anchoCm(); altoCm = d.altoCm(); precio = d.precio(); disponible = d.disponible(); publicado = d.publicado();
    }
    public void publicar(boolean valor) { publicado = valor; }
    public void archivar() { archivado = true; }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public String getMaterial() { return material; }
    public BigDecimal getAnchoCm() { return anchoCm; }
    public BigDecimal getAltoCm() { return altoCm; }
    public BigDecimal getPrecio() { return precio; }
    public boolean isDisponible() { return disponible; }
    public boolean isPublicado() { return publicado; }
    public boolean isArchivado() { return archivado; }
}
