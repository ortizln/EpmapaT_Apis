export interface UsuarioAutenticado {
  id: string;
  nombre: string;
  correo: string;
  roles: string[];
  permisos?: string[];
}

export interface PermisoSistema {
  codigo: string;
  nombre: string;
  descripcion: string;
  categoria: string;
}

export interface RolSistema {
  codigo: string;
  nombre: string;
  descripcion: string;
  permisos: string[];
}

export interface RolAuditoria {
  accion: string;
  descripcion: string;
  actorUsername: string;
  fecha: string;
}

export interface RolAuditoriaListadoItem {
  id: number;
  rolCodigo: string | null;
  rolNombre: string | null;
  accion: string;
  descripcion: string;
  actorUsername: string | null;
  fecha: string;
}

export interface RolAuditoriaListadoResponse {
  items: RolAuditoriaListadoItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
  usuario: UsuarioAutenticado;
}

export interface UsuarioSistema {
  id: string;
  username: string;
  nombre: string;
  correo: string;
  rol: string;
  activo: boolean;
}

export interface UsuarioSistemaListadoResponse {
  items: UsuarioSistema[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface UsuarioCrearRequest {
  username: string;
  nombre: string;
  correo: string;
  password: string;
  rol: string;
}

export interface UsuarioAuditoria {
  accion: string;
  descripcion: string;
  actorUsername: string;
  fecha: string;
}

export interface UsuarioAuditoriaListadoItem {
  id: number;
  usuarioId: string | null;
  username: string | null;
  nombre: string | null;
  correo: string | null;
  rol: string | null;
  accion: string;
  descripcion: string;
  actorUsername: string | null;
  fecha: string;
}

export interface UsuarioAuditoriaListadoResponse {
  items: UsuarioAuditoriaListadoItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}
