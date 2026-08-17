CREATE TABLE IF NOT EXISTS permiso (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rol (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rol_permiso (
    rol_id BIGINT NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES permiso(id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

INSERT INTO permiso (codigo, nombre, descripcion, categoria)
SELECT seed.codigo, seed.nombre, seed.descripcion, seed.categoria
FROM (
    VALUES
      ('DASHBOARD_VER', 'Ver dashboard', 'Permite consultar indicadores y resumenes operativos.', 'Dashboard'),
      ('DOCUMENTO_VER', 'Ver documentos', 'Permite consultar la bandeja documental y sus detalles.', 'Documentos'),
      ('DOCUMENTO_CREAR', 'Crear documentos', 'Permite registrar nuevas recepciones documentales.', 'Documentos'),
      ('DOCUMENTO_AUTORIZACION_CONSULTAR', 'Consultar autorizacion', 'Permite lanzar consultas manuales al flujo de autorizacion.', 'Documentos'),
      ('DOCUMENTO_CORREO_REENVIAR', 'Reenviar correo', 'Permite reenviar comprobantes por correo.', 'Documentos'),
      ('DOCUMENTO_AUDITORIA_VER', 'Ver auditoria documental', 'Permite consultar el historial reciente de cambios documentales.', 'Control'),
      ('MONITOREO_VER', 'Ver monitoreo', 'Permite acceder al estado operativo del backend.', 'Control'),
      ('CONTROL_ERRORES_VER', 'Ver control de errores', 'Permite consultar incidencias del flujo.', 'Control'),
      ('CONTROL_CORREOS_VER', 'Ver control de correos', 'Permite revisar la operacion de notificaciones.', 'Control'),
      ('CATALOGO_ADMINISTRAR', 'Administrar catalogos', 'Permite gestionar empresas, establecimientos, puntos y secuenciales.', 'Administracion'),
      ('CERTIFICADO_ADMINISTRAR', 'Administrar certificados', 'Permite cargar y gestionar certificados.', 'Administracion'),
      ('CONFIGURACION_CORREO_ADMINISTRAR', 'Administrar configuracion', 'Permite editar configuracion SRI y correo.', 'Administracion'),
      ('USUARIO_VER', 'Ver usuarios', 'Permite consultar el listado de usuarios.', 'Seguridad'),
      ('USUARIO_CREAR', 'Crear usuarios', 'Permite registrar nuevos usuarios.', 'Seguridad'),
      ('USUARIO_EDITAR', 'Editar usuarios', 'Permite actualizar estado y credenciales de usuarios.', 'Seguridad'),
      ('ROL_VER', 'Ver roles', 'Permite consultar la matriz de roles y permisos.', 'Seguridad'),
      ('ROL_ADMINISTRAR', 'Administrar roles', 'Permite definir la asignacion de permisos por rol.', 'Seguridad')
) AS seed(codigo, nombre, descripcion, categoria)
WHERE NOT EXISTS (
    SELECT 1 FROM permiso p WHERE p.codigo = seed.codigo
);

INSERT INTO rol (codigo, nombre, descripcion)
SELECT seed.codigo, seed.nombre, seed.descripcion
FROM (
    VALUES
      ('ADMIN', 'Administrador', 'Control total del sistema, configuracion y seguridad.'),
      ('OPERADOR', 'Operador', 'Gestion operativa documental sin acceso a configuraciones sensibles.')
) AS seed(codigo, nombre, descripcion)
WHERE NOT EXISTS (
    SELECT 1 FROM rol r WHERE r.codigo = seed.codigo
);

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM rol_permiso rp
      WHERE rp.rol_id = r.id
        AND rp.permiso_id = p.id
  );

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN (
    'DASHBOARD_VER',
    'DOCUMENTO_VER',
    'DOCUMENTO_CREAR',
    'DOCUMENTO_AUTORIZACION_CONSULTAR',
    'DOCUMENTO_CORREO_REENVIAR'
)
WHERE r.codigo = 'OPERADOR'
  AND NOT EXISTS (
      SELECT 1
      FROM rol_permiso rp
      WHERE rp.rol_id = r.id
        AND rp.permiso_id = p.id
  );
