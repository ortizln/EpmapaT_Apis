-- SRI-FILES
-- Script base completo para PostgreSQL
-- Fecha de generacion: 2026-08-24
-- Incluye:
-- 1. Esquema administrativo actual de sri-files
-- 2. Tablas legacy necesarias para emision de facturacion electronica independiente

BEGIN;

-- =========================================================
-- EXTENSIONES
-- =========================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- NUCLEO SRI-FILES
-- =========================================================

CREATE TABLE IF NOT EXISTS empresa (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    ruc VARCHAR(13) NOT NULL UNIQUE,
    razon_social VARCHAR(300) NOT NULL,
    nombre_comercial VARCHAR(300),
    direccion_matriz VARCHAR(500),
    obligado_contabilidad BOOLEAN NOT NULL DEFAULT FALSE,
    contribuyente_especial VARCHAR(50),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    sri_ambiente SMALLINT NOT NULL DEFAULT 1,
    correo_notificaciones VARCHAR(320),
    correo_respuesta VARCHAR(320),
    certificado_nombre VARCHAR(255),
    certificado_pkcs12 BYTEA,
    certificado_clave VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS establecimiento (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    codigo VARCHAR(3) NOT NULL,
    nombre VARCHAR(200),
    direccion VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_establecimiento UNIQUE (empresa_id, codigo)
);

CREATE TABLE IF NOT EXISTS punto_emision (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    establecimiento_id BIGINT NOT NULL REFERENCES establecimiento(id),
    codigo VARCHAR(3) NOT NULL,
    nombre VARCHAR(200),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_punto_emision UNIQUE (establecimiento_id, codigo)
);

CREATE TABLE IF NOT EXISTS secuencial (
    id BIGSERIAL PRIMARY KEY,
    punto_emision_id BIGINT NOT NULL REFERENCES punto_emision(id),
    tipo_documento VARCHAR(30) NOT NULL,
    valor_actual BIGINT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_secuencial UNIQUE (punto_emision_id, tipo_documento)
);

CREATE TABLE IF NOT EXISTS documento_electronico (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    establecimiento_id BIGINT REFERENCES establecimiento(id),
    punto_emision_id BIGINT REFERENCES punto_emision(id),
    tipo_documento VARCHAR(30) NOT NULL,
    ambiente SMALLINT NOT NULL,
    estado_actual VARCHAR(40) NOT NULL,
    external_id VARCHAR(150),
    idempotency_key VARCHAR(200),
    codigo_documento VARCHAR(2),
    establecimiento VARCHAR(3),
    punto_emision VARCHAR(3),
    secuencial VARCHAR(9),
    numero_documento VARCHAR(30),
    clave_acceso VARCHAR(49),
    fecha_emision DATE NOT NULL,
    identificacion_receptor VARCHAR(20),
    razon_social_receptor VARCHAR(300),
    email_receptor VARCHAR(320),
    moneda VARCHAR(10),
    subtotal NUMERIC(18, 6),
    descuento NUMERIC(18, 6),
    impuestos NUMERIC(18, 6),
    total NUMERIC(18, 6),
    json_original JSONB NOT NULL,
    numero_autorizacion VARCHAR(100),
    fecha_autorizacion TIMESTAMP,
    mensaje_sri TEXT,
    requiere_intervencion BOOLEAN NOT NULL DEFAULT FALSE,
    intentos_procesamiento INTEGER NOT NULL DEFAULT 0,
    fecha_recepcion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio_procesamiento TIMESTAMP,
    fecha_finalizacion TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_documento_external UNIQUE (empresa_id, external_id),
    CONSTRAINT uk_documento_idempotency UNIQUE (empresa_id, idempotency_key),
    CONSTRAINT uk_documento_numero UNIQUE (empresa_id, codigo_documento, establecimiento, punto_emision, secuencial)
);

CREATE TABLE IF NOT EXISTS documento_estado_historial (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL REFERENCES documento_electronico(id),
    estado_anterior VARCHAR(40),
    estado_nuevo VARCHAR(40) NOT NULL,
    descripcion TEXT,
    origen VARCHAR(30),
    usuario_id BIGINT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS documento_archivo (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL REFERENCES documento_electronico(id),
    tipo_archivo VARCHAR(30) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    ruta TEXT,
    hash_sha256 VARCHAR(64),
    tamanio BIGINT,
    version INTEGER NOT NULL DEFAULT 1,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS documento_error (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL REFERENCES documento_electronico(id),
    etapa VARCHAR(40) NOT NULL,
    codigo VARCHAR(100),
    mensaje TEXT NOT NULL,
    detalle TEXT,
    stack_trace TEXT,
    recuperable BOOLEAN NOT NULL DEFAULT FALSE,
    resuelto BOOLEAN NOT NULL DEFAULT FALSE,
    resuelto_por BIGINT,
    fecha_resolucion TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE IF NOT EXISTS usuario_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario_sistema(id),
    actor_uuid UUID,
    actor_username VARCHAR(80),
    accion VARCHAR(40) NOT NULL,
    descripcion TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE IF NOT EXISTS rol_auditoria (
    id BIGSERIAL PRIMARY KEY,
    rol_id BIGINT NOT NULL REFERENCES rol(id),
    actor_uuid UUID,
    actor_username VARCHAR(150),
    accion VARCHAR(50) NOT NULL,
    descripcion TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS empresa_auditoria (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    actor_uuid UUID,
    actor_username VARCHAR(150),
    accion VARCHAR(50) NOT NULL,
    descripcion TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documento_fecha ON documento_electronico(fecha_emision);
CREATE INDEX IF NOT EXISTS idx_documento_estado ON documento_electronico(estado_actual);
CREATE INDEX IF NOT EXISTS idx_documento_tipo ON documento_electronico(tipo_documento);
CREATE INDEX IF NOT EXISTS idx_documento_identificacion ON documento_electronico(identificacion_receptor);
CREATE INDEX IF NOT EXISTS idx_documento_clave ON documento_electronico(clave_acceso);
CREATE INDEX IF NOT EXISTS idx_documento_numero ON documento_electronico(numero_documento);
CREATE INDEX IF NOT EXISTS idx_documento_recepcion ON documento_electronico(fecha_recepcion);
CREATE INDEX IF NOT EXISTS idx_documento_empresa_estado ON documento_electronico(empresa_id, estado_actual);
CREATE INDEX IF NOT EXISTS idx_documento_empresa_fecha ON documento_electronico(empresa_id, fecha_emision);
CREATE INDEX IF NOT EXISTS idx_historial_documento ON documento_estado_historial(documento_id, created_at);
CREATE INDEX IF NOT EXISTS idx_error_documento ON documento_error(documento_id);
CREATE INDEX IF NOT EXISTS idx_usuario_sistema_activo ON usuario_sistema(activo);
CREATE INDEX IF NOT EXISTS idx_usuario_auditoria_usuario ON usuario_auditoria(usuario_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_empresa_auditoria_empresa_fecha ON empresa_auditoria(empresa_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_documento_clave_acceso
    ON documento_electronico(clave_acceso)
    WHERE clave_acceso IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_documento_procesamiento
    ON documento_electronico(estado_actual, fecha_recepcion)
    WHERE estado_actual IN ('RECIBIDO', 'PENDIENTE_AUTORIZACION', 'CORREO_PENDIENTE');

-- =========================================================
-- SEGURIDAD: DATOS INICIALES
-- =========================================================

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
    SELECT 1 FROM usuario_sistema WHERE username = 'admin'
);

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
      ('EMPRESA_AUDITORIA_VER', 'Ver auditoria de empresas', 'Permite consultar la auditoria administrativa de empresas y configuraciones sensibles.', 'Administracion'),
      ('USUARIO_VER', 'Ver usuarios', 'Permite consultar el listado de usuarios.', 'Seguridad'),
      ('USUARIO_CREAR', 'Crear usuarios', 'Permite registrar nuevos usuarios.', 'Seguridad'),
      ('USUARIO_EDITAR', 'Editar usuarios', 'Permite actualizar estado y credenciales de usuarios.', 'Seguridad'),
      ('USUARIO_AUDITORIA_VER', 'Ver auditoria de usuarios', 'Permite consultar la auditoria administrativa de usuarios.', 'Seguridad'),
      ('ROL_VER', 'Ver roles', 'Permite consultar la matriz de roles y permisos.', 'Seguridad'),
      ('ROL_ADMINISTRAR', 'Administrar roles', 'Permite definir la asignacion de permisos por rol.', 'Seguridad'),
      ('ROL_AUDITORIA_VER', 'Ver auditoria de roles', 'Permite consultar la auditoria administrativa de roles.', 'Seguridad')
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

-- =========================================================
-- FACTURACION ELECTRONICA LEGACY
-- Estas tablas siguen siendo usadas por el flujo /api/singsend/*
-- =========================================================

CREATE TABLE IF NOT EXISTS definir (
    iddefinir BIGSERIAL PRIMARY KEY,
    razonsocial VARCHAR(300),
    nombrecomercial VARCHAR(300),
    ruc VARCHAR(13),
    direccion VARCHAR(500),
    tipoambiente SMALLINT,
    iva REAL,
    empresa VARCHAR(150),
    ubirepo VARCHAR(500),
    posiacti VARCHAR(100),
    longacti VARCHAR(100),
    naturaleza VARCHAR(100),
    fechap DATE,
    nombre VARCHAR(300),
    ubicomprobantes VARCHAR(500),
    asunto VARCHAR(300),
    textomail TEXT,
    dirmatriz VARCHAR(500),
    fechacierre DATE,
    f_i VARCHAR(255),
    f_g VARCHAR(255),
    porciva NUMERIC(10, 4),
    ciudad VARCHAR(120),
    idtabla17 BIGINT,
    ubidigi VARCHAR(500),
    ubimagenes VARCHAR(500),
    swpreingsin VARCHAR(20),
    firma BYTEA,
    clave_firma VARCHAR(500),
    email VARCHAR(320),
    clave_email VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS tabla15 (
    idtabla15 BIGSERIAL PRIMARY KEY,
    codtabla15 VARCHAR(20),
    nomtabla15 VARCHAR(200),
    usucrea INTEGER,
    feccrea DATE,
    usumodi INTEGER,
    fecmodi DATE
);

CREATE TABLE IF NOT EXISTS facturas (
    idfactura BIGSERIAL PRIMARY KEY,
    idmodulo BIGINT,
    idcliente BIGINT,
    nrofactura VARCHAR(50),
    porcexoneracion BIGINT,
    razonexonera VARCHAR(255),
    totaltarifa NUMERIC(18, 6),
    pagado INTEGER,
    usuariocobro BIGINT,
    fechacobro DATE,
    estado BIGINT,
    usuarioanulacion BIGINT,
    fechaanulacion DATE,
    razonanulacion VARCHAR(500),
    usuarioeliminacion BIGINT,
    fechaeliminacion DATE,
    razoneliminacion VARCHAR(500),
    conveniopago BIGINT,
    fechaconvenio DATE,
    estadoconvenio BIGINT,
    formapago BIGINT,
    refeformapago VARCHAR(255),
    horacobro TIME,
    usuariotransferencia BIGINT,
    fechatransferencia DATE,
    usucrea BIGINT,
    feccrea DATE,
    usumodi BIGINT,
    fecmodi DATE,
    valorbase NUMERIC(18, 6),
    idabonado BIGINT,
    interescobrado NUMERIC(18, 6),
    swiva NUMERIC(18, 6),
    swcondonar BOOLEAN,
    valornotacredito NUMERIC(18, 6),
    secuencialfacilito VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS fec_factura (
    idfactura BIGSERIAL PRIMARY KEY,
    claveacceso VARCHAR(49),
    secuencial VARCHAR(20),
    xmlautorizado TEXT,
    errores TEXT,
    estado VARCHAR(10),
    establecimiento VARCHAR(3),
    puntoemision VARCHAR(3),
    direccionestablecimiento VARCHAR(500),
    fechaemision TIMESTAMP,
    tipoidentificacioncomprador VARCHAR(10),
    identificacioncomprador VARCHAR(20),
    guiaremision VARCHAR(50),
    razonsocialcomprador VARCHAR(300),
    telefonocomprador VARCHAR(50),
    emailcomprador VARCHAR(320),
    concepto VARCHAR(500),
    recaudador VARCHAR(200),
    referencia VARCHAR(100),
    direccioncomprador VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS fec_factura_detalles (
    idfacturadetalle BIGSERIAL PRIMARY KEY,
    idfactura BIGINT NOT NULL REFERENCES fec_factura(idfactura) ON DELETE CASCADE,
    codigoprincipal VARCHAR(50),
    descripcion VARCHAR(500),
    cantidad NUMERIC(18, 6),
    preciounitario NUMERIC(18, 6),
    descuento NUMERIC(18, 6)
);

CREATE TABLE IF NOT EXISTS fec_factura_detalles_impuestos (
    idfacturadetalleimpuestos BIGSERIAL PRIMARY KEY,
    idfacturadetalle BIGINT NOT NULL REFERENCES fec_factura_detalles(idfacturadetalle) ON DELETE CASCADE,
    codigoimpuesto VARCHAR(10),
    codigoporcentaje VARCHAR(10),
    baseimponible NUMERIC(18, 6)
);

CREATE TABLE IF NOT EXISTS fec_factura_pagos (
    idfacturapagos BIGSERIAL PRIMARY KEY,
    idfactura BIGINT NOT NULL REFERENCES fec_factura(idfactura) ON DELETE CASCADE,
    formapago VARCHAR(10),
    total NUMERIC(18, 6),
    plazo INTEGER,
    unidadtiempo VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_fec_factura_estado ON fec_factura(estado);
CREATE INDEX IF NOT EXISTS idx_fec_factura_fechaemision ON fec_factura(fechaemision);
CREATE INDEX IF NOT EXISTS idx_fec_factura_referencia ON fec_factura(referencia);
CREATE INDEX IF NOT EXISTS idx_fec_factura_identificacion ON fec_factura(identificacioncomprador);
CREATE INDEX IF NOT EXISTS idx_fec_factura_detalles_factura ON fec_factura_detalles(idfactura);
CREATE INDEX IF NOT EXISTS idx_fec_factura_det_impuestos_detalle ON fec_factura_detalles_impuestos(idfacturadetalle);
CREATE INDEX IF NOT EXISTS idx_fec_factura_pagos_factura ON fec_factura_pagos(idfactura);
CREATE INDEX IF NOT EXISTS idx_facturas_pagado ON facturas(pagado);
CREATE INDEX IF NOT EXISTS idx_facturas_idabonado ON facturas(idabonado);

-- Catalogo minimo de formas de pago para PDF/XML legacy
INSERT INTO tabla15 (codtabla15, nomtabla15, usucrea, feccrea, usumodi, fecmodi)
SELECT seed.codtabla15, seed.nomtabla15, seed.usucrea, seed.feccrea, seed.usumodi, seed.fecmodi
FROM (
    VALUES
      ('01', 'Sin utilizacion del sistema financiero', 1, CURRENT_DATE, NULL, NULL),
      ('15', 'Compensacion de deudas', 1, CURRENT_DATE, NULL, NULL),
      ('16', 'Tarjeta de debito', 1, CURRENT_DATE, NULL, NULL),
      ('17', 'Dinero electronico', 1, CURRENT_DATE, NULL, NULL),
      ('18', 'Tarjeta prepago', 1, CURRENT_DATE, NULL, NULL),
      ('19', 'Tarjeta de credito', 1, CURRENT_DATE, NULL, NULL),
      ('20', 'Otros con utilizacion del sistema financiero', 1, CURRENT_DATE, NULL, NULL),
      ('21', 'Endoso de titulos', 1, CURRENT_DATE, NULL, NULL)
) AS seed(codtabla15, nomtabla15, usucrea, feccrea, usumodi, fecmodi)
WHERE NOT EXISTS (
    SELECT 1 FROM tabla15 t WHERE t.codtabla15 = seed.codtabla15
);

-- Registro base de configuracion del emisor.
-- Ajustar manualmente valores reales de RUC, correo, certificado y rutas.
INSERT INTO definir (
    iddefinir,
    razonsocial,
    nombrecomercial,
    ruc,
    direccion,
    tipoambiente,
    iva,
    empresa,
    ubicomprobantes,
    asunto,
    textomail,
    dirmatriz,
    porciva,
    ciudad,
    email
)
SELECT
    1,
    'EMISOR SRI FILES',
    'SRI FILES',
    '9999999999999',
    'DIRECCION PENDIENTE',
    1,
    15,
    'SRI-FILES',
    './xmlFiles',
    'Comprobante electronico',
    'Adjuntamos su comprobante electronico.',
    'DIRECCION MATRIZ PENDIENTE',
    15,
    'CIUDAD',
    'noreply@sri-files.local'
WHERE NOT EXISTS (
    SELECT 1 FROM definir WHERE iddefinir = 1
);

COMMIT;
