package cl.louaraya.cotizaciones.model.entity;

import cl.louaraya.cotizaciones.dto.CotizacionRequest;
import cl.louaraya.cotizaciones.model.EstadoCotizacion;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "cotizaciones", uniqueConstraints = @UniqueConstraint(
        name = "uk_cotizacion_cliente_token", columnNames = {"cliente_id", "token_envio"}))
public class Cotizacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    @Column(name = "token_envio", nullable = false, length = 36)
    private String tokenEnvio;
    @Column(nullable = false, length = 1000)
    private String descripcion;
    @Column(nullable = false, length = 50)
    private String categoria;
    @Column(nullable = false, length = 100)
    private String material;
    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal anchoCm;
    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal largoCm;
    @Column(nullable = false)
    private Integer cantidad;
    private LocalDate fechaRequerida;
    private Long productoReferenciaId; // ID del catalogo: NO FK entre microservicios
    @Column(length = 150)
    private String productoReferenciaNombre; // snapshot historico si se archiva producto
    @Column(nullable = false, updatable = false)
    private Instant creadaEn;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacion estado = EstadoCotizacion.PENDIENTE;

    protected Cotizacion() {}
    public Cotizacion(Cliente cliente, CotizacionRequest d, Instant ahora) {
        this.cliente = cliente;
        this.tokenEnvio = d.tokenEnvio();
        this.descripcion = d.descripcion().trim();
        this.categoria = d.categoria().trim();
        this.material = d.material().trim();
        this.anchoCm = d.anchoCm();
        this.largoCm = d.largoCm();
        this.cantidad = d.cantidad();
        this.fechaRequerida = d.fechaRequerida();
        this.productoReferenciaId = d.productoReferenciaId();
        this.productoReferenciaNombre = d.productoReferenciaNombre() == null ? null : d.productoReferenciaNombre().trim();
        this.creadaEn = ahora;
    }
    public boolean mismosDatos(CotizacionRequest d) {
        return descripcion.equals(d.descripcion().trim()) && categoria.equals(d.categoria().trim())
                && material.equals(d.material().trim())
                && anchoCm.compareTo(d.anchoCm()) == 0 && largoCm.compareTo(d.largoCm()) == 0
                && cantidad.equals(d.cantidad()) && Objects.equals(fechaRequerida, d.fechaRequerida())
                && Objects.equals(productoReferenciaId, d.productoReferenciaId())
                && Objects.equals(productoReferenciaNombre, d.productoReferenciaNombre() == null ? null : d.productoReferenciaNombre().trim());
    }
    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public String getTokenEnvio() { return tokenEnvio; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public String getMaterial() { return material; }
    public BigDecimal getAnchoCm() { return anchoCm; }
    public BigDecimal getLargoCm() { return largoCm; }
    public Integer getCantidad() { return cantidad; }
    public LocalDate getFechaRequerida() { return fechaRequerida; }
    public Long getProductoReferenciaId() { return productoReferenciaId; }
    public String getProductoReferenciaNombre() { return productoReferenciaNombre; }
    public Instant getCreadaEn() { return creadaEn; }
    public EstadoCotizacion getEstado() { return estado; }
    public void cambiarEstado(EstadoCotizacion siguiente) { estado = siguiente; }
}
