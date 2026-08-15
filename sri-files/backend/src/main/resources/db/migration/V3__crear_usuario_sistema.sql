CREATE TABLE IF NOT EXISTS usuario_sistema (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    username VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(300) NOT NULL,
    correo VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usuario_sistema_activo ON usuario_sistema(activo);

INSERT INTO usuario_sistema (
    uuid,
    username,
    nombre,
    correo,
    password_hash,
    password_salt,
    rol,
    activo
) SELECT *
FROM (
    VALUES (
    '11111111-1111-1111-1111-111111111111'::uuid,
    'admin',
    'Administrador SRI Files',
    'admin@sri-files.local',
    'caf4c466c238b7daa8eaa6062083185649d8a7717a97b56715af07209cbc8d28',
    '0102030405060708090a0b0c0d0e0f10',
    'ADMIN',
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
    FROM usuario_sistema
    WHERE username = 'admin'
);
