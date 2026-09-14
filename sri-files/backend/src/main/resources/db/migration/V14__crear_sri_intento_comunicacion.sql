CREATE TABLE IF NOT EXISTS sri_intento_comunicacion (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    tipo_documento VARCHAR(30) NOT NULL,
    clave_acceso VARCHAR(49),
    ambiente VARCHAR(20),
    servicio VARCHAR(30) NOT NULL,
    endpoint TEXT,
    numero_intento INTEGER NOT NULL,
    fecha_inicio TIMESTAMP,
    fecha_fin TIMESTAMP,
    resultado VARCHAR(40),
    codigo_sri VARCHAR(20),
    mensaje_sri TEXT,
    informacion_adicional TEXT,
    tipo_error VARCHAR(50),
    excepcion TEXT,
    http_status INTEGER,
    duracion_ms BIGINT,
    reintentable BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_consulta BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_proximo_intento TIMESTAMP,
    worker_id VARCHAR(100),
    correlation_id VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sri_intento_documento ON sri_intento_comunicacion(documento_id);
CREATE INDEX IF NOT EXISTS idx_sri_intento_clave ON sri_intento_comunicacion(clave_acceso);
CREATE INDEX IF NOT EXISTS idx_sri_intento_proximo ON sri_intento_comunicacion(fecha_proximo_intento);
CREATE INDEX IF NOT EXISTS idx_sri_intento_worker ON sri_intento_comunicacion(worker_id);