-- Gestion de cuentas y blacklist para msvc-emails

CREATE TABLE IF NOT EXISTS public.email_account (
    id bigserial NOT NULL,
    code varchar(50) NOT NULL,
    name varchar(150) NOT NULL,
    provider varchar(100) NULL,
    from_address varchar(320) NOT NULL,
    from_name varchar(150) NULL,
    reply_to varchar(320) NULL,
    transport_type varchar(20) NOT NULL DEFAULT 'SMTP',
    host varchar(150) NULL,
    port integer NULL,
    protocol varchar(20) NULL DEFAULT 'smtp',
    security_type varchar(20) NULL DEFAULT 'STARTTLS',
    auth_required boolean NOT NULL DEFAULT true,
    username varchar(320) NULL,
    password varchar(500) NULL,
    api_url varchar(500) NULL,
    api_auth_header varchar(100) NULL,
    api_auth_scheme varchar(50) NULL,
    api_key varchar(500) NULL,
    active boolean NOT NULL DEFAULT true,
    is_default boolean NOT NULL DEFAULT false,
    default_for_type varchar(30) NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT email_account_pk PRIMARY KEY (id),
    CONSTRAINT email_account_code_uk UNIQUE (code),
    CONSTRAINT email_account_transport_chk CHECK (transport_type IN ('SMTP', 'API_HTTP')),
    CONSTRAINT email_account_security_chk CHECK (security_type IN ('NONE', 'STARTTLS', 'SSL_TLS')),
    CONSTRAINT email_account_default_for_type_chk CHECK (default_for_type IN ('DOC_ELECTRONICO', 'NOTIFICACION', 'CUSTOM') OR default_for_type IS NULL)
);

CREATE TABLE IF NOT EXISTS public.email_blacklist (
    id bigserial NOT NULL,
    type varchar(20) NOT NULL,
    value varchar(320) NOT NULL,
    reason varchar(500) NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT email_blacklist_pk PRIMARY KEY (id),
    CONSTRAINT email_blacklist_type_value_uk UNIQUE (type, value),
    CONSTRAINT email_blacklist_type_chk CHECK (type IN ('DOMAIN', 'HOST', 'EMAIL'))
);

ALTER TABLE public.email_message
    ADD COLUMN IF NOT EXISTS email_account_id bigint NULL;

ALTER TABLE public.email_message
    ADD COLUMN IF NOT EXISTS from_address varchar(320) NULL;

ALTER TABLE public.email_attachment
    ADD COLUMN IF NOT EXISTS content bytea NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'email_message_email_account_fk'
    ) THEN
        ALTER TABLE public.email_message
            ADD CONSTRAINT email_message_email_account_fk
            FOREIGN KEY (email_account_id)
            REFERENCES public.email_account (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS email_message_account_id_idx
    ON public.email_message (email_account_id);

CREATE INDEX IF NOT EXISTS email_blacklist_active_idx
    ON public.email_blacklist (active);

CREATE UNIQUE INDEX IF NOT EXISTS email_account_default_for_type_uidx
    ON public.email_account (default_for_type)
    WHERE default_for_type IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS email_account_general_default_uidx
    ON public.email_account ((1))
    WHERE is_default = true;

-- Ejemplos para tus cuentas reales:
-- INSERT INTO public.email_account
-- (code, name, provider, from_address, from_name, reply_to, transport_type, host, port, protocol, security_type,
--  auth_required, username, password, active, is_default, default_for_type, created_at, updated_at)
-- VALUES
-- ('FACTURACION', 'Cuenta Facturacion EPMAPA-T', 'SMTP', 'facturacion@empapatulcan.gob.ec', 'EPMAPA-T Facturacion',
--  'facturacion@empapatulcan.gob.ec', 'SMTP', 'smtp.tu-servidor.com', 465, 'smtp', 'SSL_TLS',
--  true, 'facturacion@empapatulcan.gob.ec', 'CAMBIAR_PASSWORD', true, true, 'DOC_ELECTRONICO', now(), now()),
-- ('NOTIFICACIONES', 'Cuenta Notificaciones EPMAPA-T', 'SMTP', 'notificaciones@empapatulcan.com', 'EPMAPA-T Notificaciones',
--  'notificaciones@empapatulcan.com', 'SMTP', 'smtp.tu-servidor.com', 465, 'smtp', 'SSL_TLS',
--  true, 'notificaciones@empapatulcan.com', 'CAMBIAR_PASSWORD', true, false, 'NOTIFICACION', now(), now());

-- Si una cuenta se enviara por API_HTTP en vez de SMTP, usa transport_type='API_HTTP'
-- y completa api_url, api_auth_header, api_auth_scheme y api_key.
