package cl.louaraya.backend.exception;

public class ProductoNoEncontrado extends RuntimeException {
    public ProductoNoEncontrado() { super("Producto no encontrado o no publicado"); }
}
