package cl.louaraya.backend.model.entity;

import cl.louaraya.backend.dto.CampaniaRequest;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "campanias")
public class Campania {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String titulo;
    @Column(nullable = false, length = 2000)
    private String descripcion;
    @Column(nullable = false)
    private LocalDate inicio;
    @Column(nullable = false)
    private LocalDate termino;
    @Column(nullable = false)
    private boolean publicada;
    @Column(nullable = false)
    private boolean activa;

    protected Campania() {}

    public Campania(CampaniaRequest datos) { actualizar(datos); }

    public void actualizar(CampaniaRequest datos) {
        titulo = datos.titulo();
        descripcion = datos.descripcion();
        inicio = datos.inicio();
        termino = datos.termino();
        publicada = datos.publicada();
        activa = datos.activa();
    }

    public void publicar(boolean valor) { publicada = valor; }
    public void desactivar() { activa = false; }
    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public LocalDate getInicio() { return inicio; }
    public LocalDate getTermino() { return termino; }
    public boolean isPublicada() { return publicada; }
    public boolean isActiva() { return activa; }
}
