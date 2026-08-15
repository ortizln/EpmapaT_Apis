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

export interface EmpresaConfiguracionRequest {
  ambienteSri: number;
  correoNotificaciones: string;
  correoRespuesta: string;
  certificadoNombre: string;
  certificadoBase64: string | null;
  certificadoClave: string | null;
  limpiarCertificado: boolean;
}
