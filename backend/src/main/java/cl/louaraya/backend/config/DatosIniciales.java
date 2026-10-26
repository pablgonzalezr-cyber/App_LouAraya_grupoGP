package cl.louaraya.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatosIniciales implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public DatosIniciales(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        insertar(1,"Tapiz Bohemio","Interiores","Algodón natural",24990,"Tapiz ornamental tejido a mano, ideal para muros interiores.",45,70,true);
        insertar(2,"Arco de Matrimonio","Matrimonios","Algodón trenzado",89990,"Decoración colgante diseñada para ceremonias y celebraciones.",180,220,true);
        insertar(3,"Colgante Artesanal","Interiores","Hilo de algodón",18990,"Pieza compacta ideal para espacios pequeños.",28,50,true);
        insertar(4,"Decoración Festival","Festivales","Algodón y cáñamo",45990,"Adorno textil para ferias y ambientaciones temáticas.",80,120,true);
        insertar(5,"Atrapasueños Natural","Interiores","Algodón",16990,"Atrapasueños decorativo con terminaciones artesanales.",30,60,true);
        insertar(6,"Cortina Macramé","Interiores","Algodón grueso",64990,"Cortina tejida para separar y ambientar espacios.",100,200,false);
        insertar(7,"Centro de Mesa Nupcial","Matrimonios","Cuerda de algodón",32990,"Adorno de mesa personalizado para matrimonios.",40,30,true);
        insertar(8,"Guirnalda Cultural","Festivales","Algodón reciclado",27990,"Guirnalda textil pensada para exhibiciones y eventos.",150,25,true);
    }
    private void insertar(long id, String nombre, String categoria, String material, int precio, String descripcion, int ancho, int alto, boolean disponible) {
        jdbc.update("INSERT INTO productos (id,nombre,categoria,material,precio,descripcion,ancho_cm,alto_cm,disponible,publicado) SELECT ?,?,?,?,?,?,?,?,?,TRUE WHERE NOT EXISTS (SELECT 1 FROM productos WHERE id=?)",
            id,nombre,categoria,material,precio,descripcion,ancho,alto,disponible,id);
    }
}
