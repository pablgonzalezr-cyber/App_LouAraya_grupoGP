BEGIN TRANSACTION;
SET @poblar = NOT EXISTS (SELECT 1 FROM productos)
          AND NOT EXISTS (SELECT 1 FROM colecciones)
          AND NOT EXISTS (SELECT 1 FROM campanias);

INSERT INTO productos (id, nombre, categoria, material, precio, descripcion, ancho_cm, alto_cm, disponible, publicado)
SELECT p.*, TRUE FROM (VALUES
    (1, 'Tapiz Bohemio', 'Interiores', 'Algodón natural', 24990, 'Tapiz ornamental tejido a mano, ideal para muros interiores.', 45, 70, TRUE),
    (2, 'Arco de Matrimonio', 'Matrimonios', 'Algodón trenzado', 89990, 'Decoración colgante diseñada para ceremonias y celebraciones.', 180, 220, TRUE),
    (3, 'Colgante Artesanal', 'Interiores', 'Hilo de algodón', 18990, 'Pieza compacta ideal para espacios pequeños.', 28, 50, TRUE),
    (4, 'Decoración Festival', 'Festivales', 'Algodón y cáñamo', 45990, 'Adorno textil para ferias y ambientaciones temáticas.', 80, 120, TRUE),
    (5, 'Atrapasueños Natural', 'Interiores', 'Algodón', 16990, 'Atrapasueños decorativo con terminaciones artesanales.', 30, 60, TRUE),
    (6, 'Cortina Macramé', 'Interiores', 'Algodón grueso', 64990, 'Cortina tejida para separar y ambientar espacios.', 100, 200, FALSE),
    (7, 'Centro de Mesa Nupcial', 'Matrimonios', 'Cuerda de algodón', 32990, 'Adorno de mesa personalizado para matrimonios.', 40, 30, TRUE),
    (8, 'Guirnalda Cultural', 'Festivales', 'Algodón reciclado', 27990, 'Guirnalda textil pensada para exhibiciones y eventos.', 150, 25, TRUE)
) p WHERE @poblar;

INSERT INTO colecciones (nombre, descripcion, activa, publicada)
SELECT 'Textiles de interior', 'Tapices y colgantes tejidos a mano para el hogar.', TRUE, TRUE WHERE @poblar;

INSERT INTO coleccion_productos (coleccion_id, producto_id)
SELECT c.id, p.id FROM colecciones c CROSS JOIN productos p
WHERE @poblar AND c.nombre = 'Textiles de interior' AND p.id IN (1, 3);

INSERT INTO campanias (titulo, descripcion, inicio, termino, activa, publicada)
SELECT 'Temporada de macramé', 'Descubre los diseños de interior de esta temporada.',
       CURRENT_DATE - 1, CURRENT_DATE + 30, TRUE, TRUE WHERE @poblar;
COMMIT;
