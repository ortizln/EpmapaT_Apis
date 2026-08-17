export interface MonitorPendienteItem {
  uuid: string;
  tipoDocumento: string;
  numeroDocumento: string | null;
  razonSocial: string | null;
  estado: string;
  fechaRecepcion: string | null;
  intentos: number;
  requiereIntervencion: boolean;
}

export interface MonitorPendientesResponse {
  total: number;
  items: MonitorPendienteItem[];
}

export interface CorreoPendienteItem {
  uuid: string;
  tipoDocumento: string;
  numeroDocumento: string | null;
  razonSocial: string | null;
  destinatario: string | null;
  estado: string;
  fechaRecepcion: string | null;
  fechaAutorizacion: string | null;
  requiereIntervencion: boolean;
}

export interface CorreoPendienteResponse {
  total: number;
  items: CorreoPendienteItem[];
}
