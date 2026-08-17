INSERT INTO permiso (codigo, nombre, descripcion, categoria)
SELECT 'USUARIO_AUDITORIA_VER', 'Ver auditoria de usuarios', 'Permite consultar la auditoria administrativa de usuarios.', 'Seguridad'
WHERE NOT EXISTS (
    SELECT 1 FROM permiso WHERE codigo = 'USUARIO_AUDITORIA_VER'
);

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo = 'USUARIO_AUDITORIA_VER'
WHERE r.codigo = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM rol_permiso rp
      WHERE rp.rol_id = r.id
        AND rp.permiso_id = p.id
  );
