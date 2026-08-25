CREATE TABLE recurso_empresa (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    tipo VARCHAR(30) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    ruta TEXT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recurso_empresa_empresa ON recurso_empresa(empresa_id, created_at DESC);

CREATE TABLE plantilla_ride (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    empresa_id BIGINT NOT NULL REFERENCES empresa(id),
    tipo_documento VARCHAR(30) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    predeterminada BOOLEAN NOT NULL DEFAULT FALSE,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    ruta_archivo VARCHAR(500),
    nombre_archivo VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_plantilla_ride_empresa_tipo_version UNIQUE (empresa_id, tipo_documento, version)
);

CREATE INDEX idx_plantilla_ride_empresa ON plantilla_ride(empresa_id, tipo_documento, created_at DESC);
