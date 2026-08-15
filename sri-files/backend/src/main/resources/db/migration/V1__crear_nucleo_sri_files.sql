CREATE TABLE empresa (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    ruc VARCHAR(13) NOT NULL UNIQUE,
    razon_social VARCHAR(300) NOT NULL,
    nombre_comercial VARCHAR(300),
    direccion_matriz VARCHAR(500),
    obligado_contabilidad BOOLEAN NOT NULL DEFAULT FALSE,
    contribuyente_especial VARCHAR(50),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE establecimiento (
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

CREATE TABLE punto_emision (
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

CREATE TABLE secuencial (
    id BIGSERIAL PRIMARY KEY,
    punto_emision_id BIGINT NOT NULL REFERENCES punto_emision(id),
    tipo_documento VARCHAR(30) NOT NULL,
    valor_actual BIGINT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_secuencial UNIQUE (punto_emision_id, tipo_documento)
);

CREATE TABLE documento_electronico (
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

CREATE TABLE documento_estado_historial (
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

CREATE TABLE documento_archivo (
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

CREATE INDEX idx_documento_fecha ON documento_electronico(fecha_emision);
CREATE INDEX idx_documento_estado ON documento_electronico(estado_actual);
CREATE INDEX idx_documento_tipo ON documento_electronico(tipo_documento);
CREATE INDEX idx_documento_identificacion ON documento_electronico(identificacion_receptor);
CREATE INDEX idx_documento_clave ON documento_electronico(clave_acceso);
CREATE INDEX idx_documento_numero ON documento_electronico(numero_documento);
CREATE INDEX idx_documento_recepcion ON documento_electronico(fecha_recepcion);
CREATE INDEX idx_documento_empresa_estado ON documento_electronico(empresa_id, estado_actual);
CREATE INDEX idx_documento_empresa_fecha ON documento_electronico(empresa_id, fecha_emision);
CREATE INDEX idx_historial_documento ON documento_estado_historial(documento_id, created_at);
CREATE INDEX idx_documento_procesamiento
    ON documento_electronico(estado_actual, fecha_recepcion)
    WHERE estado_actual IN ('RECIBIDO', 'PENDIENTE_AUTORIZACION', 'CORREO_PENDIENTE');
CREATE UNIQUE INDEX uk_documento_clave_acceso
    ON documento_electronico(clave_acceso)
    WHERE clave_acceso IS NOT NULL;
