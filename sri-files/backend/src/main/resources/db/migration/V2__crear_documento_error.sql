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

CREATE INDEX IF NOT EXISTS idx_error_documento ON documento_error(documento_id);
