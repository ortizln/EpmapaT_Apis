CREATE TABLE empresa_auditoria (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    actor_uuid UUID NULL,
    actor_username VARCHAR(150),
    accion VARCHAR(50) NOT NULL,
    descripcion TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_empresa_auditoria_empresa_fecha
    ON empresa_auditoria (empresa_id, created_at DESC);

INSERT INTO permiso (codigo, nombre, descripcion, categoria)
SELECT 'EMPRESA_AUDITORIA_VER', 'Ver auditoria de empresas', 'Permite consultar la auditoria administrativa de empresas y configuraciones sensibles.', 'Administracion'
WHERE NOT EXISTS (
    SELECT 1 FROM permiso WHERE codigo = 'EMPRESA_AUDITORIA_VER'
);

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo = 'EMPRESA_AUDITORIA_VER'
WHERE r.codigo = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM rol_permiso rp
    WHERE rp.rol_id = r.id
      AND rp.permiso_id = p.id
);
