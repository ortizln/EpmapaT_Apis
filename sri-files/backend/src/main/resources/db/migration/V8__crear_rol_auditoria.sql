CREATE TABLE IF NOT EXISTS rol_auditoria (
    id BIGSERIAL PRIMARY KEY,
    rol_id BIGINT NOT NULL REFERENCES rol(id),
    actor_uuid UUID NULL,
    actor_username VARCHAR(150),
    accion VARCHAR(50) NOT NULL,
    descripcion TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO permiso (codigo, nombre, descripcion, categoria)
SELECT 'ROL_AUDITORIA_VER', 'Ver auditoria de roles', 'Permite consultar la auditoria administrativa de roles.', 'Seguridad'
WHERE NOT EXISTS (
    SELECT 1 FROM permiso WHERE codigo = 'ROL_AUDITORIA_VER'
);

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo = 'ROL_AUDITORIA_VER'
WHERE r.codigo = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM rol_permiso rp
      WHERE rp.rol_id = r.id
        AND rp.permiso_id = p.id
  );
