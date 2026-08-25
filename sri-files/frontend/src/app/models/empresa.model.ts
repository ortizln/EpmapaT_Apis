export interface Empresa {
  id: string;
  ruc: string;
  razonSocial: string;
  nombreComercial: string | null;
  direccionMatriz: string | null;
  obligadoContabilidad: boolean;
  contribuyenteEspecial: string | null;
  ambienteSri: number;
  correoNotificaciones: string | null;
  certificadoConfigurado: boolean;
  activo: boolean;
}

export interface EmpresaListadoResponse {
  items: Empresa[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface EmpresaRequest {
  ruc: string;
  razonSocial: string;
  nombreComercial: string;
  direccionMatriz: string;
  obligadoContabilidad: boolean;
  contribuyenteEspecial: string;
}

export interface EmpresaConfiguracion {
  empresaId: string;
  ambienteSri: number;
  correoNotificaciones: string | null;
  correoRespuesta: string | null;
  certificadoConfigurado: boolean;
  certificadoNombre: string | null;
  certificadoAlias: string | null;
  certificadoTitular: string | null;
  certificadoVigenciaDesde: string | null;
  certificadoVigenciaHasta: string | null;
}

export interface SriConfiguracion {
  empresaId: string;
  ambiente: number;
  timeoutConexionMs: number;
  timeoutRespuestaMs: number;
  maxReintentos: number;
  activo: boolean;
}

export interface SriConfiguracionRequest {
  ambiente: number;
  timeoutConexionMs: number;
  timeoutRespuestaMs: number;
  maxReintentos: number;
  activo: boolean;
}

export interface CorreoConfiguracion {
  empresaId: string;
  remitente: string | null;
  nombreRemitente: string | null;
  enviarXml: boolean;
  enviarRide: boolean;
  plantillaAsunto: string | null;
}

export interface CorreoConfiguracionRequest {
  remitente: string;
  nombreRemitente: string;
  enviarXml: boolean;
  enviarRide: boolean;
  plantillaAsunto: string;
}

export interface EmpresaConfiguracionRequest {
  ambienteSri: number;
  correoNotificaciones: string;
  correoRespuesta: string;
  certificadoNombre: string;
  certificadoBase64: string | null;
  certificadoClave: string | null;
  limpiarCertificado: boolean;
}

export interface EmpresaAuditoria {
  accion: string;
  descripcion: string;
  actorUsername: string | null;
  fecha: string;
}

export interface EmpresaAuditoriaListadoItem {
  id: number;
  empresaId: string | null;
  ruc: string | null;
  razonSocial: string | null;
  accion: string;
  descripcion: string;
  actorUsername: string | null;
  fecha: string;
}

export interface EmpresaAuditoriaListadoResponse {
  items: EmpresaAuditoriaListadoItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}
