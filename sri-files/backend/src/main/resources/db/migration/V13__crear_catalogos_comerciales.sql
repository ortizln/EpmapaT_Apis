create table if not exists cliente_catalogo (
    id bigserial primary key,
    uuid uuid not null unique,
    empresa_id bigint not null references empresa(id),
    tipo_identificacion varchar(2) not null,
    identificacion varchar(20) not null,
    razon_social varchar(300) not null,
    nombre_comercial varchar(300),
    email varchar(320),
    telefono varchar(30),
    direccion varchar(500),
    observacion varchar(500),
    activo boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_cliente_catalogo_empresa_identificacion unique (empresa_id, identificacion)
);

create table if not exists producto_catalogo (
    id bigserial primary key,
    uuid uuid not null unique,
    empresa_id bigint not null references empresa(id),
    codigo varchar(60) not null,
    nombre varchar(300) not null,
    descripcion varchar(500),
    unidad_medida varchar(20),
    precio_base numeric(14, 6) not null default 0,
    porcentaje_iva numeric(5, 2) not null default 0,
    activo boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_producto_catalogo_empresa_codigo unique (empresa_id, codigo)
);

create table if not exists forma_pago_catalogo (
    id bigserial primary key,
    uuid uuid not null unique,
    empresa_id bigint not null references empresa(id),
    codigo varchar(20) not null,
    nombre varchar(150) not null,
    descripcion varchar(300),
    dias_plazo integer not null default 0,
    activo boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_forma_pago_catalogo_empresa_codigo unique (empresa_id, codigo)
);

create table if not exists iva_tarifa_catalogo (
    id bigserial primary key,
    uuid uuid not null unique,
    empresa_id bigint not null references empresa(id),
    codigo varchar(20) not null,
    nombre varchar(150) not null,
    porcentaje numeric(5, 2) not null default 0,
    codigo_sri varchar(10),
    descripcion varchar(300),
    activo boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_iva_tarifa_catalogo_empresa_codigo unique (empresa_id, codigo)
);
