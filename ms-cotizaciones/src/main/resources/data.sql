BEGIN TRANSACTION;
SET @poblar = NOT EXISTS (SELECT 1 FROM clientes) AND NOT EXISTS (SELECT 1 FROM cotizaciones);

-- BCrypt de un secreto aleatorio descartado. Las solicitudes se pueden consultar con el administrador configurado.
INSERT INTO clientes (correo, clave_hash)
SELECT 'muestra@example.invalid', '$2a$10$8cihAcScdlQpYaxhWrrzAeV.9DRhFpUbtaiTh7wN9nTjH7Okm1Gia'
WHERE @poblar;

INSERT INTO cotizaciones (cliente_id, token_envio, descripcion, categoria, material, ancho_cm, largo_cm,
                         cantidad, creada_en, estado, producto_referencia_id, producto_referencia_nombre)
SELECT c.id, s.token, s.descripcion, 'Interiores', 'Algodón natural', 45, 70,
       1, CURRENT_TIMESTAMP, s.estado, 1, 'Tapiz Bohemio'
FROM clientes c CROSS JOIN (VALUES
    ('71fe6251-dfd6-4217-b399-81a255722a01', 'Solicito un tapiz para el salón.', 'PENDIENTE'),
    ('71fe6251-dfd6-4217-b399-81a255722a02', 'Quisiera personalizar un tapiz para el dormitorio.', 'EN_REVISION')
) s(token, descripcion, estado)
WHERE @poblar AND c.correo = 'muestra@example.invalid';
COMMIT;
