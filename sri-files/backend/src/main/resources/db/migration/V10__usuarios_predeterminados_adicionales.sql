INSERT INTO usuario_sistema (
    uuid,
    username,
    nombre,
    correo,
    password_hash,
    password_salt,
    rol,
    activo
)
SELECT *
FROM (
    VALUES
    (
        '22222222-2222-2222-2222-222222222222'::uuid,
        'supervisor',
        'Supervisor SRI Files',
        'supervisor@sri-files.local',
        'caf4c466c238b7daa8eaa6062083185649d8a7717a97b56715af07209cbc8d28',
        '0102030405060708090a0b0c0d0e0f10',
        'ADMIN',
        TRUE
    ),
    (
        '33333333-3333-3333-3333-333333333333'::uuid,
        'operador',
        'Operador SRI Files',
        'operador@sri-files.local',
        'caf4c466c238b7daa8eaa6062083185649d8a7717a97b56715af07209cbc8d28',
        '0102030405060708090a0b0c0d0e0f10',
        'OPERADOR',
        TRUE
    )
) AS seed (
    uuid,
    username,
    nombre,
    correo,
    password_hash,
    password_salt,
    rol,
    activo
)
WHERE NOT EXISTS (
    SELECT 1
    FROM usuario_sistema u
    WHERE lower(u.username) = lower(seed.username)
       OR lower(u.correo) = lower(seed.correo)
);
