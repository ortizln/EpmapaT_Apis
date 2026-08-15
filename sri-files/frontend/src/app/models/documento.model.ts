export type TipoDocumento =
  | 'FACTURA'
  | 'RETENCION'
  | 'GUIA_REMISION'
  | 'NOTA_CREDITO'
  | 'NOTA_DEBITO'
  | 'LIQUIDACION_COMPRA';

export interface DocumentoResumen {
  id: string;
  tipoDocumento: TipoDocumento;
  numeroComprobante: string | null;
  razonSocial: string;
  fechaEmision: string;
  estado: string;
}

export interface DocumentoListadoResponse {
  items: DocumentoResumen[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface DocumentoConteo {
  clave: string;
  total: number;
}

export interface DocumentoResumenOperativo {
  totalDocumentos: number;
  recibidosHoy: number;
  autorizados: number;
  pendientes: number;
  conErrores: number;
  requiereIntervencion: number;
  porTipo: DocumentoConteo[];
  porEstado: DocumentoConteo[];
}

export interface DocumentoEstadoResponse {
  id: string;
  estado: string;
  requiereIntervencion: boolean;
}

export interface DocumentoCampoContrato {
  nombre: string;
  tipo: string;
  requerido: boolean;
  descripcion: string;
  ejemplo: string;
}

export interface DocumentoSeccionContrato {
  nombre: string;
  multiple: boolean;
  campos: DocumentoCampoContrato[];
}

export interface DocumentoContrato {
  tipoDocumento: TipoDocumento;
  endpoint: string;
  metodo: string;
  secciones: DocumentoSeccionContrato[];
}

export interface CorreoRequest {
  enviar: boolean;
  destinatarios: string[];
}

export interface DocumentoRecepcionRequest {
  tipoDocumento: TipoDocumento;
  externalId: string | null;
  emisor: Record<string, unknown>;
  receptor: Record<string, unknown>;
  documento: Record<string, unknown>;
  detalles: Record<string, unknown>[];
  destinatarios: Record<string, unknown>[];
  motivos: Record<string, unknown>[];
  impuestos: Record<string, unknown>[];
  informacionAdicional: Record<string, unknown>;
  correo: CorreoRequest;
}

export interface DocumentoRecepcionResponse {
  id: string;
  tipoDocumento: TipoDocumento;
  estado: string;
  mensaje: string;
  duplicado: boolean;
}

export interface DocumentoDetalleResponse {
  id: string;
  tipoDocumento: TipoDocumento;
  estado: string;
  externalId: string | null;
  numeroDocumento: string | null;
  claveAcceso: string | null;
  identificacionReceptor: string | null;
  razonSocialReceptor: string | null;
  emailReceptor: string | null;
  fechaEmision: string | null;
  fechaRecepcion: string | null;
}

export interface DocumentoEstadoTimelineItem {
  titulo: string;
  descripcion: string;
  valor: string;
  resaltado?: boolean;
}

export interface DocumentoAutorizacionManualResponse {
  id: string;
  claveAcceso: string | null;
  estado: string;
  autorizado: boolean;
  numeroAutorizacion: string | null;
  fechaAutorizacion: string | null;
  mensaje: string | null;
  actualizado: boolean;
}

export interface DocumentoAutorizacionConsultaResponse {
  claveAcceso: string;
  estado: string;
  autorizado: boolean;
  numeroAutorizacion: string | null;
  fechaAutorizacion: string | null;
  mensaje: string | null;
  encontrada: boolean;
  xmlAutorizado: string | null;
}

export interface DocumentoHistorialItemResponse {
  id: number;
  estadoAnterior: string | null;
  estadoNuevo: string | null;
  descripcion: string | null;
  origen: string | null;
  usuarioId: number | null;
  metadata: string | null;
  createdAt: string | null;
}

export interface DocumentoErrorItemResponse {
  id: number;
  etapa: string | null;
  codigo: string | null;
  mensaje: string;
  detalle: string | null;
  recuperable: boolean;
  resuelto: boolean;
  fechaResolucion: string | null;
  createdAt: string | null;
}

export interface DocumentoIntentoSriItemResponse {
  tipo: string;
  etapa: string | null;
  estado: string | null;
  resultado: string | null;
  descripcion: string | null;
  codigo: string | null;
  mensaje: string | null;
  recuperable: boolean;
  createdAt: string | null;
}

export interface DocumentoIntentoSriResponse {
  id: string;
  totalIntentos: number;
  estadoActual: string | null;
  requiereIntervencion: boolean;
  fechaInicioProcesamiento: string | null;
  fechaFinalizacion: string | null;
  eventos: DocumentoIntentoSriItemResponse[];
}

export interface DocumentoCorreoEventoResponse {
  tipo: string;
  estado: string | null;
  resultado: string | null;
  descripcion: string | null;
  codigo: string | null;
  mensaje: string | null;
  recuperable: boolean;
  createdAt: string | null;
}

export interface DocumentoCorreoSeguimientoResponse {
  id: string;
  destinatario: string | null;
  remitente: string | null;
  estadoActual: string | null;
  requiereIntervencion: boolean;
  correoConfigurado: boolean;
  eventos: DocumentoCorreoEventoResponse[];
}

export interface DocumentoCorreoReenvioResponse {
  id: string;
  estado: string;
  destinatario: string;
  mensaje: string;
}
