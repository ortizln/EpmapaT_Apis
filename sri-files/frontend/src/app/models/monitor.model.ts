export interface MonitorResumen {
  totalDocumentos: number;
  pendientesProcesamiento: number;
  pendientesAutorizacion: number;
  pendientesCorreo: number;
  conError: number;
  finalizados: number;
}

export interface MonitorComponenteEstado {
  nombre: string;
  estado: string;
  detalle: string;
}

export interface MonitorHealthResponse {
  estado: string;
  timestamp: string;
  resumen: MonitorResumen;
  componentes: MonitorComponenteEstado[];
}
