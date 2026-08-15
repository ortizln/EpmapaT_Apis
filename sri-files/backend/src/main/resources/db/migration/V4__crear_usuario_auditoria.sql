CREATE TABLE IF NOT EXISTS usuario_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario_sistema(id),
    actor_uuid UUID,
    actor_username VARCHAR(80),
    accion VARCHAR(40) NOT NULL,
    descripcion TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usuario_auditoria_usuario ON usuario_auditoria(usuario_id, created_at DESC);
