export interface DashboardResumen {
  total: number;
  recibidos: number;
  procesando: number;
  autorizados: number;
  noAutorizados: number;
  errores: number;
  correosPendientes: number;
}

export interface DashboardDocumentoTipo {
  tipo: string;
  cantidad: number;
}

export interface DashboardDocumentoEstado {
  estado: string;
  cantidad: number;
}

export interface DashboardDocumentoDia {
  fecha: string;
  cantidad: number;
}

export interface DashboardErrorEtapa {
  etapa: string;
  cantidad: number;
}

export interface DashboardTiempos {
  promedioProcesamientoMs: number;
  promedioAutorizacionMs: number;
}

export interface DashboardSnapshot {
  resumen: DashboardResumen;
  porTipo: DashboardDocumentoTipo[];
  porEstado: DashboardDocumentoEstado[];
  porDia: DashboardDocumentoDia[];
  erroresPorEtapa: DashboardErrorEtapa[];
  tiempos: DashboardTiempos;
}
