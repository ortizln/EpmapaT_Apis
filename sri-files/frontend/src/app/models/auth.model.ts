export interface UsuarioAutenticado {
  id: string;
  nombre: string;
  correo: string;
  roles: string[];
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
